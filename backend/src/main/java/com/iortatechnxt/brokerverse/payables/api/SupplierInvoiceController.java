package com.iortatechnxt.brokerverse.payables.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.payables.api.dto.InvoiceRequest;
import com.iortatechnxt.brokerverse.payables.api.dto.InvoiceResponse;
import com.iortatechnxt.brokerverse.payables.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.payables.service.SupplierInvoiceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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

/** Supplier invoices (accounts payable) with maker-checker approval. */
@RestController
@RequestMapping("/api/v1/payables/invoices")
public class SupplierInvoiceController {

  private final SupplierInvoiceService service;

  /**
   * Creates the controller.
   *
   * @param service invoice service
   */
  public SupplierInvoiceController(SupplierInvoiceService service) {
    this.service = service;
  }

  /**
   * Searches invoices.
   *
   * @param companyId company
   * @param status status filter
   * @param partyCode supplier filter
   * @param from invoice date from (default 2000-01-01)
   * @param to invoice date to (default 2999-12-31)
   * @param page page
   * @param size page size
   * @return page
   */
  @GetMapping
  @PreAuthorize(PayablesAccess.VIEW)
  public PageResponse<InvoiceResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) InvoiceStatus status,
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
            ApiDefaults.page(page, size, "invoiceDate")),
        InvoiceResponse::summary);
  }

  /**
   * Gets an invoice.
   *
   * @param id id
   * @return invoice with lines
   */
  @GetMapping("/{id}")
  @PreAuthorize(PayablesAccess.VIEW)
  public InvoiceResponse get(@PathVariable Long id) {
    return InvoiceResponse.from(service.get(id));
  }

  /**
   * Captures a draft invoice.
   *
   * @param request request
   * @return invoice
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(PayablesAccess.MAINTAIN)
  public InvoiceResponse create(@Valid @RequestBody InvoiceRequest request) {
    return InvoiceResponse.from(service.create(request.toCommand()));
  }

  /**
   * Updates a draft invoice.
   *
   * @param id id
   * @param request request
   * @return invoice
   */
  @PutMapping("/{id}")
  @PreAuthorize(PayablesAccess.MAINTAIN)
  public InvoiceResponse update(@PathVariable Long id, @Valid @RequestBody InvoiceRequest request) {
    return InvoiceResponse.from(service.update(id, request.toCommand()));
  }

  /**
   * Submits for approval.
   *
   * @param id id
   * @return invoice
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(PayablesAccess.MAINTAIN)
  public InvoiceResponse submit(@PathVariable Long id) {
    return InvoiceResponse.from(service.submit(id));
  }

  /**
   * Approves and posts.
   *
   * @param id id
   * @return invoice
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public InvoiceResponse approve(@PathVariable Long id) {
    return InvoiceResponse.from(service.approve(id));
  }

  /**
   * Returns to the maker.
   *
   * @param id id
   * @param request reason
   * @return invoice
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(PayablesAccess.AUTHORIZE)
  public InvoiceResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return InvoiceResponse.from(service.reject(id, request.reason()));
  }

  /**
   * Cancels an unapproved invoice.
   *
   * @param id id
   * @param request reason
   * @return invoice
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(PayablesAccess.MAINTAIN_OR_AUTHORIZE)
  public InvoiceResponse cancel(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return InvoiceResponse.from(service.cancel(id, request.reason()));
  }
}
