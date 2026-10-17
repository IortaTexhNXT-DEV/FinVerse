/**
 * Broker booking (BRD-1 New Business, BRD 2.4): booked invoices with the broker accounting entry,
 * open items and service invoice, individual / batch / direct / multi-year booking, auto-book
 * rules, endorsements and cancellations, incentive flag and cost center (BRNB.027, 036, 038, 061,
 * 076, 081, 094, 097, 100, 100b, 107, 108, 111, 112).
 *
 * <p>Contracts for other modules (Operations BRD-2 in particular): the domain event {@code
 * InvoiceBooked}, {@code BookingQueryService}, {@code BookingQueueService.enqueue}, {@code
 * EndorsementPostingService.post} and {@code ServiceInvoiceService.issue / credit}. See
 * docs/architecture/BROKING_ARCHITECTURE.md section 11 (booking).
 */
package com.iortatechnxt.brokerverse.booking;
