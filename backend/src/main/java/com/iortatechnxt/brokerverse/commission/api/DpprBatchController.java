package com.iortatechnxt.brokerverse.commission.api;

import com.iortatechnxt.brokerverse.commission.domain.DpprBatch;
import com.iortatechnxt.brokerverse.commission.domain.DpprBatchLine;
import com.iortatechnxt.brokerverse.commission.service.DpprBatchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy direct payment PR reversal batches (screen "DP PR Legacy Reversal"): legacy invoices paid
 * directly to the insurer, prepared by the Commission user, approved by the team lead and posted
 * line by line.
 */
@RestController
@RequestMapping("/api/v1/commission/dppr-batches")
public class DpprBatchController {

  private static final String VIEW =
      "hasAnyAuthority('LEGACY_REVERSAL_REQUEST', 'LEGACY_REVERSAL_APPROVE')";
  private static final String REQUEST = "hasAuthority('LEGACY_REVERSAL_REQUEST')";
  private static final String APPROVE = "hasAuthority('LEGACY_REVERSAL_APPROVE')";

  private final DpprBatchService batches;

  /**
   * Creates the controller.
   *
   * @param batches batches
   */
  public DpprBatchController(DpprBatchService batches) {
    this.batches = batches;
  }

  /**
   * Batches of a company.
   *
   * @param companyId company
   * @return batches
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<BatchResponse> list(@RequestParam Long companyId) {
    return batches.list(companyId).stream().map(BatchResponse::from).toList();
  }

  /**
   * Legacy invoices that may be reversed.
   *
   * @param companyId company
   * @param taggedOnly only those tagged "DP PR for reversal" by Collections
   * @return candidates
   */
  @GetMapping("/candidates")
  @PreAuthorize(REQUEST)
  public List<DpprBatchService.Candidate> candidates(
      @RequestParam Long companyId, @RequestParam(defaultValue = "false") boolean taggedOnly) {
    return batches.candidates(companyId, taggedOnly);
  }

  /**
   * A batch with its lines.
   *
   * @param batchNo batch
   * @return detail
   */
  @GetMapping("/{batchNo}")
  @PreAuthorize(VIEW)
  public BatchDetail get(@PathVariable String batchNo) {
    DpprBatch b = batches.get(batchNo);
    return new BatchDetail(
        BatchResponse.from(b), batches.lines(b).stream().map(LineResponse::from).toList());
  }

  /**
   * Opens a batch.
   *
   * @param companyId company
   * @param body reason
   * @return the batch
   */
  @PostMapping
  @PreAuthorize(REQUEST)
  public BatchResponse create(@RequestParam Long companyId, @Valid @RequestBody CommentBody body) {
    return BatchResponse.from(batches.create(companyId, body.comment()));
  }

  /**
   * Adds a legacy invoice.
   *
   * @param batchNo batch
   * @param body invoice, amount of the upload and reason
   * @return the line
   */
  @PostMapping("/{batchNo}/invoices")
  @PreAuthorize(REQUEST)
  public LineResponse addInvoice(
      @PathVariable String batchNo, @Valid @RequestBody InvoiceBody body) {
    return LineResponse.from(
        batches.addInvoice(batchNo, body.invoiceNo(), body.amount(), body.reason()));
  }

  /**
   * Removes a line of a draft.
   *
   * @param batchNo batch
   * @param lineId line
   */
  @DeleteMapping("/{batchNo}/lines/{lineId}")
  @PreAuthorize(REQUEST)
  public void removeLine(@PathVariable String batchNo, @PathVariable Long lineId) {
    batches.removeLine(batchNo, lineId);
  }

  /**
   * Submits a draft.
   *
   * @param batchNo batch
   * @return the batch
   */
  @PostMapping("/{batchNo}/submit")
  @PreAuthorize(REQUEST)
  public BatchResponse submit(@PathVariable String batchNo) {
    return BatchResponse.from(batches.submit(batchNo));
  }

  /**
   * Cancels a draft.
   *
   * @param batchNo batch
   * @return the batch
   */
  @PostMapping("/{batchNo}/cancel")
  @PreAuthorize(REQUEST)
  public BatchResponse cancel(@PathVariable String batchNo) {
    return BatchResponse.from(batches.cancel(batchNo));
  }

  /**
   * Approves and posts the batch.
   *
   * @param batchNo batch
   * @param body comment
   * @return the batch
   */
  @PostMapping("/{batchNo}/approve")
  @PreAuthorize(APPROVE)
  public BatchResponse approve(@PathVariable String batchNo, @RequestBody CommentBody body) {
    return BatchResponse.from(batches.approve(batchNo, body.comment()));
  }

  /**
   * Returns the batch to its requester.
   *
   * @param batchNo batch
   * @param body reason
   * @return the batch
   */
  @PostMapping("/{batchNo}/return")
  @PreAuthorize(APPROVE)
  public BatchResponse returnBatch(@PathVariable String batchNo, @RequestBody CommentBody body) {
    return BatchResponse.from(batches.returnBatch(batchNo, body.comment()));
  }

  /**
   * A legacy invoice to add.
   *
   * @param invoiceNo invoice
   * @param amount amount of the upload, checked against the open premium receivable (optional)
   * @param reason reason
   */
  public record InvoiceBody(
      @NotBlank String invoiceNo, BigDecimal amount, @Size(max = 250) String reason) {}

  /**
   * A reason or comment.
   *
   * @param comment text
   */
  public record CommentBody(@Size(max = 500) String comment) {}

  /**
   * A batch.
   *
   * @param batchNo number
   * @param status status
   * @param reason reason
   * @param total total
   * @param lineCount lines
   * @param createdBy requester
   * @param submittedAt submitted
   * @param approvedBy approver
   * @param executedAt posted
   * @param postedCount lines posted
   * @param failedCount lines refused
   * @param returnReason last return reason
   */
  public record BatchResponse(
      String batchNo,
      DpprBatch.Status status,
      String reason,
      BigDecimal total,
      int lineCount,
      String createdBy,
      Instant submittedAt,
      String approvedBy,
      Instant executedAt,
      int postedCount,
      int failedCount,
      String returnReason) {

    static BatchResponse from(DpprBatch b) {
      return new BatchResponse(
          b.getBatchNo(),
          b.getStatus(),
          b.getReason(),
          b.getTotal(),
          b.getLineCount(),
          b.getCreatedBy(),
          b.getSubmittedAt(),
          b.getApprovedBy(),
          b.getExecutedAt(),
          b.getPostedCount(),
          b.getFailedCount(),
          b.getReturnReason());
    }
  }

  /**
   * A line.
   *
   * @param id id
   * @param lineNo number
   * @param invoiceNo invoice
   * @param amount open premium receivable reversed
   * @param reason reason
   * @param status status
   * @param journalBatchNo journal
   * @param message refusal
   */
  public record LineResponse(
      Long id,
      int lineNo,
      String invoiceNo,
      BigDecimal amount,
      String reason,
      String status,
      String journalBatchNo,
      String message) {

    static LineResponse from(DpprBatchLine l) {
      return new LineResponse(
          l.getId(),
          l.getLineNo(),
          l.getInvoiceNo(),
          l.getAmount(),
          l.getReason(),
          l.getStatus(),
          l.getJournalBatchNo(),
          l.getMessage());
    }
  }

  /**
   * A batch with its lines.
   *
   * @param batch batch
   * @param lines lines
   */
  public record BatchDetail(BatchResponse batch, List<LineResponse> lines) {

    /** Defensive copy. */
    public BatchDetail {
      lines = List.copyOf(lines);
    }
  }
}
