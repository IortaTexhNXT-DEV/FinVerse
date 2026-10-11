package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatchLine;
import com.iortatechnxt.brokerverse.cashiering.service.LegacyBatchService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
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
 * The Cashiering legacy batches (screens "Unapplied to Income" and "Legacy PR 2307 Reversal"):
 * reclassification of old unapplied payments to other income and reversal of legacy PR 2307
 * balances, each prepared, submitted, approved and posted line by line.
 */
@RestController
@RequestMapping("/api/v1/cashiering/legacy-batches")
public class LegacyBatchController {

  private static final String VIEW =
      "hasAnyAuthority('CASH_UPP_INCOME_REQUEST', 'CASH_UPP_INCOME_APPROVE',"
          + " 'CASH_DISPOSITION_APPROVE', 'LEGACY_REVERSAL_REQUEST', 'LEGACY_REVERSAL_APPROVE')";
  private static final String INCOME_REQUEST = "CASH_UPP_INCOME_REQUEST";
  private static final String REVERSAL_REQUEST = "LEGACY_REVERSAL_REQUEST";

  private final LegacyBatchService batches;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param batches batches
   * @param currentUser current user
   */
  public LegacyBatchController(LegacyBatchService batches, CurrentUser currentUser) {
    this.batches = batches;
    this.currentUser = currentUser;
  }

  /**
   * Batches of a kind.
   *
   * @param companyId company
   * @param kind INCOME_RECLASS or PR2307_REVERSAL
   * @return batches
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<BatchResponse> list(
      @RequestParam Long companyId, @RequestParam LegacyBatch.Kind kind) {
    return batches.list(companyId, kind).stream().map(BatchResponse::from).toList();
  }

  /**
   * Unapplied items that may be taken to income.
   *
   * @param companyId company
   * @param minAgeDays minimum age
   * @param origin MIGRATED or BIBS, all when absent
   * @return candidates
   */
  @GetMapping("/candidates")
  @PreAuthorize("hasAuthority('CASH_UPP_INCOME_REQUEST')")
  public List<LegacyBatchService.Candidate> candidates(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int minAgeDays,
      @RequestParam(required = false) String origin) {
    return batches.candidates(companyId, Math.max(0, minAgeDays), origin);
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
    LegacyBatch b = batches.get(batchNo);
    return new BatchDetail(
        BatchResponse.from(b), batches.lines(b).stream().map(LineResponse::from).toList());
  }

  /**
   * Opens a batch.
   *
   * @param companyId company
   * @param body kind, reason and currency
   * @return the batch
   */
  @PostMapping
  @PreAuthorize("hasAnyAuthority('CASH_UPP_INCOME_REQUEST', 'LEGACY_REVERSAL_REQUEST')")
  public BatchResponse create(@RequestParam Long companyId, @Valid @RequestBody CreateBody body) {
    requester(body.kind());
    return BatchResponse.from(
        batches.create(companyId, body.kind(), body.reason(), body.currency()));
  }

  /**
   * Adds an unapplied item.
   *
   * @param batchNo batch
   * @param body item and reason
   * @return the line
   */
  @PostMapping("/{batchNo}/items")
  @PreAuthorize("hasAuthority('CASH_UPP_INCOME_REQUEST')")
  public LineResponse addItem(@PathVariable String batchNo, @Valid @RequestBody ItemBody body) {
    return LineResponse.from(batches.addUnapplied(batchNo, body.unappliedId(), body.reason()));
  }

  /**
   * Adds a legacy invoice.
   *
   * @param batchNo batch
   * @param body invoice, amount and reason
   * @return the line
   */
  @PostMapping("/{batchNo}/invoices")
  @PreAuthorize("hasAuthority('LEGACY_REVERSAL_REQUEST')")
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
  @PreAuthorize("hasAnyAuthority('CASH_UPP_INCOME_REQUEST', 'LEGACY_REVERSAL_REQUEST')")
  public void removeLine(@PathVariable String batchNo, @PathVariable Long lineId) {
    requester(batches.get(batchNo).getKind());
    batches.removeLine(batchNo, lineId);
  }

  /**
   * Submits a draft.
   *
   * @param batchNo batch
   * @return the batch
   */
  @PostMapping("/{batchNo}/submit")
  @PreAuthorize("hasAnyAuthority('CASH_UPP_INCOME_REQUEST', 'LEGACY_REVERSAL_REQUEST')")
  public BatchResponse submit(@PathVariable String batchNo) {
    requester(batches.get(batchNo).getKind());
    return BatchResponse.from(batches.submit(batchNo));
  }

  /**
   * Cancels a draft.
   *
   * @param batchNo batch
   * @return the batch
   */
  @PostMapping("/{batchNo}/cancel")
  @PreAuthorize("hasAnyAuthority('CASH_UPP_INCOME_REQUEST', 'LEGACY_REVERSAL_REQUEST')")
  public BatchResponse cancel(@PathVariable String batchNo) {
    requester(batches.get(batchNo).getKind());
    return BatchResponse.from(batches.cancel(batchNo));
  }

  /**
   * Approves the batch at its level; the last approval posts it.
   *
   * @param batchNo batch
   * @param body comment
   * @return the batch
   */
  @PostMapping("/{batchNo}/approve")
  @PreAuthorize(VIEW)
  public BatchResponse approve(@PathVariable String batchNo, @RequestBody CommentBody body) {
    approver(batches.get(batchNo));
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
  @PreAuthorize(VIEW)
  public BatchResponse returnBatch(@PathVariable String batchNo, @RequestBody CommentBody body) {
    approver(batches.get(batchNo));
    return BatchResponse.from(batches.returnBatch(batchNo, body.comment()));
  }

  private void requester(LegacyBatch.Kind kind) {
    require(kind == LegacyBatch.Kind.INCOME_RECLASS ? INCOME_REQUEST : REVERSAL_REQUEST);
  }

  private void approver(LegacyBatch b) {
    if (b.getStatus() == LegacyBatch.Status.FOR_TOP_MANAGEMENT) {
      require("CASH_UPP_INCOME_APPROVE");
    } else {
      require(
          b.getKind() == LegacyBatch.Kind.INCOME_RECLASS
              ? "CASH_DISPOSITION_APPROVE"
              : "LEGACY_REVERSAL_APPROVE");
    }
  }

  private void require(String permission) {
    if (!currentUser.hasAuthority(permission)) {
      throw new AccessDeniedException("Permission " + permission + " is required");
    }
  }

  /**
   * A batch to open.
   *
   * @param kind kind
   * @param reason reason
   * @param currency currency
   */
  public record CreateBody(
      @NotNull LegacyBatch.Kind kind,
      @NotBlank @Size(max = 500) String reason,
      @NotBlank @Size(min = 3, max = 3) String currency) {}

  /**
   * An unapplied item to add.
   *
   * @param unappliedId item
   * @param reason reason
   */
  public record ItemBody(@NotNull Long unappliedId, @Size(max = 250) String reason) {}

  /**
   * A legacy invoice to add.
   *
   * @param invoiceNo invoice
   * @param amount amount
   * @param reason reason
   */
  public record InvoiceBody(
      @NotBlank String invoiceNo, @NotNull BigDecimal amount, @Size(max = 250) String reason) {}

  /**
   * A comment or reason.
   *
   * @param comment text
   */
  public record CommentBody(@Size(max = 500) String comment) {}

  /**
   * A batch.
   *
   * @param batchNo number
   * @param kind kind
   * @param status status
   * @param reason reason
   * @param currency currency
   * @param total total
   * @param lineCount lines
   * @param createdBy requester
   * @param submittedAt submitted
   * @param firstApprovedBy team lead
   * @param finalApprovedBy last approver
   * @param executedAt posted
   * @param postedCount lines posted
   * @param failedCount lines refused
   * @param returnReason last return reason
   */
  public record BatchResponse(
      String batchNo,
      LegacyBatch.Kind kind,
      LegacyBatch.Status status,
      String reason,
      String currency,
      BigDecimal total,
      int lineCount,
      String createdBy,
      Instant submittedAt,
      String firstApprovedBy,
      String finalApprovedBy,
      Instant executedAt,
      int postedCount,
      int failedCount,
      String returnReason) {

    static BatchResponse from(LegacyBatch b) {
      return new BatchResponse(
          b.getBatchNo(),
          b.getKind(),
          b.getStatus(),
          b.getReason(),
          b.getCurrency(),
          b.getTotal(),
          b.getLineCount(),
          b.getCreatedBy(),
          b.getSubmittedAt(),
          b.getFirstApprovedBy(),
          b.getFinalApprovedBy(),
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
   * @param unappliedId item
   * @param invoiceNo invoice
   * @param reference reference
   * @param ledgerContext NEW or LEGACY
   * @param amount amount
   * @param ageDays age
   * @param reason reason
   * @param status status
   * @param journalBatchNo journal
   * @param message refusal
   */
  public record LineResponse(
      Long id,
      int lineNo,
      Long unappliedId,
      String invoiceNo,
      String reference,
      String ledgerContext,
      BigDecimal amount,
      Integer ageDays,
      String reason,
      String status,
      String journalBatchNo,
      String message) {

    static LineResponse from(LegacyBatchLine l) {
      return new LineResponse(
          l.getId(),
          l.getLineNo(),
          l.getUnappliedId(),
          l.getInvoiceNo(),
          l.getReference(),
          l.getLedgerContext(),
          l.getAmount(),
          l.getAgeDays(),
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
