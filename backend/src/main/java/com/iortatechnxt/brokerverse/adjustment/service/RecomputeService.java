package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.ComponentChange;
import com.iortatechnxt.brokerverse.adjustment.domain.Computation;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.adjustment.service.PremiumDeltas.Delta;
import com.iortatechnxt.brokerverse.adjustment.service.Recompute.Baseline;
import com.iortatechnxt.brokerverse.adjustment.service.Recompute.ServiceInvoiceImpact;
import com.iortatechnxt.brokerverse.adjustment.service.Recompute.Settlement;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceAdjustmentTotal;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recompute of an endorsement request (ADJID.008/009/012-014/027/028): the premium and commission
 * changes of {@link PremiumDeltas}, laid out before / after per invoice component, with the
 * payments to re-apply, the AR Insurer of a remitted decrease, the service invoice impact, the
 * over-adjustment baseline and the "quotation required" check of a TSI increase.
 */
@Service
@Transactional(readOnly = true)
public class RecomputeService {

  /** Parameter of the over-adjustment baseline in percent (ADJID.028). */
  public static final String BASELINE_PARAMETER = "ADJ_BASELINE_PERCENT";

  private static final BigDecimal DEFAULT_BASELINE = BigDecimal.valueOf(100);
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final String ISSUE = "ISSUE";
  private static final String CREDIT = "CREDIT";
  private static final String NONE = "NONE";

  private final PremiumDeltas deltas;
  private final InvoiceLedgerQueryService ledger;
  private final AccountQueryService accounts;
  private final ProductCatalogService products;
  private final SystemParameterService parameters;

  /**
   * Creates the service.
   *
   * @param deltas premium and commission changes
   * @param ledger Operations ledger
   * @param accounts accounts (TSI, product)
   * @param products catalog (package TSI limit)
   * @param parameters business parameters (baseline)
   */
  public RecomputeService(
      PremiumDeltas deltas,
      InvoiceLedgerQueryService ledger,
      AccountQueryService accounts,
      ProductCatalogService products,
      SystemParameterService parameters) {
    this.deltas = deltas;
    this.ledger = ledger;
    this.accounts = accounts;
    this.products = products;
    this.parameters = parameters;
  }

  /**
   * Recomputes a request on its invoice.
   *
   * @param invoiceNo invoice
   * @param computation computation
   * @param terms terms
   * @param amounts amounts entered
   * @return recompute
   */
  public Recompute compute(
      String invoiceNo, Computation computation, RequestTerms terms, AmountInput amounts) {
    OpsInvoice invoice = ledger.require(invoiceNo);
    Delta delta = deltas.compute(invoice, computation, terms, amounts);
    Map<LedgerComponent, BigDecimal> changes = changes(computation, delta);
    List<ComponentChange> components = new ArrayList<>();
    changes.forEach(
        (component, change) ->
            components.add(
                new ComponentChange(component, invoice.component(component).due(), change)));
    boolean reduces = delta.premium().total().signum() < 0;
    return new Recompute(
        components,
        delta.shares(),
        delta.premium(),
        delta.commission(),
        settlement(invoice, computation, reduces, changes.get(LedgerComponent.DTIP)),
        impact(delta.commission().commission(), delta.commission().vatOnCommission()),
        baseline(invoice, delta.premium().total()),
        quotationRequired(invoice, computation, terms));
  }

  /**
   * The recompute recorded on a request when it was posted or cancelled (the account may no longer
   * be endorsable): before / after and insurer changes as stored.
   *
   * @param request request
   * @return recompute as recorded
   */
  public static Recompute recorded(EndorsementRequest request) {
    Map<LedgerComponent, BigDecimal> delta = new EnumMap<>(LedgerComponent.class);
    request.getChanges().forEach(c -> delta.put(c.component(), c.delta()));
    PremiumComponents premium =
        new PremiumComponents(
            delta.get(LedgerComponent.BASIC),
            delta.get(LedgerComponent.DST),
            delta.get(LedgerComponent.PREMIUM_TAX_VAT),
            delta.get(LedgerComponent.LGT),
            delta.get(LedgerComponent.FST),
            delta.get(LedgerComponent.OTHER));
    BigDecimal commission = delta.getOrDefault(LedgerComponent.COMMISSION, zero());
    BigDecimal vat = delta.getOrDefault(LedgerComponent.COMMISSION_VAT, zero());
    BigDecimal arInsurer = request.outcome().arInsurerAmount();
    return new Recompute(
        request.getChanges(),
        request.getShares(),
        premium,
        CommissionTerms.of(BigDecimal.ZERO, commission, vat, BigDecimal.ZERO),
        new Settlement(
            null,
            null,
            request.getStage() == RequestStage.AWAITING_REAPPLICATION,
            arInsurer == null ? zero() : arInsurer),
        impact(commission, vat),
        null,
        request.isQuotationRequired());
  }

  private static Map<LedgerComponent, BigDecimal> changes(Computation computation, Delta delta) {
    Map<LedgerComponent, BigDecimal> map = new EnumMap<>(LedgerComponent.class);
    PremiumComponents p = delta.premium();
    map.put(LedgerComponent.BASIC, p.basic());
    map.put(LedgerComponent.DST, p.dst());
    map.put(LedgerComponent.PREMIUM_TAX_VAT, p.premiumTaxVat());
    map.put(LedgerComponent.LGT, p.lgt());
    map.put(LedgerComponent.FST, p.fst());
    map.put(LedgerComponent.OTHER, p.other());
    map.put(
        LedgerComponent.DTIP,
        computation == Computation.WRITE_OFF ? BigDecimal.ZERO.setScale(2) : p.total());
    map.put(LedgerComponent.COMMISSION, delta.commission().commission());
    map.put(LedgerComponent.COMMISSION_VAT, delta.commission().vatOnCommission());
    return map;
  }

  private static Settlement settlement(
      OpsInvoice invoice, Computation computation, boolean reduces, BigDecimal dtipChange) {
    BigDecimal applied =
        invoice.getComponents().stream()
            .filter(c -> c.getComponent().isPremiumReceivable())
            .map(OpsInvoiceComponent::netApplied)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    OpsInvoiceComponent dtip = invoice.component(LedgerComponent.DTIP);
    BigDecimal arInsurer =
        dtip.getBalance().add(dtipChange).negate().max(BigDecimal.ZERO).min(dtip.getRemitted());
    boolean reapply = reduces && computation != Computation.WRITE_OFF && applied.signum() > 0;
    return new Settlement(applied, dtip.getRemitted(), reapply, reduces ? arInsurer : zero());
  }

  private static ServiceInvoiceImpact impact(BigDecimal commission, BigDecimal vat) {
    int sign = commission.signum();
    String action = sign == 0 ? NONE : ISSUE;
    if (sign < 0) {
      action = CREDIT;
    }
    return new ServiceInvoiceImpact(action, commission.abs(), vat.abs());
  }

  private Baseline baseline(OpsInvoice invoice, BigDecimal change) {
    String original =
        invoice.getParentInvoiceNo() == null
            ? invoice.getInvoiceNo()
            : invoice.getParentInvoiceNo();
    OpsInvoiceAdjustmentTotal total = ledger.adjustmentTotal(original).orElse(null);
    BigDecimal originalPremium =
        total == null ? invoice.getGrossPremium() : total.getOriginalPremium();
    BigDecimal before = total == null ? zero() : total.getAdjustedPremium();
    BigDecimal after = before.add(change);
    BigDecimal limit = limitPercent();
    BigDecimal allowed =
        originalPremium.abs().multiply(limit).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    boolean exceeded =
        change.signum() != 0
            && (after.abs().compareTo(allowed) > 0 || originalPremium.add(after).signum() < 0);
    return new Baseline(originalPremium, before, after, limit, exceeded);
  }

  private BigDecimal limitPercent() {
    try {
      return new BigDecimal(
          parameters.text(BASELINE_PARAMETER, DEFAULT_BASELINE.toPlainString()).strip());
    } catch (NumberFormatException e) {
      return DEFAULT_BASELINE;
    }
  }

  private boolean quotationRequired(
      OpsInvoice invoice, Computation computation, RequestTerms terms) {
    BigDecimal change = terms.sumInsuredChange();
    if (computation != Computation.SUM_INSURED || change == null || change.signum() <= 0) {
      return false;
    }
    Account account = accounts.requireByArn(invoice.getArn());
    BigDecimal endorsed =
        account.getTotalSumInsured() == null ? change : account.getTotalSumInsured().add(change);
    return products.requireProduct(account.getProductCode()).exceedsPackageLimit(endorsed);
  }

  private static BigDecimal zero() {
    return BigDecimal.ZERO.setScale(2);
  }
}
