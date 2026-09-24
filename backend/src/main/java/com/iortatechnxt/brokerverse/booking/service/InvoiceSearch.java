package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import java.time.LocalDate;

/**
 * Booked invoice search criteria; null criteria are not applied.
 *
 * @param companyId company
 * @param text invoice number, ARN, client code or name, or policy number fragment
 * @param status status (default BOOKED)
 * @param kind booking, endorsement or cancellation
 * @param insurerCode lead insurer
 * @param from booked on or after
 * @param to booked on or before
 * @param lineCode product line
 */
public record InvoiceSearch(
    Long companyId,
    String text,
    InvoiceStatus status,
    InvoiceKind kind,
    String insurerCode,
    LocalDate from,
    LocalDate to,
    String lineCode) {}
