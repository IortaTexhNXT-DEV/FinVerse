package com.iortatechnxt.brokerverse.booking.api;

import com.iortatechnxt.brokerverse.booking.api.dto.ServiceInvoiceCreditRequest;
import com.iortatechnxt.brokerverse.booking.api.dto.ServiceInvoiceIssueRequest;
import com.iortatechnxt.brokerverse.booking.api.dto.ServiceInvoiceResponse;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceRegister;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service invoice register (BRNB.100/100b): search, view, download, resend, manual issue and
 * credit.
 */
@RestController
@RequestMapping("/api/v1/booking/service-invoices")
public class ServiceInvoiceController {

  private final ServiceInvoiceService serviceInvoices;
  private final ServiceInvoiceRegister register;

  /**
   * Creates the controller.
   *
   * @param serviceInvoices issue, credit, resend
   * @param register register reads
   */
  public ServiceInvoiceController(
      ServiceInvoiceService serviceInvoices, ServiceInvoiceRegister register) {
    this.serviceInvoices = serviceInvoices;
    this.register = register;
  }

  /**
   * Searches the register.
   *
   * @param companyId company
   * @param q number, invoice, ARN or recipient
   * @param kind invoice or credit
   * @param page page
   * @param size size
   * @return service invoices, newest first
   */
  @GetMapping
  @PreAuthorize(BookingAccess.VIEW)
  public PageResponse<ServiceInvoiceResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) SiKind kind,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), BookingAccess.MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "id"));
    return PageResponse.of(
        register.search(companyId, q, kind, pageable), ServiceInvoiceResponse::from);
  }

  /**
   * One service invoice.
   *
   * @param id service invoice
   * @return service invoice
   */
  @GetMapping("/{id}")
  @PreAuthorize(BookingAccess.VIEW)
  public ServiceInvoiceResponse get(@PathVariable Long id) {
    return ServiceInvoiceResponse.from(serviceInvoices.get(id));
  }

  /**
   * Service invoices and credits of a booked invoice.
   *
   * @param invoiceNo booked invoice
   * @return service invoices
   */
  @GetMapping("/by-invoice/{invoiceNo}")
  @PreAuthorize(BookingAccess.VIEW)
  public List<ServiceInvoiceResponse> forInvoice(@PathVariable String invoiceNo) {
    return register.forInvoice(invoiceNo).stream().map(ServiceInvoiceResponse::from).toList();
  }

  /**
   * The PDF as issued.
   *
   * @param id service invoice
   * @return PDF
   */
  @GetMapping("/{id}/pdf")
  @PreAuthorize(BookingAccess.VIEW)
  public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
    ServiceInvoice si = serviceInvoices.get(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(si.getSiNo() + ".pdf"))
        .body(register.document(id));
  }

  /**
   * Sends a service invoice again.
   *
   * @param id service invoice
   * @return service invoice
   */
  @PostMapping("/{id}/resend")
  @PreAuthorize(BookingAccess.VIEW)
  public ServiceInvoiceResponse resend(@PathVariable Long id) {
    return ServiceInvoiceResponse.from(serviceInvoices.resend(id));
  }

  /**
   * Issues a service invoice by hand.
   *
   * @param request type, recipient and lines
   * @return service invoice
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(BookingAccess.PROCESS)
  public ServiceInvoiceResponse issue(@Valid @RequestBody ServiceInvoiceIssueRequest request) {
    return ServiceInvoiceResponse.from(serviceInvoices.issueManual(request.toRequest()));
  }

  /**
   * Credits a service invoice.
   *
   * @param id service invoice
   * @param request amounts and reason
   * @return the credit
   */
  @PostMapping("/{id}/credit")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(BookingAccess.ADJUST)
  public ServiceInvoiceResponse credit(
      @PathVariable Long id, @Valid @RequestBody ServiceInvoiceCreditRequest request) {
    return ServiceInvoiceResponse.from(
        serviceInvoices.credit(serviceInvoices.get(id).getSiNo(), request.toRequest()));
  }
}
