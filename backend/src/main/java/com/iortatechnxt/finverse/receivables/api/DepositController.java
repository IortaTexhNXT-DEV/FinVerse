package com.iortatechnxt.finverse.receivables.api;

import com.iortatechnxt.finverse.receivables.api.dto.DateRequest;
import com.iortatechnxt.finverse.receivables.api.dto.DepositSlipRequest;
import com.iortatechnxt.finverse.receivables.api.dto.DepositSlipResponse;
import com.iortatechnxt.finverse.receivables.api.dto.ReceiptSummaryResponse;
import com.iortatechnxt.finverse.receivables.service.DepositService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cheques and cash not yet banked, and bank deposit slips. */
@RestController
@RequestMapping("/api/v1/receivables/deposits")
public class DepositController {

  private final DepositService deposits;

  /**
   * Creates the controller.
   *
   * @param deposits deposit service
   */
  public DepositController(DepositService deposits) {
    this.deposits = deposits;
  }

  /**
   * Lists approved cash and cheque receipts not yet deposited.
   *
   * @param companyId company
   * @param bankAccountCode bank account filter
   * @return receipts
   */
  @GetMapping("/undeposited")
  @PreAuthorize(ReceivablesAccess.VIEW)
  public List<ReceiptSummaryResponse> undeposited(
      @RequestParam Long companyId, @RequestParam(required = false) String bankAccountCode) {
    return deposits.undeposited(companyId, bankAccountCode).stream()
        .map(ReceiptSummaryResponse::from)
        .toList();
  }

  /**
   * Lists deposit slips.
   *
   * @param companyId company
   * @return slips newest first
   */
  @GetMapping("/slips")
  @PreAuthorize(ReceivablesAccess.VIEW)
  public List<DepositSlipResponse> slips(@RequestParam Long companyId) {
    return deposits.list(companyId).stream()
        .map(s -> DepositSlipResponse.from(s, List.of()))
        .toList();
  }

  /**
   * Gets a slip with its receipts.
   *
   * @param id slip
   * @return slip
   */
  @GetMapping("/slips/{id}")
  @PreAuthorize(ReceivablesAccess.VIEW)
  public DepositSlipResponse slip(@PathVariable Long id) {
    return DepositSlipResponse.from(
        deposits.get(id),
        deposits.receiptsOf(id).stream().map(ReceiptSummaryResponse::from).toList());
  }

  /**
   * Prepares a deposit slip.
   *
   * @param request slip
   * @return slip
   */
  @PostMapping("/slips")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public DepositSlipResponse create(@Valid @RequestBody DepositSlipRequest request) {
    return slip(deposits.create(request).getId());
  }

  /**
   * Confirms that a slip was deposited.
   *
   * @param id slip
   * @param request deposit date
   * @return slip
   */
  @PostMapping("/slips/{id}/confirm")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public DepositSlipResponse confirm(
      @PathVariable Long id, @Valid @RequestBody DateRequest request) {
    deposits.confirm(id, request);
    return slip(id);
  }

  /**
   * Cancels a prepared slip.
   *
   * @param id slip
   * @return slip
   */
  @PostMapping("/slips/{id}/cancel")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public DepositSlipResponse cancel(@PathVariable Long id) {
    deposits.cancel(id);
    return slip(id);
  }
}
