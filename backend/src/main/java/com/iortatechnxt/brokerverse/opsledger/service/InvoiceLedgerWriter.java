package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponent;
import com.iortatechnxt.brokerverse.booking.service.BookingEvents;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.opsledger.domain.FeedSource;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceAdjustmentTotal;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceAdjustmentTotalRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceData;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.OpsInvoiceBooked;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Copies a booked invoice into the Operations ledger (OPERATIONS_DESIGN 4.1, feed from booking):
 * header facts, insurer shares with the lead flagged (ADJID.027), one BOOKED movement per non-zero
 * component (PR by component, DTIP, commission, VAT on commission, withholding tax), cumulative
 * adjustments of the original for endorsements and cancellations (ADJID.028). Idempotent on the
 * invoice number: an invoice already in the ledger is left as it is.
 *
 * <p>Direct payment invoices (BRNB.114, MKTID.011) carry the invoiced premium on their PR and DTIP
 * components although booking posts no premium receivable for them; their payment and remittance
 * status is NOT_APPLICABLE and commission closes them with a DP_REVERSAL movement (MKTID.012).
 */
@Service
@Transactional
public class InvoiceLedgerWriter {

  private static final String ENTITY = "OpsInvoice";
  private static final String OVER_ADJUSTED = "ADJ_OVER_BASELINE";

  private final OpsInvoiceRepository invoices;
  private final OpsInvoiceAdjustmentTotalRepository totals;
  private final BookingQueryService bookings;
  private final InvoiceLedgerService ledger;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final ApplicationEventPublisher events;

  /**
   * Creates the writer.
   *
   * @param invoices ledger invoices
   * @param totals cumulative adjustments
   * @param bookings booked invoices (contract of booking)
   * @param ledger movements
   * @param alerts alerts (over-adjustment)
   * @param audit audit trail
   * @param events event publisher
   */
  public InvoiceLedgerWriter(
      OpsInvoiceRepository invoices,
      OpsInvoiceAdjustmentTotalRepository totals,
      BookingQueryService bookings,
      InvoiceLedgerService ledger,
      AlertService alerts,
      AuditTrailService audit,
      ApplicationEventPublisher events) {
    this.invoices = invoices;
    this.totals = totals;
    this.bookings = bookings;
    this.ledger = ledger;
    this.alerts = alerts;
    this.audit = audit;
    this.events = events;
  }

  /**
   * Copies a booked invoice.
   *
   * @param event the invoice as booking published it
   * @param source event or replay
   * @return the new ledger invoice, empty when it was already in the ledger
   */
  public Optional<OpsInvoice> record(InvoiceBooked event, FeedSource source) {
    if (invoices.existsByInvoiceNo(event.invoiceNo())) {
      return Optional.empty();
    }
    BookedInvoice booked = bookings.byNo(event.invoiceNo());
    OpsInvoice invoice =
        invoices.save(OpsInvoice.of(data(event, booked), shares(event, booked), source));
    ledger.record(
        invoice,
        new MovementRequest(
            invoice.getInvoiceNo(),
            MovementType.BOOKED,
            BookingEvents.MODULE,
            invoice.getInvoiceNo(),
            event.bookingDate(),
            bookedAmounts(event, booked),
            new DocumentRefs(null, null, null, firstJournal(booked)),
            "Booked " + event.kind()));
    if (booked.getParentInvoiceNo() != null) {
      addToOriginal(booked.getParentInvoiceNo(), invoice);
    }
    audit.record(
        ENTITY,
        invoice.getInvoiceNo(),
        AuditAction.CREATE,
        "Copied from booking (" + source + "): " + event.kind() + " " + event.arn());
    events.publishEvent(
        new OpsInvoiceBooked(
            invoice.getCompanyId(),
            invoice.getInvoiceNo(),
            invoice.getArn(),
            invoice.getKind(),
            invoice.getClientCode(),
            invoice.getInsurerCode(),
            invoice.getPolicyNo(),
            invoice.isDpFlag(),
            source));
    return Optional.of(invoice);
  }

  private static OpsInvoiceData data(InvoiceBooked e, BookedInvoice b) {
    return new OpsInvoiceData(
        new OpsInvoiceData.Keys(
            b.getCompanyId(),
            b.getBranchId(),
            e.invoiceNo(),
            e.arn(),
            b.getAccountId(),
            e.kind(),
            e.endorsementNo(),
            b.getParentInvoiceNo(),
            e.policyNo(),
            e.policyYear()),
        new OpsInvoiceData.Parties(
            e.clientCode(),
            b.getFacts().clientName(),
            b.getFacts().clientName(),
            b.getFacts().insurerCode()),
        new OpsInvoiceData.Classification(
            e.currency(),
            e.bookingDate(),
            e.inceptionDate(),
            e.expiryDate(),
            e.riskCode(),
            e.lineCode(),
            e.segment(),
            e.aoUsername(),
            e.salesUnit(),
            e.costCenter()),
        new OpsInvoiceData.Amounts(
            e.grossPremium(), e.commission(), e.vatOnCommission(), e.wtaxRate()),
        new OpsInvoiceData.Flags(e.directPayment(), e.cwt2Percent(), e.incentiveEligible()));
  }

  private static List<OpsInvoiceShare> shares(InvoiceBooked e, BookedInvoice b) {
    String lead = b.getFacts().insurerCode();
    boolean leadListed = e.shares().stream().anyMatch(s -> s.insurerCode().equals(lead));
    return e.shares().stream()
        .map(
            s ->
                new OpsInvoiceShare(
                    s.insurerCode(),
                    s.sharePct(),
                    leadListed ? s.insurerCode().equals(lead) : s.equals(e.shares().get(0))))
        .toList();
  }

  private static Map<LedgerComponent, BigDecimal> bookedAmounts(InvoiceBooked e, BookedInvoice b) {
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    for (Map.Entry<PremiumComponent, BigDecimal> c : e.components().entrySet()) {
      amounts.put(LedgerComponent.of(c.getKey()), c.getValue());
    }
    amounts.put(LedgerComponent.DTIP, e.grossPremium());
    amounts.put(LedgerComponent.COMMISSION, e.commission());
    amounts.put(LedgerComponent.COMMISSION_VAT, e.vatOnCommission());
    amounts.put(LedgerComponent.WTAX, b.getCommission().wtaxAmount());
    return amounts;
  }

  private static String firstJournal(BookedInvoice b) {
    return b.getJournalBatches().isEmpty() ? null : b.getJournalBatches().get(0);
  }

  private void addToOriginal(String originalNo, OpsInvoice adjustment) {
    Optional<OpsInvoice> original = invoices.findByInvoiceNo(originalNo);
    if (original.isEmpty()) {
      return;
    }
    OpsInvoiceAdjustmentTotal total =
        totals
            .findByOriginalInvoiceNo(originalNo)
            .orElseGet(() -> new OpsInvoiceAdjustmentTotal(original.get()));
    total.add(adjustment);
    totals.save(total);
    if (total.isOverAdjusted()) {
      alerts.raise(
          OVER_ADJUSTED,
          new AlertFacts(
              adjustment.getCompanyId(),
              adjustment.getBranchId(),
              ENTITY,
              originalNo,
              "Adjustments of " + originalNo + " exceed its original premium or DTIP",
              total.getAdjustedPremium(),
              OVER_ADJUSTED + ":" + originalNo));
    }
  }
}
