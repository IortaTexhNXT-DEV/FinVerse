package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.worklist.domain.Assignment;
import com.iortatechnxt.brokerverse.collections.worklist.service.EditLockService.LockState;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The collection account page (COLLECTIONS_DESIGN 11): the item with the live net PR breakdown of
 * the ledger (BRCLXN.046), the payments (BRCLXN.054), the policy, invoice family and co-insurance
 * shares (BRCLXN.056) and the ledger part of the timeline (BRCLXN.057). The ledger stays the only
 * source of the money: nothing is copied here.
 */
@Service
@Transactional(readOnly = true)
public class AccountViewService {

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
  private static final Set<MovementType> PAYMENTS =
      Set.of(MovementType.APPLIED, MovementType.UNAPPLIED);

  private final CollectionItems items;
  private final InvoiceLedgerQueryService ledger;
  private final EditLockService locks;
  private final AssignmentService assignments;

  /**
   * Creates the service.
   *
   * @param items collection items
   * @param ledger invoice ledger
   * @param locks edit lock
   * @param assignments assignment history
   */
  public AccountViewService(
      CollectionItems items,
      InvoiceLedgerQueryService ledger,
      EditLockService locks,
      AssignmentService assignments) {
    this.items = items;
    this.ledger = ledger;
    this.locks = locks;
    this.assignments = assignments;
  }

  /**
   * The account header and live breakdown.
   *
   * @param invoiceNo invoice
   * @return item, ledger invoice (loaded) and edit lock
   */
  public Account account(String invoiceNo) {
    CollectionItem item = items.require(invoiceNo);
    Optional<OpsInvoice> invoice = ledger.find(invoiceNo);
    invoice.ifPresent(OpsInvoice::loadCollections);
    return new Account(item, invoice.orElse(null), locks.stateOf(item));
  }

  /**
   * The payments applied to the premium and their reversals, one line per transaction (BRCLXN.054),
   * with the summary booked / paid / outstanding.
   *
   * @param invoiceNo invoice
   * @return payments
   */
  public Payments payments(String invoiceNo) {
    items.require(invoiceNo);
    OpsInvoice invoice = ledger.require(invoiceNo);
    invoice.loadCollections();
    Map<String, PaymentLine> lines = new LinkedHashMap<>();
    for (OpsInvoiceMovement m : ledger.movements(invoiceNo)) {
      if (PAYMENTS.contains(m.getMovementType()) && premium(m.getComponent())) {
        String key = m.getMovementType() + "|" + m.getSourceModule() + "|" + m.getSourceRef();
        lines.merge(key, PaymentLine.of(m), PaymentLine::plus);
      }
    }
    BigDecimal booked = BigDecimal.ZERO;
    BigDecimal paid = BigDecimal.ZERO;
    for (var c : invoice.getComponents()) {
      if (premium(c.getComponent())) {
        booked = booked.add(c.due());
        paid = paid.add(c.netApplied());
      }
    }
    BigDecimal outstanding = invoice.premiumBalance().add(pr2307(invoice));
    return new Payments(booked, paid, outstanding, List.copyOf(lines.values()));
  }

  /**
   * The policy, the invoice family and the co-insurance shares (BRCLXN.056).
   *
   * @param invoiceNo invoice
   * @return the ledger invoice, its family and the first receipt date
   */
  public PolicyView policy(String invoiceNo) {
    items.require(invoiceNo);
    OpsInvoice invoice = ledger.require(invoiceNo);
    invoice.loadCollections();
    LocalDate firstReceipt =
        ledger.movements(invoiceNo).stream()
            .filter(m -> m.getMovementType() == MovementType.APPLIED && m.getArNo() != null)
            .map(PaymentLine::dateOf)
            .min(Comparator.naturalOrder())
            .orElse(null);
    return new PolicyView(invoice, ledger.family(invoiceNo), firstReceipt);
  }

  /**
   * The ledger movements and the assignments of an account as timeline entries (BRCLXN.057); the
   * dispositions, efforts and hand-offs are added by the caller.
   *
   * @param item item
   * @return entries, unsorted
   */
  public List<TimelineEntry> ledgerTimeline(CollectionItem item) {
    List<TimelineEntry> out = new ArrayList<>();
    Map<String, TimelineEntry> grouped = new LinkedHashMap<>();
    for (OpsInvoiceMovement m : ledger.movements(item.getInvoiceNo())) {
      String key = m.getMovementType() + "|" + m.getSourceModule() + "|" + m.getSourceRef();
      grouped.merge(
          key,
          new TimelineEntry(
              m.getPostedAt(),
              "LEDGER",
              label(m.getMovementType()),
              m.getSourceModule() + " " + m.getSourceRef(),
              m.getPostedBy(),
              premium(m.getComponent()) ? m.getAmount() : BigDecimal.ZERO),
          TimelineEntry::plus);
    }
    out.addAll(grouped.values());
    for (Assignment a : assignments.history(item.getId())) {
      out.add(
          new TimelineEntry(
              a.getCreatedAt(),
              "ASSIGNMENT",
              a.getKind() + " assignment to " + a.getHandlerUsername(),
              a.getReason(),
              a.getAssignedBy(),
              null));
    }
    return out;
  }

  private static String label(MovementType type) {
    String name = type.name().replace("_", " ").toLowerCase(Locale.ROOT);
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  private static boolean premium(LedgerComponent c) {
    return c.isPremiumReceivable() || c == LedgerComponent.PR2307;
  }

  private static BigDecimal pr2307(OpsInvoice invoice) {
    return invoice.getComponents().stream()
        .filter(c -> c.getComponent() == LedgerComponent.PR2307)
        .map(OpsInvoiceComponent::getBalance)
        .findFirst()
        .orElse(BigDecimal.ZERO);
  }

  /**
   * The account header.
   *
   * @param item item
   * @param invoice ledger invoice, null when it left the ledger
   * @param lock edit lock
   */
  public record Account(CollectionItem item, OpsInvoice invoice, LockState lock) {}

  /**
   * Payments of an account.
   *
   * @param booked premium due (booked and adjusted)
   * @param paid applied net of reversals
   * @param outstanding premium and PR2307 still open
   * @param lines one line per transaction
   */
  public record Payments(
      BigDecimal booked, BigDecimal paid, BigDecimal outstanding, List<PaymentLine> lines) {

    /** Defensive copy. */
    public Payments {
      lines = List.copyOf(lines);
    }
  }

  /**
   * A payment transaction on the premium.
   *
   * @param date value date (posting date when missing), Philippine time
   * @param type APPLIED or UNAPPLIED (reversal)
   * @param sourceModule module (CASHIERING ...)
   * @param reference transaction reference
   * @param arNo acknowledgement receipt
   * @param orNo official receipt
   * @param amount amount on the premium components
   */
  public record PaymentLine(
      LocalDate date,
      MovementType type,
      String sourceModule,
      String reference,
      String arNo,
      String orNo,
      BigDecimal amount) {

    static PaymentLine of(OpsInvoiceMovement m) {
      return new PaymentLine(
          dateOf(m),
          m.getMovementType(),
          m.getSourceModule(),
          m.getSourceRef(),
          m.getArNo(),
          m.getOrNo(),
          m.getAmount());
    }

    static LocalDate dateOf(OpsInvoiceMovement m) {
      return m.getValueDate() != null
          ? m.getValueDate()
          : m.getPostedAt().atZone(MANILA).toLocalDate();
    }

    PaymentLine plus(PaymentLine other) {
      return new PaymentLine(
          date,
          type,
          sourceModule,
          reference,
          arNo == null ? other.arNo() : arNo,
          orNo == null ? other.orNo() : orNo,
          amount.add(other.amount()));
    }
  }

  /**
   * The policy view.
   *
   * @param invoice ledger invoice with its shares
   * @param family invoices sharing its root invoice
   * @param firstReceiptDate date of the first AR applied (receipt date, CQ17)
   */
  public record PolicyView(
      OpsInvoice invoice, List<OpsInvoice> family, LocalDate firstReceiptDate) {

    /** Defensive copy. */
    public PolicyView {
      family = List.copyOf(family);
    }
  }

  /**
   * An entry of the account timeline (BRCLXN.057).
   *
   * @param at when
   * @param kind LEDGER, ASSIGNMENT, DISPOSITION, EFFORT, HANDOFF, INBOX or CHANGE
   * @param title what happened
   * @param detail details
   * @param by user
   * @param amount amount on the premium, null when none
   */
  public record TimelineEntry(
      Instant at, String kind, String title, String detail, String by, BigDecimal amount) {

    TimelineEntry plus(TimelineEntry other) {
      BigDecimal sum = amount == null ? other.amount() : amount.add(other.amount());
      return new TimelineEntry(at, kind, title, detail, by, sum);
    }
  }
}
