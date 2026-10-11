package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceAdjustmentTotal;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshot;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshotRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceStatusChange;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems.RelatedItem;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceRelatedItems.Section;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  /** Work item type of an account (NB_ACCOUNT workflow). */
  private static final String ACCOUNT = "Account";

  private final InvoiceLedgerQueryService ledger;
  private final BookingQueryService bookings;
  private final List<InvoiceRelatedItems> related;
  private final OpsInvoiceOriginSnapshotRepository snapshots;
  private final WorkCaseRepository workCases;

  /**
   * Creates the service.
   *
   * @param ledger ledger reads
   * @param bookings booked invoices
   * @param related the modules' related records
   * @param snapshots origin snapshots of legacy invoices
   * @param workCases work items (the account's workflow panel)
   */
  public Invoice360Service(
      InvoiceLedgerQueryService ledger,
      BookingQueryService bookings,
      List<InvoiceRelatedItems> related,
      OpsInvoiceOriginSnapshotRepository snapshots,
      WorkCaseRepository workCases) {
    this.ledger = ledger;
    this.bookings = bookings;
    this.related = related;
    this.snapshots = snapshots;
    this.workCases = workCases;
  }

  /**
   * The 360 view of an invoice. A legacy invoice has no booking in BIBS: its booking references are
   * empty and the view carries the legacy snapshot instead (DATA_MIGRATION_DESIGN 14.1). Its
   * account, imported from legacy, has no account workflow, so the view says whether the account
   * has a work item and the screen shows the workflow panel only then.
   *
   * @param invoiceNo invoice number
   * @return view
   */
  public Invoice360 view(String invoiceNo) {
    OpsInvoice invoice = ledger.require(invoiceNo);
    OpsInvoiceOriginSnapshot snapshot = null;
    BookingRefs refs;
    if (invoice.getRecordOrigin().isMigrated()) {
      snapshot = snapshots.findByInvoiceId(invoice.getId()).orElse(null);
      Optional.ofNullable(snapshot).ifPresent(OpsInvoiceOriginSnapshot::loadLines);
      refs = new BookingRefs(null, null, List.of());
    } else {
      BookedInvoice booked = bookings.byNo(invoiceNo);
      refs =
          new BookingRefs(booked.getId(), booked.getServiceInvoiceNo(), booked.getJournalBatches());
    }
    String original =
        invoice.getParentInvoiceNo() == null ? invoiceNo : invoice.getParentInvoiceNo();
    return new Invoice360(
        invoice,
        refs,
        ledger.movements(invoiceNo),
        ledger.history(invoiceNo),
        ledger.adjustmentTotal(original).orElse(null),
        relatedItems(invoiceNo),
        snapshot,
        hasAccountWorkflow(invoice.getAccountId()));
  }

  private boolean hasAccountWorkflow(Long accountId) {
    return accountId != null
        && workCases.findByEntityTypeAndEntityId(ACCOUNT, accountId.toString()).isPresent();
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
   * @param origin frozen original values of a legacy invoice, null for BIBS invoices
   * @param accountWorkflow whether the invoice's account has a work item (none for an account
   *     imported from legacy)
   */
  public record Invoice360(
      OpsInvoice invoice,
      BookingRefs booking,
      List<OpsInvoiceMovement> movements,
      List<OpsInvoiceStatusChange> history,
      OpsInvoiceAdjustmentTotal adjustments,
      Map<Section, List<RelatedItem>> related,
      OpsInvoiceOriginSnapshot origin,
      boolean accountWorkflow) {}
}
