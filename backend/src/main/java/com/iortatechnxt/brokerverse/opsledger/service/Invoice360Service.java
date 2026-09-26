package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceAdjustmentTotal;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceStatusChange;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems.RelatedItem;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems.Section;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The invoice 360 view (RMTID.026, RMTID.032/038, ADJID.024): header, components with their
 * balances, insurer shares, movements, status history, cumulative adjustments, the booking
 * references and the records of every Operations module ({@link InvoiceRelatedItems}).
 */
@Service
@Transactional(readOnly = true)
public class Invoice360Service {

  private final InvoiceLedgerQueryService ledger;
  private final BookingQueryService bookings;
  private final List<InvoiceRelatedItems> related;

  /**
   * Creates the service.
   *
   * @param ledger ledger reads
   * @param bookings booked invoices
   * @param related the modules' related records
   */
  public Invoice360Service(
      InvoiceLedgerQueryService ledger,
      BookingQueryService bookings,
      List<InvoiceRelatedItems> related) {
    this.ledger = ledger;
    this.bookings = bookings;
    this.related = related;
  }

  /**
   * The 360 view of an invoice.
   *
   * @param invoiceNo invoice number
   * @return view
   */
  public Invoice360 view(String invoiceNo) {
    OpsInvoice invoice = ledger.require(invoiceNo);
    BookedInvoice booked = bookings.byNo(invoiceNo);
    String original =
        invoice.getParentInvoiceNo() == null ? invoiceNo : invoice.getParentInvoiceNo();
    return new Invoice360(
        invoice,
        new BookingRefs(booked.getId(), booked.getServiceInvoiceNo(), booked.getJournalBatches()),
        ledger.movements(invoiceNo),
        ledger.history(invoiceNo),
        ledger.adjustmentTotal(original).orElse(null),
        relatedItems(invoiceNo));
  }

  private Map<Section, List<RelatedItem>> relatedItems(String invoiceNo) {
    Map<Section, List<RelatedItem>> items = new EnumMap<>(Section.class);
    for (InvoiceRelatedItems source : related) {
      items
          .computeIfAbsent(source.section(), s -> new ArrayList<>())
          .addAll(source.itemsFor(invoiceNo));
    }
    return items;
  }

  /**
   * Booking references of an invoice.
   *
   * @param bookedInvoiceId booked invoice id (booking screens)
   * @param serviceInvoiceNo commission service invoice
   * @param journalBatches GL journals of the booking
   */
  public record BookingRefs(
      Long bookedInvoiceId, String serviceInvoiceNo, List<String> journalBatches) {

    /** Defensive copy. */
    public BookingRefs {
      journalBatches = List.copyOf(journalBatches);
    }
  }

  /**
   * The 360 view.
   *
   * @param invoice invoice with components and shares
   * @param booking booking references
   * @param movements movements in posting order
   * @param history status, flag and lock history
   * @param adjustments cumulative adjustments of the original invoice, null when none
   * @param related records of the Operations modules by section
   */
  public record Invoice360(
      OpsInvoice invoice,
      BookingRefs booking,
      List<OpsInvoiceMovement> movements,
      List<OpsInvoiceStatusChange> history,
      OpsInvoiceAdjustmentTotal adjustments,
      Map<Section, List<RelatedItem>> related) {}
}
