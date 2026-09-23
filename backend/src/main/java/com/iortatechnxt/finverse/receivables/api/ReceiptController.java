package com.iortatechnxt.finverse.receivables.api;

import com.iortatechnxt.finverse.common.api.PageResponse;
import com.iortatechnxt.finverse.receivables.api.dto.ApplyRequest;
import com.iortatechnxt.finverse.receivables.api.dto.ReceiptRequest;
import com.iortatechnxt.finverse.receivables.api.dto.ReceiptResponse;
import com.iortatechnxt.finverse.receivables.api.dto.ReceiptSearchParams;
import com.iortatechnxt.finverse.receivables.api.dto.ReceiptSummaryResponse;
import com.iortatechnxt.finverse.receivables.api.dto.ReversalRequest;
import com.iortatechnxt.finverse.receivables.service.ReceiptPostingService;
import com.iortatechnxt.finverse.receivables.service.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Official receipts: entry, inquiry, approval, application of money on account, reversal. */
@RestController
@RequestMapping("/api/v1/receivables/receipts")
public class ReceiptController {

  private static final int MAX_PAGE = 200;

  private final ReceiptService receipts;
  private final ReceiptPostingService posting;

  /**
   * Creates the controller.
   *
   * @param receipts receipt service
   * @param posting receipt posting service
   */
  public ReceiptController(ReceiptService receipts, ReceiptPostingService posting) {
    this.receipts = receipts;
    this.posting = posting;
  }

  /**
   * Searches receipts, newest first.
   *
   * @param params filters
   * @param page page
   * @param size page size
   * @return page of receipts
   */
  @GetMapping
  @PreAuthorize(ReceivablesAccess.VIEW)
  public PageResponse<ReceiptSummaryResponse> search(
      @Valid @ModelAttribute ReceiptSearchParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    var result =
        receipts.search(
            params.toSearch(),
            PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE),
                Sort.by(Sort.Direction.DESC, "receiptDate", "id")));
    return PageResponse.of(result, ReceiptSummaryResponse::from);
  }

  /**
   * Gets a receipt with its allocations.
   *
   * @param id id
   * @return receipt
   */
  @GetMapping("/{id}")
  @PreAuthorize(ReceivablesAccess.VIEW)
  public ReceiptResponse get(@PathVariable Long id) {
    return ReceiptResponse.from(receipts.get(id));
  }

  /**
   * Enters a receipt (pending approval).
   *
   * @param request receipt
   * @return created receipt
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public ReceiptResponse create(@Valid @RequestBody ReceiptRequest request) {
    return ReceiptResponse.from(receipts.get(receipts.create(request).getId()));
  }

  /**
   * Approves a receipt (checker): posts the journals and matches the debit notes.
   *
   * @param id receipt
   * @return receipt
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_AUTHORIZE')")
  public ReceiptResponse approve(@PathVariable Long id) {
    posting.approve(id);
    return get(id);
  }

  /**
   * Rejects a pending receipt (checker).
   *
   * @param id receipt
   * @param request date and reason
   * @return receipt
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_AUTHORIZE')")
  public ReceiptResponse reject(
      @PathVariable Long id, @Valid @RequestBody ReversalRequest request) {
    receipts.reject(id, request);
    return get(id);
  }

  /**
   * Applies money held on account to debit notes.
   *
   * @param id receipt
   * @param request allocations
   * @return receipt
   */
  @PostMapping("/{id}/apply")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public ReceiptResponse apply(@PathVariable Long id, @Valid @RequestBody ApplyRequest request) {
    posting.applyUnapplied(id, request);
    return get(id);
  }

  /**
   * Cancels an approved receipt (reversal journal, debit notes re-opened).
   *
   * @param id receipt
   * @param request date and reason
   * @return receipt
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_AUTHORIZE')")
  public ReceiptResponse cancel(
      @PathVariable Long id, @Valid @RequestBody ReversalRequest request) {
    posting.cancel(id, request);
    return get(id);
  }

  /**
   * Records a bounced cheque (reversal journal, debit notes re-opened).
   *
   * @param id receipt
   * @param request date and reason
   * @return receipt
   */
  @PostMapping("/{id}/bounce")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_AUTHORIZE')")
  public ReceiptResponse bounce(
      @PathVariable Long id, @Valid @RequestBody ReversalRequest request) {
    posting.bounce(id, request);
    return get(id);
  }
}
