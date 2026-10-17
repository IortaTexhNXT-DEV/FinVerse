package com.iortatechnxt.brokerverse.renewal.proposal.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.TsuRequest;
import com.iortatechnxt.brokerverse.renewal.domain.TsuRequestRepository;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RemarkService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quotation requests to TSU of the renewal accounts For Quotation (FRRN.018.01): one open request
 * per account, inheriting the account information; submitted for the approval of the Team Lead
 * (Pending Team Lead Approval), who approves, rejects or returns it for revision; once approved a
 * TSU Officer is assigned (For TSU Processing) and records the insurer quotations of the
 * comparative table; Marketing selects the insurers and TSU completes the request with the
 * proposal, which is then viewed, downloaded and sent like any proposal. Every change is notified.
 */
@Service
public class TsuRequests {

  private static final Map<String, String> DECISIONS =
      Map.of(
          TsuRequest.APPROVED, "approved by the Team Lead",
          TsuRequest.REJECTED, "rejected by the Team Lead",
          TsuRequest.RETURNED, "returned for revision");

  private final RenewalRecords records;
  private final TsuRequestRepository requests;
  private final RenewalProposals proposals;
  private final DocumentService storage;
  private final DocumentNumberService numbers;
  private final NamedParameterJdbcTemplate jdbc;
  private final RenewalNotices notices;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param requests TSU requests
   * @param proposals proposal of a completed request
   * @param storage stored proposal
   * @param numbers request numbers
   * @param jdbc comparative table
   * @param notices notifications
   * @param currentUser user
   * @param audit audit trail
   * @param clock clock
   */
  public TsuRequests(
      RenewalRecords records,
      TsuRequestRepository requests,
      RenewalProposals proposals,
      DocumentService storage,
      DocumentNumberService numbers,
      NamedParameterJdbcTemplate jdbc,
      RenewalNotices notices,
      CurrentUser currentUser,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.requests = requests;
    this.proposals = proposals;
    this.storage = storage;
    this.numbers = numbers;
    this.jdbc = jdbc;
    this.notices = notices;
    this.currentUser = currentUser;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Creates the TSU request of an account For Quotation.
   *
   * @param companyId company
   * @param ref renewal
   * @param remarks remarks
   * @return the request
   */
  @Transactional
  public TsuRequest create(Long companyId, String ref, String remarks) {
    RenewalCandidate c = records.get(companyId, ref);
    if (c.getDisposition() == null
        || c.getDisposition().code() != RenewalDisposition.FOR_QUOTATION) {
      throw new BusinessRuleException(
          "RNW_TSU_DISPOSITION", "A TSU request is created only for an account For Quotation");
    }
    if (requests.findByCandidateIdOrderByIdDesc(c.getId()).stream().anyMatch(TsuRequest::isOpen)) {
      throw new BusinessRuleException(
          "RNW_TSU_DUPLICATE", "A TSU request already exists for this renewal account");
    }
    TsuRequest r =
        requests.save(
            new TsuRequest(
                c,
                numbers.next("TSU-" + BusinessClock.today(clock).getYear()),
                remarks == null || remarks.isBlank() ? null : remarks.strip()));
    audit.record(RenewalCodes.ENTITY, ref, AuditAction.CREATE, "TSU request " + r.getRequestNo());
    return r;
  }

  /**
   * Submits a request for the Team Lead's approval.
   *
   * @param companyId company
   * @param requestNo request
   * @return the request
   */
  @Transactional
  public TsuRequest submit(Long companyId, String requestNo) {
    TsuRequest r = request(companyId, requestNo);
    require(r, TsuRequest.DRAFT, TsuRequest.RETURNED);
    r.status(TsuRequest.PENDING);
    RenewalCandidate c = records.byId(r.getCandidateId());
    if (c.getOwnerUnit() != null) {
      notices.teamLeaders(
          companyId,
          c.getOwnerUnit(),
          RenewalCodes.EVENT_ASSIGNED,
          c,
          new RenewalNotices.Text(
              requestNo + " pending your approval", c.getSnapshot().clientName()));
    }
    audit.record(
        RenewalCodes.ENTITY, c.getRenewalRef(), AuditAction.SUBMIT, requestNo + " submitted");
    return r;
  }

  /**
   * The Team Lead's decision: APPROVED, REJECTED or RETURNED_FOR_REVISION.
   *
   * @param companyId company
   * @param requestNo request
   * @param decision decision
   * @param remarks remarks, required for a rejection or a return
   * @return the request
   */
  @Transactional
  public TsuRequest decide(Long companyId, String requestNo, String decision, String remarks) {
    TsuRequest r = request(companyId, requestNo);
    require(r, TsuRequest.PENDING);
    if (!TsuRequest.APPROVED.equals(decision)
        && !TsuRequest.REJECTED.equals(decision)
        && !TsuRequest.RETURNED.equals(decision)) {
      throw new BusinessRuleException("RNW_TSU_DECISION", "Approve, reject or return the request");
    }
    String text =
        TsuRequest.APPROVED.equals(decision)
            ? remarks
            : RemarkService.requireText(remarks, "Enter the remarks");
    r.decide(decision, currentUser.username(), text);
    notifyOwner(r, requestNo + ": " + DECISIONS.get(decision));
    return r;
  }

  /**
   * Assigns the TSU Officer of an approved request.
   *
   * @param companyId company
   * @param requestNo request
   * @param officer TSU Officer
   * @return the request
   */
  @Transactional
  public TsuRequest assign(Long companyId, String requestNo, String officer) {
    TsuRequest r = request(companyId, requestNo);
    require(r, TsuRequest.APPROVED);
    if (officer == null || officer.isBlank()) {
      throw new BusinessRuleException("RNW_TSU_OFFICER", "Select the TSU Officer");
    }
    r.assign(officer.strip());
    RenewalCandidate c = records.byId(r.getCandidateId());
    notices.users(
        Collections.singletonList(officer.strip()),
        RenewalCodes.EVENT_ASSIGNED,
        c,
        new RenewalNotices.Text(requestNo + " assigned to you", c.getSnapshot().clientName()));
    notifyOwner(r, requestNo + ": For TSU Processing");
    return r;
  }

  /**
   * Records an insurer quotation of the comparative table.
   *
   * @param companyId company
   * @param requestNo request
   * @param quote insurer, premium and terms
   */
  @Transactional
  public void quote(Long companyId, String requestNo, Quote quote) {
    TsuRequest r = request(companyId, requestNo);
    require(r, TsuRequest.PROCESSING);
    if (quote.insurerCode() == null || quote.insurerCode().isBlank()) {
      throw new BusinessRuleException("RNW_TSU_QUOTE", "Select the insurer");
    }
    jdbc.update(
        "insert into rnw_tsu_quote (request_id, insurer_code, premium, terms, created_at, created_by)"
            + " values (:request, :insurer, :premium, :terms, :at, :user)"
            + " on conflict (request_id, insurer_code) do update set premium = excluded.premium,"
            + " terms = excluded.terms",
        new MapSqlParameterSource()
            .addValue("request", r.getId())
            .addValue("insurer", quote.insurerCode().strip())
            .addValue("premium", quote.premium())
            .addValue("terms", quote.terms())
            .addValue("at", java.sql.Timestamp.from(clock.instant()))
            .addValue("user", currentUser.username()));
    notifyOwner(r, requestNo + ": comparative table updated");
  }

  /**
   * Marketing's choice of insurers from the comparative table.
   *
   * @param companyId company
   * @param requestNo request
   * @param insurers insurers selected
   */
  @Transactional
  public void select(Long companyId, String requestNo, List<String> insurers) {
    TsuRequest r = request(companyId, requestNo);
    require(r, TsuRequest.PROCESSING, TsuRequest.COMPLETED);
    jdbc.update(
        "update rnw_tsu_quote set selected = (insurer_code = any(cast(:codes as varchar[])))"
            + " where request_id = :request",
        new MapSqlParameterSource()
            .addValue("codes", insurers.toArray(String[]::new))
            .addValue("request", r.getId()));
  }

  /**
   * Completes a request with the proposal prepared by TSU.
   *
   * @param companyId company
   * @param requestNo request
   * @param fileName proposal file name
   * @param content proposal
   * @return the request
   */
  @Transactional
  public TsuRequest complete(Long companyId, String requestNo, String fileName, byte[] content) {
    TsuRequest r = request(companyId, requestNo);
    require(r, TsuRequest.PROCESSING);
    RenewalCandidate c = records.byId(r.getCandidateId());
    Long id =
        storage
            .upload(
                new AttachmentTarget(RenewalCodes.ENTITY, c.getId().toString()),
                List.of(new DocumentService.UploadedFile(fileName, content)),
                new DocumentService.UploadOptions(
                    "QUOTATION", false, c.getRenewalRef(), "Proposal of " + requestNo, null))
            .get(0)
            .getId();
    r.complete(id);
    proposals.fromTsu(c, fileName, id);
    notifyOwner(r, requestNo + ": proposal completed by TSU");
    return r;
  }

  /**
   * The requests of an account with their comparative table.
   *
   * @param companyId company
   * @param ref renewal
   * @return requests
   */
  @Transactional(readOnly = true)
  public List<View> of(Long companyId, String ref) {
    RenewalCandidate c = records.get(companyId, ref);
    return requests.findByCandidateIdOrderByIdDesc(c.getId()).stream().map(this::view).toList();
  }

  /**
   * The requests waiting for TSU or for a Team Lead.
   *
   * @param companyId company
   * @param status status
   * @return requests
   */
  @Transactional(readOnly = true)
  public List<View> inStatus(Long companyId, String status) {
    return requests.findByCompanyIdAndStatusOrderByIdAsc(companyId, status).stream()
        .map(this::view)
        .toList();
  }

  private View view(TsuRequest r) {
    RenewalCandidate c = records.byId(r.getCandidateId());
    List<Map<String, Object>> quotes =
        jdbc.queryForList(
            "select insurer_code as \"insurerCode\", premium, terms, selected from rnw_tsu_quote"
                + " where request_id = :id order by premium nulls last",
            Map.of("id", r.getId()));
    return new View(
        r.getRequestNo(),
        c.getRenewalRef(),
        c.getSnapshot().clientName(),
        c.getSnapshot().product() == null ? null : c.getSnapshot().product().productCode(),
        c.getSnapshot().insurerCode(),
        c.getAssignedAo(),
        r.getStatus(),
        r.getRemarks(),
        r.getDecisionRemarks(),
        r.getTsuOfficer(),
        r.getProposalAttachmentId(),
        quotes);
  }

  private void notifyOwner(TsuRequest r, String title) {
    RenewalCandidate c = records.byId(r.getCandidateId());
    notices.users(
        List.of(
            r.getCreatedBy() == null ? "" : r.getCreatedBy(), String.valueOf(c.getAssignedAo())),
        RenewalCodes.EVENT_ASSIGNED,
        c,
        new RenewalNotices.Text(title, c.getSnapshot().clientName()));
    audit.record(RenewalCodes.ENTITY, c.getRenewalRef(), AuditAction.UPDATE, title);
  }

  private TsuRequest request(Long companyId, String requestNo) {
    TsuRequest r =
        requests
            .findByCompanyIdAndRequestNo(companyId, requestNo)
            .orElseThrow(
                () -> new BusinessRuleException("RNW_TSU_NOT_FOUND", "TSU request not found"));
    records.requireScope(records.byId(r.getCandidateId()));
    return r;
  }

  private static void require(TsuRequest r, String... statuses) {
    for (String s : statuses) {
      if (s.equals(r.getStatus())) {
        return;
      }
    }
    throw new BusinessRuleException(
        "RNW_TSU_STATUS", "TSU request " + r.getRequestNo() + " is " + r.getStatus());
  }

  /**
   * An insurer quotation.
   *
   * @param insurerCode insurer
   * @param premium premium quoted
   * @param terms terms and conditions
   */
  public record Quote(String insurerCode, BigDecimal premium, String terms) {}

  /**
   * A TSU request.
   *
   * @param requestNo number
   * @param renewalRef renewal
   * @param clientName client
   * @param riskCode risk code
   * @param insurerCode current insurer
   * @param accountOfficer Account Officer
   * @param status status
   * @param remarks remarks
   * @param decisionRemarks Team Lead remarks
   * @param tsuOfficer TSU Officer
   * @param proposalAttachmentId proposal of TSU
   * @param quotes comparative table
   */
  public record View(
      String requestNo,
      String renewalRef,
      String clientName,
      String riskCode,
      String insurerCode,
      String accountOfficer,
      String status,
      String remarks,
      String decisionRemarks,
      String tsuOfficer,
      Long proposalAttachmentId,
      List<Map<String, Object>> quotes) {

    /** Defensive copy. */
    public View {
      quotes = List.copyOf(quotes);
    }
  }
}
