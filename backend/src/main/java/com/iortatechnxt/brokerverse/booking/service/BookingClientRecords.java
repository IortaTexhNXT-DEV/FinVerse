package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Booked invoices of a client for the client 360 view (BRNB.099), through the crm port. */
@Component
public class BookingClientRecords implements ClientRecordsProvider {

  private final BookedInvoiceRepository invoices;

  /**
   * Creates the provider.
   *
   * @param invoices booked invoices
   */
  public BookingClientRecords(BookedInvoiceRepository invoices) {
    this.invoices = invoices;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return invoices.findByFactsClientIdOrderByIdDesc(clientId).stream()
        .filter(BookedInvoice::isBooked)
        .map(BookingClientRecords::record)
        .toList();
  }

  private static ClientRecord record(BookedInvoice i) {
    return new ClientRecord(
        "Booked invoice",
        i.getInvoiceNo(),
        i.getKind()
            + " - "
            + i.getArn()
            + " - "
            + i.getFacts().insurerCode()
            + " - gross "
            + i.getPremium().total().toPlainString(),
        i.getStatus().name(),
        i.getBookingDate(),
        "/booking/invoices/" + i.getId());
  }
}
