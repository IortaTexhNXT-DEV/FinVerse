package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.booking.service.InvoiceSearch;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the booked invoice search.
 *
 * @param q invoice number, ARN, policy number or client
 * @param status status (default BOOKED)
 * @param kind kind
 * @param insurer lead insurer
 * @param from booked on or after
 * @param to booked on or before
 * @param line product line
 */
public record InvoiceSearchParams(
    String q,
    InvoiceStatus status,
    InvoiceKind kind,
    String insurer,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    String line) {

  /**
   * The search criteria of a company.
   *
   * @param companyId company
   * @return criteria
   */
  public InvoiceSearch toSearch(Long companyId) {
    return new InvoiceSearch(companyId, q, status, kind, insurer, from, to, line);
  }
}
