package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveKey;
import com.iortatechnxt.brokerverse.reserves.domain.TakafulItem;
import com.iortatechnxt.brokerverse.reserves.domain.TakafulSetting;
import com.iortatechnxt.brokerverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.brokerverse.underwriting.service.ClaimsFigures;
import com.iortatechnxt.brokerverse.underwriting.service.CoverPeriod;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.brokerverse.underwriting.service.ReinsuranceFigures;
import com.iortatechnxt.brokerverse.underwriting.service.TransactionRef;
import com.iortatechnxt.brokerverse.underwriting.service.UnderwritingPorts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Takaful surplus / Mudharabah (PGIBR074) of the takaful policies expiring in a period, with the
 * BrokerVerse rule documented on {@link TakafulItem}. Amounts are the company share in base
 * currency of the transactions of the expiring policy period; claims are the incurred claims
 * reported by the claims module (zero without it), retakaful the treaty + FAC premium ceded (zero
 * without the reinsurance module).
 */
@Component
@Transactional(readOnly = true)
public class TakafulCalculator {

  private final PolicyQueryService policies;
  private final UnderwritingPorts ports;

  /**
   * Creates the calculator.
   *
   * @param policies underwriting read API
   * @param ports reinsurance and claims figures (optional ports)
   */
  public TakafulCalculator(PolicyQueryService policies, UnderwritingPorts ports) {
    this.policies = policies;
    this.ports = ports;
  }

  /**
   * Surplus of the takaful policies expiring in a period.
   *
   * @param companyId company
   * @param setting takaful settings of the company
   * @param from expiry date from
   * @param to expiry date to
   * @return one item per expiring takaful policy
   */
  public List<TakafulItem> calculate(
      Long companyId, TakafulSetting setting, LocalDate from, LocalDate to) {
    List<PolicySnapshot> expiring =
        policies.policiesExpiring(companyId, from, to).stream()
            .filter(p -> setting.covers(p.productCode()))
            .toList();
    if (expiring.isEmpty()) {
      return List.of();
    }
    Map<Long, ClaimsFigures> claims =
        ports.claims(expiring.stream().map(PolicySnapshot::id).toList());
    List<TakafulItem> out = new ArrayList<>();
    for (PolicySnapshot p : expiring) {
      out.add(item(p, setting, claims.getOrDefault(p.id(), ClaimsFigures.none())));
    }
    return out;
  }

  private TakafulItem item(PolicySnapshot p, TakafulSetting setting, ClaimsFigures claims) {
    List<PremiumTransaction> txns = currentPeriod(p);
    Map<TransactionRef, ReinsuranceFigures> ri =
        ports.reinsurance(txns.stream().map(PremiumTransaction::ref).toList());
    BigDecimal gross = BigDecimal.ZERO;
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal loading = BigDecimal.ZERO;
    BigDecimal commission = BigDecimal.ZERO;
    BigDecimal retakaful = BigDecimal.ZERO;
    for (PremiumTransaction t : txns) {
      PremiumBreakdown b = t.premium();
      gross = gross.add(t.toBase(b.getOurGrossPremium()));
      discount = discount.add(t.toBase(b.getOurDiscount()));
      loading = loading.add(t.toBase(b.getOurLoading()));
      commission = commission.add(t.toBase(b.getCommission()));
      ReinsuranceFigures r = ri.get(t.ref());
      if (r != null) {
        retakaful = retakaful.add(t.toBase(r.treatyPremium().add(r.facPremium())));
      }
    }
    BigDecimal claimsBase =
        txns.isEmpty() ? Money.round(claims.netClaims()) : txns.get(0).toBase(claims.netClaims());
    BigDecimal applicable =
        gross.subtract(discount).add(loading).subtract(commission).subtract(claimsBase);
    BigDecimal surplus = applicable.subtract(retakaful);
    BigDecimal share =
        surplus.signum() > 0
            ? Percent.of(surplus, setting.terms().participantSharePct())
            : Money.zero();
    BigDecimal tax = Percent.of(share, setting.terms().taxPct());
    return new TakafulItem(
        p.id(),
        p.policyNo(),
        p.insuredName(),
        new ReserveKey(p.branchId(), p.businessLine(), p.productCode(), p.sourceType().name()),
        p.periodTo(),
        Money.round(gross),
        Money.round(discount),
        Money.round(loading),
        Money.round(commission),
        claimsBase,
        Money.round(applicable),
        Money.round(retakaful),
        tax,
        share.subtract(tax));
  }

  /** Approved transactions of the current (expiring) policy period. */
  private List<PremiumTransaction> currentPeriod(PolicySnapshot p) {
    List<PremiumTransaction> all = policies.policyTransactions(p.id());
    Map<TransactionRef, CoverPeriod> covers = policies.coverPeriods(all);
    return all.stream().filter(t -> p.periodTo().equals(covers.get(t.ref()).to())).toList();
  }
}
