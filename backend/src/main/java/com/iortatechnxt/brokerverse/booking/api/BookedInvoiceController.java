package com.iortatechnxt.brokerverse.booking.api;

import com.iortatechnxt.brokerverse.booking.api.dto.EndorsementResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.InvoiceResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.InvoiceSearchParams;
import com.iortatechnxt.brokerverse.booking.api.dto.InvoiceSummaryResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.OpenItemLineResponse;
import com.iortatechnxt.brokerverse.booking.api.dto.PostedLineResponse;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Booked invoices (BRNB.027): search, detail with open items, the schedule and endorsements of an
 * account, and the published event of an invoice (replay for the Operations feed).
 */
@RestController
@RequestMapping("/api/v1/booking")
public class BookedInvoiceController {

  private final BookingQueryService queries;

  /**
   * Creates the controller.
   *
   * @param queries invoice reads
   */
  public BookedInvoiceController(BookingQueryService queries) {
    this.queries = queries;
  }

  /**
   * Searches booked invoices.
   *
   * @param companyId company
   * @param params text, status, kind, insurer and booking dates
   * @param page page
   * @param size size
   * @return invoices, newest first
   */
  @GetMapping("/invoices")
  @PreAuthorize(BookingAccess.VIEW)
  public PageResponse<InvoiceSummaryResponse> search(
      @RequestParam Long companyId,
      InvoiceSearchParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), BookingAccess.MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "id"));
    return PageResponse.of(
        queries.search(params.toSearch(companyId), pageable), InvoiceSummaryResponse::from);
  }

  /**
   * One invoice.
   *
   * @param id invoice
   * @return invoice
   */
  @GetMapping("/invoices/{id}")
  @PreAuthorize(BookingAccess.VIEW)
  public InvoiceResponse get(@PathVariable Long id) {
    return InvoiceResponse.from(queries.get(id));
  }

  /**
   * One invoice by number.
   *
   * @param invoiceNo invoice number
   * @return invoice
   */
  @GetMapping("/invoices/by-no/{invoiceNo}")
  @PreAuthorize(BookingAccess.VIEW)
  public InvoiceResponse byNo(@PathVariable String invoiceNo) {
    return InvoiceResponse.from(queries.byNo(invoiceNo));
  }

  /**
   * The published event of an invoice (Operations feed replay).
   *
   * @param invoiceNo invoice number
   * @return event data
   */
  @GetMapping("/invoices/by-no/{invoiceNo}/event")
  @PreAuthorize(BookingAccess.VIEW)
  public InvoiceBooked event(@PathVariable String invoiceNo) {
    return queries.invoice(invoiceNo);
  }

  /**
   * Journal lines posted for an invoice.
   *
   * @param id invoice
   * @return lines of every journal batch
   */
  @GetMapping("/invoices/{id}/journal")
  @PreAuthorize(BookingAccess.VIEW)
  public List<PostedLineResponse> journal(@PathVariable Long id) {
    return queries.journalLines(queries.get(id)).stream().map(PostedLineResponse::from).toList();
  }

  /**
   * Open items of an invoice.
   *
   * @param id invoice
   * @return items
   */
  @GetMapping("/invoices/{id}/open-items")
  @PreAuthorize(BookingAccess.VIEW)
  public List<OpenItemLineResponse> openItems(@PathVariable Long id) {
    return queries.openItems(queries.get(id)).stream().map(OpenItemLineResponse::from).toList();
  }

  /**
   * Every invoice of an account (booked, scheduled and cancelled policy years).
   *
   * @param arn account
   * @return invoices
   */
  @GetMapping("/accounts/{arn}/invoices")
  @PreAuthorize(BookingAccess.VIEW)
  public List<InvoiceResponse> schedule(@PathVariable String arn) {
    return queries.schedule(arn).stream().map(InvoiceResponse::from).toList();
  }

  /**
   * Endorsements of an account.
   *
   * @param arn account
   * @return endorsements
   */
  @GetMapping("/accounts/{arn}/endorsements")
  @PreAuthorize(BookingAccess.VIEW)
  public List<EndorsementResponse> endorsements(@PathVariable String arn) {
    return queries.endorsements(arn).stream().map(EndorsementResponse::from).toList();
  }
}
