package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationPlanner.InvoiceBalances;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationPlanner.Plan;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPosting.PostingContext;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerContext;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies money to a booked invoice by premium component (CSHID.020/022) and reverses applications
 * (CSHID.012, ADJID.009/012/013). Each application posts {@code OPS_PAYMENT_APPLY} (with the
 * commission realized pro rata when {@code OPS_COMMISSION_REALIZATION = ON_COLLECTION}) and records
 * an {@code APPLIED} movement on the invoice ledger in the same transaction; a reversal re-posts it
 * negative and records an {@code UNAPPLIED} movement.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ApplicationService {

  private static final String ENTITY = "CashApplication";
  private static final int RATIO_SCALE = 10;

  private final ApplicationRepository applications;
  private final CashReceiptRepository receipts;
  private final InvoiceLedgerService ledger;
  private final CashieringPosting posting;
  private final CashieringSettings settings;
  private final AuditTrailService audit;
  private final Clock clock;
  private final UnappliedRepository unappliedItems;

  /**
   * Creates the service.
   *
   * @param applications applications
   * @param receipts receipts
   * @param ledger invoice ledger
   * @param posting accounting events
   * @param settings parameters
   * @param audit audit trail
   * @param clock clock
   * @param unappliedItems unapplied items (ledger context of a legacy unapplied payment)
   */
  @SuppressWarnings("java:S107") // collaborators
  public ApplicationService(
      ApplicationRepository applications,
      CashReceiptRepository receipts,
      InvoiceLedgerService ledger,
      CashieringPosting posting,
      CashieringSettings settings,
      AuditTrailService audit,
      Clock clock,
      UnappliedRepository unappliedItems) {
    this.unappliedItems = unappliedItems;
    this.applications = applications;
    this.receipts = receipts;
    this.ledger = ledger;
    this.posting = posting;
    this.settings = settings;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * What an invoice still expects, with the 2% a CWT client withholds (CSHID.020).
   *
   * @param invoice invoice (components loaded)
   * @return balances
   */
  public InvoiceBalances balancesOf(OpsInvoice invoice) {
    BigDecimal withheld = BigDecimal.ZERO;
    if (invoice.isCwtFlag()) {
      withheld =
          ApplicationPlanner.withheld(
              premiumDue(invoice),
              invoice.component(LedgerComponent.PR2307).getAdjusted().max(BigDecimal.ZERO),
              settings.cwtApplicationPercent());
    }
    return new InvoiceBalances(invoice.balances(), withheld);
  }

  /**
   * Whether an invoice takes client payments at all.
   *
   * @param invoice invoice
   * @return false for direct payment, return and cancelled invoices
   */
  public static boolean receivable(OpsInvoice invoice) {
    return invoice.getPaymentStatus() != PaymentStatus.NOT_APPLICABLE && !invoice.isCancelled();
  }

  /**
   * Applies money to an invoice.
   *
   * @param invoice invoice (components loaded)
   * @param amount money available
   * @param origin receipt or unapplied item and source
   * @param options value date, DST only, AR number
   * @return the application, empty when nothing could be applied
   */
  public Optional<Application> apply(
      OpsInvoice invoice, BigDecimal amount, Application.Origin origin, ApplyOptions options) {
    if (!receivable(invoice) || amount.signum() <= 0) {
      return Optional.empty();
    }
    InvoiceBalances balances =
        options.fullPremium()
            ? new InvoiceBalances(invoice.balances(), BigDecimal.ZERO)
            : balancesOf(invoice);
    Plan plan = ApplicationPlanner.plan(balances, amount, options.dstOnly());
    if (plan.applied().signum() <= 0) {
      return Optional.empty();
    }
    Application app =
        applications.save(
            new Application(
                new Application.Target(
                    invoice.getCompanyId(),
                    invoice.getInvoiceNo(),
                    invoice.getArn(),
                    invoice.getClientCode()),
                origin,
                plan.allocation(),
                options.valueDate()));
    String batch = postApplication(invoice, app, plan, options.valueDate());
    ledger.post(
        new MovementRequest(
            invoice.getInvoiceNo(),
            MovementType.APPLIED,
            CashieringSettings.MODULE,
            app.reference(),
            options.valueDate(),
            plan.allocation(),
            new DocumentRefs(options.arNo(), null, null, batch),
            origin.source() + " " + nz(origin.sourceRef())));
    receiptApplied(app.getReceiptId(), plan.applied());
    audit.record(
        ENTITY,
        app.reference(),
        AuditAction.CREATE,
        "Applied " + plan.applied() + " to " + invoice.getInvoiceNo() + " " + plan.allocation());
    return Optional.of(app);
  }

  private String postApplication(
      OpsInvoice invoice, Application app, Plan plan, LocalDate valueDate) {
    BigDecimal ratio = ratio(plan.applied(), invoice);
    boolean realize = settings.realizeOnCollection();
    BigDecimal commission =
        realize ? Money.round(invoice.getCommission().multiply(ratio)) : BigDecimal.ZERO;
    BigDecimal vat =
        realize ? Money.round(invoice.getVatOnCommission().multiply(ratio)) : BigDecimal.ZERO;
    Map<String, BigDecimal> amounts =
        amounts(invoice, app, plan.applied(), plan.allocation(), new Realized(commission, vat));
    String batch =
        posting.publish(
            context(invoice, valueDate, "Payment application " + app.reference()),
            CashieringPosting.PAYMENT_APPLY,
            app.reference(),
            amounts);
    app.posted(commission, vat, batch);
    return batch;
  }

  /**
   * The amounts of {@code OPS_PAYMENT_APPLY} (DATA_MIGRATION_DESIGN 14.4 B): the unapplied side in
   * the context of the unapplied item (LG_APPLIED for a legacy unapplied payment), the premium
   * receivable and the realised commission in the context of the invoice (LG_PR_*, LG_REALIZED_*
   * for a legacy invoice).
   */
  private Map<String, BigDecimal> amounts(
      OpsInvoice invoice,
      Application app,
      BigDecimal applied,
      Map<LedgerComponent, BigDecimal> allocation,
      Realized realized) {
    LedgerContext invoiceContext = invoice.getLegacy().ledgerContext();
    LedgerContext uppContext =
        app.getUnappliedId() == null
            ? LedgerContext.NEW
            : unappliedItems
                .findById(app.getUnappliedId())
                .map(u -> u.getLegacy().ledgerContext())
                .orElse(LedgerContext.NEW);
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put(uppContext.component("APPLIED"), applied);
    CashieringPosting.prAmounts(allocation, false)
        .forEach((k, v) -> amounts.merge(invoiceContext.component(k), v, BigDecimal::add));
    amounts.put(invoiceContext.component("REALIZED_COMMISSION"), realized.commission());
    amounts.put(invoiceContext.component("REALIZED_VAT"), realized.vat());
    return amounts;
  }

  /**
   * Commission and VAT realised on collection.
   *
   * @param commission commission
   * @param vat VAT on commission
   */
  private record Realized(BigDecimal commission, BigDecimal vat) {}

  /**
   * Reverses an application: negative {@code OPS_PAYMENT_APPLY}, {@code UNAPPLIED} movement.
   *
   * @param app active application
   * @param invoice its invoice
   * @param reversalRef source reference of the reversal (e.g. {@code APP:12:CANCEL:CAN-2026-1})
   * @param reason reason
   */
  public void reverse(Application app, OpsInvoice invoice, String reversalRef, String reason) {
    reverse(app, invoice, new Reversal(reversalRef, reason, false));
  }

  /**
   * Reverses an application; a re-application inside another module's posting records the undo as a
   * negative {@code APPLIED} movement, which the ledger accepts on an invoice that module locked.
   *
   * @param app active application
   * @param invoice its invoice
   * @param reversal source reference, reason and movement kind
   */
  public void reverse(Application app, OpsInvoice invoice, Reversal reversal) {
    String reversalRef = reversal.ref();
    String reason = reversal.reason();
    LocalDate today = BusinessClock.today(clock);
    Map<LedgerComponent, BigDecimal> allocation = app.allocation();
    Map<LedgerComponent, BigDecimal> negated = new EnumMap<>(LedgerComponent.class);
    allocation.forEach((c, v) -> negated.put(c, v.negate()));
    Map<String, BigDecimal> amounts =
        amounts(
            invoice,
            app,
            app.getAmount().negate(),
            negated,
            new Realized(app.getRealizedCommission().negate(), app.getRealizedVat().negate()));
    String batch =
        posting.publish(
            context(
                invoice,
                today,
                "Reversal of the payment applied to " + app.getInvoiceNo() + ": " + reason),
            CashieringPosting.PAYMENT_APPLY,
            reversalRef,
            amounts);
    Map<LedgerComponent, BigDecimal> undo = new EnumMap<>(LedgerComponent.class);
    allocation.forEach((c, v) -> undo.put(c, reversal.negativeApplied() ? v.negate() : v));
    ledger.post(
        new MovementRequest(
            app.getInvoiceNo(),
            reversal.negativeApplied() ? MovementType.APPLIED : MovementType.UNAPPLIED,
            CashieringSettings.MODULE,
            reversalRef,
            today,
            undo,
            new DocumentRefs(null, null, null, batch),
            reason));
    app.reverse(reversalRef, reason, clock.instant(), batch);
    receiptApplied(app.getReceiptId(), app.getAmount().negate());
    audit.record(
        ENTITY, app.reference(), AuditAction.UPDATE, "Reversed (" + reversalRef + "): " + reason);
  }

  private void receiptApplied(Long receiptId, BigDecimal delta) {
    if (receiptId != null) {
      receipts.findById(receiptId).ifPresent(r -> r.applied(delta));
    }
  }

  /**
   * The posting facts of an invoice.
   *
   * @param invoice invoice
   * @param date value date
   * @param narration narration
   * @return context with the client as party
   */
  static PostingContext context(OpsInvoice invoice, LocalDate date, String narration) {
    return new PostingContext(
        invoice.getCompanyId(),
        invoice.getBranchId(),
        date,
        invoice.getCurrency(),
        invoice.getInvoiceNo(),
        invoice.getClientCode(),
        invoice.getClassification().productLine(),
        invoice.getClassification().costCenter(),
        narration,
        null);
  }

  /**
   * Premium receivable due of an invoice (booked and adjusted).
   *
   * @param invoice invoice
   * @return premium due
   */
  static BigDecimal premiumDue(OpsInvoice invoice) {
    return invoice.getComponents().stream()
        .filter(c -> c.getComponent().isPremiumReceivable())
        .map(OpsInvoiceComponent::due)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal ratio(BigDecimal applied, OpsInvoice invoice) {
    BigDecimal base =
        premiumDue(invoice)
            .add(invoice.component(LedgerComponent.PR2307).getAdjusted().max(BigDecimal.ZERO));
    if (base.signum() <= 0) {
      return BigDecimal.ZERO;
    }
    return applied.divide(base, RATIO_SCALE, RoundingMode.HALF_UP).min(BigDecimal.ONE);
  }

  private static String nz(String value) {
    return value == null ? "" : value;
  }

  /**
   * How an application is reversed.
   *
   * @param ref source reference of the reversal
   * @param reason reason
   * @param negativeApplied record a negative APPLIED movement instead of UNAPPLIED
   */
  public record Reversal(String ref, String reason, boolean negativeApplied) {}

  /**
   * Options of an application.
   *
   * @param valueDate value date
   * @param dstOnly apply to the DST only
   * @param arNo AR number shown on the ledger movement, may be null
   * @param fullPremium apply up to 100% even for a 2% CWT account (2307 cash path, CSHID.026)
   */
  public record ApplyOptions(
      LocalDate valueDate, boolean dstOnly, String arNo, boolean fullPremium) {

    /**
     * Options of an ordinary application.
     *
     * @param valueDate value date
     * @param dstOnly apply to the DST only
     * @param arNo AR number, may be null
     */
    public ApplyOptions(LocalDate valueDate, boolean dstOnly, String arNo) {
      this(valueDate, dstOnly, arNo, false);
    }
  }
}
