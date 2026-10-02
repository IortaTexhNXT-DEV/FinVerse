package com.iortatechnxt.brokerverse.receivables.api;

import com.iortatechnxt.brokerverse.receivables.api.dto.DateRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.PdcReplaceRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.PdcRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.PdcResponse;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReceiptResponse;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReversalRequest;
import com.iortatechnxt.brokerverse.receivables.domain.PdcStatus;
import com.iortatechnxt.brokerverse.receivables.service.PdcService;
import com.iortatechnxt.brokerverse.receivables.service.ReceiptService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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

/** Register of post-dated cheques received. */
@RestController
@RequestMapping("/api/v1/receivables/pdcs")
public class PdcController {

  private final PdcService pdcs;
  private final ReceiptService receipts;

  /**
   * Creates the controller.
   *
   * @param pdcs PDC service
   * @param receipts receipt service
   */
  public PdcController(PdcService pdcs, ReceiptService receipts) {
    this.pdcs = pdcs;
    this.receipts = receipts;
  }

  /**
   * Lists cheques.
   *
   * @param companyId company
   * @param status status filter
   * @return cheques
   */
  @GetMapping
  @PreAuthorize(ReceivablesAccess.VIEW)
  public List<PdcResponse> list(
      @RequestParam Long companyId, @RequestParam(required = false) PdcStatus status) {
    return pdcs.list(companyId, status).stream().map(p -> PdcResponse.from(p, List.of())).toList();
  }

  /**
   * Gets a cheque with its status history (confirmation audit trail).
   *
   * @param id cheque
   * @return cheque
   */
  @GetMapping("/{id}")
  @PreAuthorize(ReceivablesAccess.VIEW)
  public PdcResponse get(@PathVariable Long id) {
    return PdcResponse.from(pdcs.get(id), pdcs.history(id));
  }

  /**
   * Registers a cheque received.
   *
   * @param request cheque
   * @return cheque
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public PdcResponse register(@Valid @RequestBody PdcRequest request) {
    return get(pdcs.register(request).getId());
  }

  /**
   * Marks cheques whose date has been reached as due to be banked.
   *
   * @param companyId company
   * @param asOf date
   * @return cheques marked due
   */
  @PostMapping("/mark-due")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public List<PdcResponse> markDue(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
    return pdcs.markDue(companyId, asOf).stream().map(p -> PdcResponse.from(p, List.of())).toList();
  }

  /**
   * Banks a cheque: raises a receipt pending approval.
   *
   * @param id cheque
   * @param request deposit date
   * @return the receipt raised
   */
  @PostMapping("/{id}/deposit")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public ReceiptResponse deposit(@PathVariable Long id, @Valid @RequestBody DateRequest request) {
    return ReceiptResponse.from(receipts.get(pdcs.deposit(id, request).getId()));
  }

  /**
   * Confirms the realisation of a banked cheque.
   *
   * @param id cheque
   * @param request clearing date
   * @return cheque
   */
  @PostMapping("/{id}/clear")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public PdcResponse clear(@PathVariable Long id, @Valid @RequestBody DateRequest request) {
    pdcs.clear(id, request);
    return get(id);
  }

  /**
   * Records a dishonoured cheque (reverses its receipt).
   *
   * @param id cheque
   * @param request date and reason
   * @return cheque
   */
  @PostMapping("/{id}/bounce")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_AUTHORIZE')")
  public PdcResponse bounce(@PathVariable Long id, @Valid @RequestBody ReversalRequest request) {
    pdcs.bounce(id, request);
    return get(id);
  }

  /**
   * Returns a cheque on hand to the customer.
   *
   * @param id cheque
   * @param request date and reason
   * @return cheque
   */
  @PostMapping("/{id}/return")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public PdcResponse returnCheque(
      @PathVariable Long id, @Valid @RequestBody ReversalRequest request) {
    pdcs.returnCheque(id, request);
    return get(id);
  }

  /**
   * Replaces a cheque on hand by a new one.
   *
   * @param id cheque
   * @param request new cheque
   * @return the new cheque
   */
  @PostMapping("/{id}/replace")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public PdcResponse replace(@PathVariable Long id, @Valid @RequestBody PdcReplaceRequest request) {
    return get(pdcs.replace(id, request).getId());
  }
}
