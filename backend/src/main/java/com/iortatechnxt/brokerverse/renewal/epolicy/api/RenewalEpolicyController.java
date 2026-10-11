package com.iortatechnxt.brokerverse.renewal.epolicy.api;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.domain.EpolicyReceipt;
import com.iortatechnxt.brokerverse.renewal.domain.EpolicyReceiptRepository;
import com.iortatechnxt.brokerverse.renewal.epolicy.service.EpolicyReceipts;
import com.iortatechnxt.brokerverse.renewal.epolicy.service.EpolicySending;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * E-policies of the renewal accounts (FRRN.033): upload of the summary and ZIP files, the receipts
 * with their file-level outcome and record-level results, and the sending through CCM.
 */
@RestController
@RequestMapping("/api/v1/renewal/epolicy")
public class RenewalEpolicyController {

  private static final String PROCESS = "hasAuthority('RNW_PROCESS')";
  private static final int PAGE = 200;

  private final EpolicyReceipts receipts;
  private final EpolicyReceiptRepository receiptRows;
  private final EpolicySending sending;
  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the controller.
   *
   * @param receipts receipt processing
   * @param receiptRows receipts
   * @param sending sending
   * @param jdbc record-level results
   */
  public RenewalEpolicyController(
      EpolicyReceipts receipts,
      EpolicyReceiptRepository receiptRows,
      EpolicySending sending,
      NamedParameterJdbcTemplate jdbc) {
    this.receipts = receipts;
    this.receiptRows = receiptRows;
    this.sending = sending;
    this.jdbc = jdbc;
  }

  /**
   * Uploads the e-policy files of an insurer.
   *
   * @param companyId company
   * @param summary summary file (.txt)
   * @param zip ZIP file of the e-policy documents
   * @return the receipt
   */
  @PostMapping("/receipts")
  @PreAuthorize(PROCESS)
  public Receipt upload(
      @RequestParam Long companyId,
      @RequestPart(value = "summary", required = false) MultipartFile summary,
      @RequestPart(value = "zip", required = false) MultipartFile zip) {
    return Receipt.of(receipts.receive(companyId, "UPLOAD", incoming(summary), incoming(zip)));
  }

  /**
   * The receipts, latest first.
   *
   * @param companyId company
   * @param search file name fragment
   * @param status SUCCESSFUL or FAILED
   * @return receipts
   */
  @GetMapping("/receipts")
  @PreAuthorize(PROCESS)
  @Transactional(readOnly = true)
  public List<Receipt> receipts(
      @RequestParam Long companyId,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status) {
    String text = search == null ? "" : search.strip();
    return receiptRows.findByCompanyIdOrderByIdDesc(companyId, PageRequest.of(0, PAGE)).stream()
        .filter(r -> status == null || status.isBlank() || status.equals(r.getStatus()))
        .filter(r -> text.isEmpty() || matches(r, text))
        .map(Receipt::of)
        .toList();
  }

  /**
   * The record-level results of a receipt.
   *
   * @param companyId company
   * @param receiptNo receipt
   * @param match MATCHED or UNMATCHED
   * @param search reference or policy number fragment
   * @return records
   */
  @GetMapping("/receipts/{receiptNo}/lines")
  @PreAuthorize(PROCESS)
  public List<Map<String, Object>> lines(
      @RequestParam Long companyId,
      @PathVariable String receiptNo,
      @RequestParam(required = false) String match,
      @RequestParam(required = false) String search) {
    Map<String, Object> args = new HashMap<>();
    args.put("companyId", companyId);
    args.put("receiptNo", receiptNo);
    args.put("match", match == null || match.isBlank() ? null : match);
    args.put("search", search == null || search.isBlank() ? null : "%" + search.strip() + "%");
    return jdbc.queryForList(
        "select l.seq_no as \"seq\", l.renewal_ref as \"renewalRef\", l.policy_no as \"policyNo\","
            + " l.pdf_file as \"pdfFile\", l.document_tag as \"documentTag\","
            + " l.match_status as \"matchStatus\", l.remarks as \"remarks\","
            + " l.attachment_id as \"attachmentId\""
            + " from rnw_epolicy_line l join rnw_epolicy_receipt r on r.id = l.receipt_id"
            + " where r.company_id = :companyId and r.receipt_no = :receiptNo"
            + " and (cast(:match as varchar) is null or l.match_status = :match)"
            + " and (cast(:search as varchar) is null or l.renewal_ref ilike :search"
            + " or l.policy_no ilike :search)"
            + " order by l.seq_no",
        args);
  }

  /**
   * The accounts For E-Policy Sending with whether each can be sent.
   *
   * @param companyId company
   * @return accounts
   */
  @GetMapping("/sending")
  @PreAuthorize(PROCESS)
  public List<EpolicySending.Account> forSending(@RequestParam Long companyId) {
    return sending.accounts(companyId);
  }

  /**
   * Sends the e-policies of accounts through CCM.
   *
   * @param companyId company
   * @param body accounts and copy recipients
   * @return summary
   */
  @PostMapping("/send")
  @PreAuthorize(PROCESS)
  public EpolicySending.Summary send(@RequestParam Long companyId, @RequestBody Send body) {
    return sending.send(companyId, body.refs(), body.cc());
  }

  private static boolean matches(EpolicyReceipt r, String text) {
    return contains(r.getSummaryFile(), text) || contains(r.getZipFile(), text);
  }

  private static boolean contains(String value, String text) {
    return value != null && value.contains(text);
  }

  private static EpolicyReceipts.Incoming incoming(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    if (file.getOriginalFilename() == null) {
      throw new BusinessRuleException("RNW_EPOLICY_FILE", "The file has no name");
    }
    try {
      return new EpolicyReceipts.Incoming(file.getOriginalFilename(), file.getBytes());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * A sending.
   *
   * @param refs accounts
   * @param cc copy recipients
   */
  public record Send(List<String> refs, List<String> cc) {}

  /**
   * A receipt.
   *
   * @param receiptNo identifier
   * @param receiptType MFT or UPLOAD
   * @param summaryFile summary file
   * @param zipFile ZIP file
   * @param status SUCCESSFUL or FAILED
   * @param remarks reason or remarks
   * @param records records of the summary file
   * @param matched matched records
   * @param receivedAt receipt time
   * @param receivedBy user
   */
  public record Receipt(
      String receiptNo,
      String receiptType,
      String summaryFile,
      String zipFile,
      String status,
      String remarks,
      int records,
      int matched,
      Instant receivedAt,
      String receivedBy) {

    static Receipt of(EpolicyReceipt r) {
      return new Receipt(
          r.getReceiptNo(),
          r.getReceiptType(),
          r.getSummaryFile(),
          r.getZipFile(),
          r.getStatus(),
          r.getRemarks(),
          r.getRecords(),
          r.getMatched(),
          r.getCreatedAt(),
          r.getCreatedBy());
    }
  }
}
