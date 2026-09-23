package com.iortatechnxt.finverse.payables.api;

import com.iortatechnxt.finverse.common.api.PageResponse;
import com.iortatechnxt.finverse.common.api.ReasonRequest;
import com.iortatechnxt.finverse.payables.api.dto.DatedReasonRequest;
import com.iortatechnxt.finverse.payables.api.dto.PayableItemResponse;
import com.iortatechnxt.finverse.payables.api.dto.PaymentRequest;
import com.iortatechnxt.finverse.payables.api.dto.VoucherResponse;
import com.iortatechnxt.finverse.payables.domain.VoucherStatus;
import com.iortatechnxt.finverse.payables.service.PaymentApprovalService;
import com.iortatechnxt.finverse.payables.service.PaymentVoucherService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Payment vouchers: pay open payables of any party by cheque, transfer or PDC. */
@RestController
@RequestMapping("/api/v1/payables/vouchers")
public class PaymentVoucherController {

  private final PaymentVoucherService service;
  private final PaymentApprovalService approvals;

  /**
   * Creates the controller.
   *
   * @param service capture service
   * @param approvals approval service
   */
  public PaymentVoucherController(PaymentVoucherService service, PaymentApprovalService approvals) {
    this.service = service;
    this.approvals = approvals;
  }

  /**
   * Lists the open payables of a party available for payment.
   *
   * @param companyId company
   * @param partyCode party
   * @param voucherId voucher being edited (optional)
   * @return payables
   */
  @GetMapping("/payable-items")
  @PreAuthorize(PayablesAccess.VIEW)
  public List<PayableItemResponse> payableItems(
      @RequestParam Long companyId,
      @RequestParam String partyCode,
      @RequestParam(required = false) Long voucherId) {
    return service.payableItems(companyId, partyCode, voucherId).stream()
        .map(PayableItemResponse::from)
        .toList();
  }

  /**
   * Searches vouchers.
   *
   * @param companyId company
   * @param status status
   * @param partyCode party
   * @param from from date
   * @param to to date
   * @param page page
   * @param size size
   * @return page
   */
  @GetMapping
  @PreAuthorize(PayablesAccess.VIEW)
  public PageResponse<VoucherResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) VoucherStatus status,
      @RequestParam(required = false) String partyCode,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        service.search(
            companyId,
            status,
            ApiDefaults.blankToNull(partyCode),
            ApiDefaults.from(from),
            ApiDefaults.to(to),
            ApiDefaults.page(page, size, "voucherDate")),
        VoucherResponse::summary);
  }

  /**
   * Gets a voucher.
   *
   * @param id id
   * @return voucher with allocations
   */
  @GetMapping("/{id}")
  @PreAuthorize(PayablesAccess.VIEW)
  public VoucherResponse get(@PathVariable Long id) {
    return VoucherResponse.from(service.get(id));
  }

  /**
   * Captures a draft voucher.
   *
   * @param request request
   * @return voucher
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(PayablesAccess.MAINTAIN)
  public VoucherResponse create(@Valid @RequestBody PaymentRequest request) {
    return VoucherResponse.from(service.create(request.toCommand()));
  }

  /**
   * Updates a draft voucher.
   *
   * @param id id
   * @param request request
   * @return voucher
   */
  @PutMapping("/{id}")
  @PreAuthorize(PayablesAccess.MAINTAIN)
  public VoucherResponse update(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
    return VoucherResponse.from(service.update(id, request.toCommand()));
  }

  /**
   * Submits for approval.
   *
   * @param id id
   * @return voucher
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(PayablesAccess.MAINTAIN)
  public VoucherResponse submit(@PathVariable Long id) {
    return VoucherResponse.from(service.submit(id));
  }

  /**
   * Approves, numbers and posts.
   *
   * @param id id
   * @return voucher
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public VoucherResponse approve(@PathVariable Long id) {
    return VoucherResponse.from(approvals.approve(id));
  }

  /**
   * Returns to the maker.
   *
   * @param id id
   * @param request reason
   * @return voucher
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public VoucherResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return VoucherResponse.from(service.reject(id, request.reason()));
  }

  /**
   * Cancels an unapproved voucher.
   *
   * @param id id
   * @param request reason
   * @return voucher
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(PayablesAccess.MAINTAIN_OR_AUTHORIZE)
  public VoucherResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return VoucherResponse.from(service.cancel(id, request.reason()));
  }

  /**
   * Confirms cheque presentation.
   *
   * @param id id
   * @param request presentation date
   * @return voucher
   */
  @PostMapping("/{id}/presented")
  @PreAuthorize(PayablesAccess.MAINTAIN_OR_AUTHORIZE)
  public VoucherResponse presented(
      @PathVariable Long id, @Valid @RequestBody DatedReasonRequest request) {
    return VoucherResponse.from(approvals.markPresented(id, request.date()));
  }

  /**
   * Voids an unpresented cheque (reversal).
   *
   * @param id id
   * @param request void date and reason
   * @return voucher
   */
  @PostMapping("/{id}/void")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public VoucherResponse voidCheque(
      @PathVariable Long id, @Valid @RequestBody DatedReasonRequest request) {
    return VoucherResponse.from(
        approvals.voidCheque(
            id, request.date(), ApiDefaults.reasonOrDefault(request.reason(), "Cheque voided")));
  }
}
