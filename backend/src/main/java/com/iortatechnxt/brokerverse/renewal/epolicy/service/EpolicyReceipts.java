package com.iortatechnxt.brokerverse.renewal.epolicy.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.CandidatePlacement;
import com.iortatechnxt.brokerverse.renewal.domain.EpolicyReceipt;
import com.iortatechnxt.brokerverse.renewal.domain.EpolicyReceiptRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receipt of the e-policy files of an insurer (FRRN.033.01 to FRRN.033.03), uploaded or taken from
 * its MFT location: the summary file and the ZIP file are both required; every document of the ZIP
 * must be in the summary file; each record is matched to its renewal account on the reference
 * number, the policy number of the account is updated and the e-policy attached, and the account is
 * For E-Policy Sending. Each receipt keeps its file-level outcome and its record-level results.
 */
@Service
public class EpolicyReceipts {

  /** Document type of an e-policy. */
  public static final String DOC_TYPE = "EPOLICY";

  private static final String MATCHED = "MATCHED";
  private static final String UNMATCHED = "UNMATCHED";

  private final EpolicyReceiptRepository receipts;
  private final RenewalCandidateRepository candidates;
  private final EpolicyEligibility eligibility;
  private final DocumentService storage;
  private final DocumentNumberService numbers;
  private final NamedParameterJdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param receipts receipts
   * @param candidates renewals
   * @param eligibility eligibility and duplicate policy numbers
   * @param storage stored e-policies
   * @param numbers receipt numbers
   * @param jdbc record-level results
   * @param currentUser user
   * @param audit audit trail
   * @param clock clock
   */
  public EpolicyReceipts(
      EpolicyReceiptRepository receipts,
      RenewalCandidateRepository candidates,
      EpolicyEligibility eligibility,
      DocumentService storage,
      DocumentNumberService numbers,
      NamedParameterJdbcTemplate jdbc,
      CurrentUser currentUser,
      AuditTrailService audit,
      Clock clock) {
    this.receipts = receipts;
    this.candidates = candidates;
    this.eligibility = eligibility;
    this.storage = storage;
    this.numbers = numbers;
    this.jdbc = jdbc;
    this.currentUser = currentUser;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Receives the e-policy files of an insurer.
   *
   * @param companyId company
   * @param type UPLOAD or MFT
   * @param summary summary file, may be null
   * @param zip ZIP file, may be null
   * @return the receipt
   */
  @Transactional
  public EpolicyReceipt receive(Long companyId, String type, Incoming summary, Incoming zip) {
    EpolicyReceipt r =
        receipts.save(
            new EpolicyReceipt(
                companyId,
                numbers.next("EPR-" + BusinessClock.today(clock).getYear()),
                type,
                summary == null ? null : summary.name(),
                zip == null ? null : zip.name()));
    if (summary == null || zip == null) {
      r.failed("The E-Policy Summary File and the E-Policy ZIP File are both required");
      return r;
    }
    try {
      List<EpolicyFiles.Summary> records = EpolicyFiles.summary(summary.content());
      Map<String, byte[]> docs = EpolicyFiles.documents(zip.content());
      int matched = process(r, records, docs);
      r.processed(records.size(), matched);
    } catch (BusinessRuleException ex) {
      r.failed(ex.getMessage());
    }
    audit.record(
        "EpolicyReceipt",
        r.getReceiptNo(),
        AuditAction.CREATE,
        r.getStatus() + " " + Objects.toString(r.getRemarks(), ""));
    return r;
  }

  private int process(
      EpolicyReceipt r, List<EpolicyFiles.Summary> records, Map<String, byte[]> docs) {
    Set<String> listed = new HashSet<>();
    records.forEach(s -> listed.add(s.pdfFile()));
    List<String> strays = docs.keySet().stream().filter(n -> !listed.contains(n)).toList();
    if (!strays.isEmpty()) {
      throw new BusinessRuleException(
          "RNW_EPOLICY_STRAY", "Documents not in the summary file: " + String.join(", ", strays));
    }
    int matched = 0;
    for (EpolicyFiles.Summary s : records) {
      Optional<RenewalCandidate> c =
          candidates.findByCompanyIdAndRenewalRef(r.getCompanyId(), s.reference());
      String problem = problem(c, s, docs);
      Long attachment = null;
      if (problem == null) {
        attachment = attach(c.get(), s, docs.get(s.pdfFile()), r.getReceiptNo());
        matched++;
      }
      line(r, s, c.map(RenewalCandidate::getId).orElse(null), problem, attachment);
    }
    return matched;
  }

  private String problem(
      Optional<RenewalCandidate> c, EpolicyFiles.Summary s, Map<String, byte[]> docs) {
    if (c.isEmpty()) {
      return "Record not found";
    }
    if (!docs.containsKey(s.pdfFile())) {
      return "Document " + s.pdfFile() + " is missing from the ZIP file";
    }
    return eligibility.problem(c.get(), s.policyNo());
  }

  private Long attach(RenewalCandidate c, EpolicyFiles.Summary s, byte[] pdf, String receiptNo) {
    Long id =
        storage
            .upload(
                new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString()),
                List.of(new DocumentService.UploadedFile(s.pdfFile(), pdf)),
                new DocumentService.UploadOptions(
                    DOC_TYPE, false, c.getRenewalRef(), "E-policy " + s.tag(), null))
            .get(0)
            .getId();
    c.getPlacement().epolicy(s.policyNo(), receiptNo, currentUser.username(), clock.instant());
    c.getPlacement().status(CandidatePlacement.FOR_EPOLICY_SENDING);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "E-policy " + s.policyNo() + " received (" + receiptNo + ")");
    return id;
  }

  private void line(
      EpolicyReceipt r, EpolicyFiles.Summary s, Long candidateId, String problem, Long attachment) {
    jdbc.update(
        "insert into rnw_epolicy_line (receipt_id, seq_no, renewal_ref, policy_no, pdf_file,"
            + " document_tag, match_status, remarks, candidate_id, attachment_id) values"
            + " (:receipt, :seq, :ref, :policy, :pdf, :tag, :match, :remarks, :candidate,"
            + " :attachment)",
        new MapSqlParameterSource()
            .addValue("receipt", r.getId())
            .addValue("seq", s.seq())
            .addValue("ref", s.reference())
            .addValue("policy", s.policyNo())
            .addValue("pdf", s.pdfFile())
            .addValue("tag", s.tag())
            .addValue("match", problem == null ? MATCHED : UNMATCHED)
            .addValue("remarks", problem)
            .addValue("candidate", problem == null ? candidateId : null)
            .addValue("attachment", attachment));
  }

  /**
   * A file received.
   *
   * @param name file name
   * @param content content
   */
  public record Incoming(String name, byte[] content) {

    /** Defensive copy. */
    public Incoming {
      content = content == null ? new byte[0] : content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Incoming i && i.name.equals(name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return "Incoming[" + name + "]";
    }
  }
}
