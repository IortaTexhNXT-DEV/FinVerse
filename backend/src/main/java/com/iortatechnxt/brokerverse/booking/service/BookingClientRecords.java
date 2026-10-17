package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.catalog.service.CatalogNames;
import com.iortatechnxt.brokerverse.crm.service.ClientRecord;
import com.iortatechnxt.brokerverse.crm.service.ClientRecordsProvider;
import com.iortatechnxt.brokerverse.crm.service.RecordDescriptions;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Booked invoices of a client for the client 360 view (BRNB.099), through the crm port. */
@Component
public class BookingClientRecords implements ClientRecordsProvider {

  private final BookedInvoiceRepository invoices;
  private final CatalogNames names;

  /**
   * Creates the provider.
   *
   * @param invoices booked invoices
   * @param names insurer names
   */
  public BookingClientRecords(BookedInvoiceRepository invoices, CatalogNames names) {
    this.invoices = invoices;
    this.names = names;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClientRecord> recordsOf(Long clientId) {
    return invoices.findByFactsClientIdOrderByIdDesc(clientId).stream()
        .filter(BookedInvoice::isBooked)
        .map(this::record)
        .toList();
  }

  private ClientRecord record(BookedInvoice i) {
    return new ClientRecord(
        "Booked invoice",
        i.getInvoiceNo(),
        RecordDescriptions.bookedInvoice(
            i.getKind(),
            i.getArn(),
            names.insurer(i.getFacts().insurerCode()),
            i.getCurrency(),
            i.getPremium().total()),
        i.getStatus().name(),
        i.getBookingDate(),
        "/booking/invoices/" + i.getId());
  }
}
