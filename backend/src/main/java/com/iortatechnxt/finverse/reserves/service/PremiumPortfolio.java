package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.reserves.domain.PremiumAmounts;
import com.iortatechnxt.finverse.reserves.domain.ReserveKey;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.UprBasis;
import com.iortatechnxt.finverse.underwriting.service.CoverPeriod;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.finverse.underwriting.service.ProductService;
import com.iortatechnxt.finverse.underwriting.service.ReinsuranceFigures;
import com.iortatechnxt.finverse.underwriting.service.TransactionRef;
import com.iortatechnxt.finverse.underwriting.service.UnderwritingPorts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the premium portfolio of a company from underwriting: approved premium transactions with
 * their cover period, the product's UPR basis and the reinsurance split (when the reinsurance
 * module provides {@code PolicyReinsuranceView}; otherwise everything is retained).
 *
 * <p>Amounts are the company share in base currency: premium = company net premium (gross of
 * reinsurance, after discount and loading), commission = intermediary commission, reinsurance
 * commission = treaty premium × treaty commission % + FAC premium × FAC commission % of the line of
 * business parameters. Non-financial transactions (NIL endorsements) are skipped.
 */
@Component
@Transactional(readOnly = true)
public class PremiumPortfolio {

  private final PolicyQueryService policies;
  private final ProductService products;
  private final UnderwritingPorts ports;

  /**
   * Creates the loader.
   *
   * @param policies underwriting read API
   * @param products products (UPR basis)
   * @param ports reinsurance split of premium (optional port)
   */
  public PremiumPortfolio(
      PolicyQueryService policies, ProductService products, UnderwritingPorts ports) {
    this.policies = policies;
    this.products = products;
    this.ports = ports;
  }

  /**
   * Loads the transactions approved on or before a date.
   *
   * @param companyId company
   * @param asOf last approval date
   * @param params reserve parameters (reinsurance commission rates)
   * @return portfolio
   */
  public Portfolio load(Long companyId, LocalDate asOf, ReserveParameterSet params) {
    List<PremiumTransaction> txns =
        policies.approvedTransactions(companyId, null, asOf).stream()
            .filter(PremiumPortfolio::isFinancial)
            .toList();
    Map<Long, UprBasis> bases =
        products.list(companyId).stream()
            .collect(Collectors.toMap(Product::getId, Product::getUprBasis));
    Map<TransactionRef, CoverPeriod> covers = policies.coverPeriods(txns);
    Map<TransactionRef, ReinsuranceFigures> ri =
        ports.reinsurance(txns.stream().map(PremiumTransaction::ref).toList());
    return new Portfolio(
        txns.stream()
            .map(
                t ->
                    entry(
                        t,
                        covers.get(t.ref()),
                        bases.getOrDefault(t.policy().productId(), UprBasis.DAYS_365),
                        ri.get(t.ref()),
                        params.terms(t.policy().businessLine())))
            .toList());
  }

  /**
   * Reporting unit of a transaction.
   *
   * @param t transaction
   * @return key
   */
  public static ReserveKey keyOf(PremiumTransaction t) {
    return new ReserveKey(
        t.policy().branchId(),
        t.policy().businessLine(),
        t.policy().productCode(),
        t.policy().sourceType().name());
  }

  private static Portfolio.Entry entry(
      PremiumTransaction t,
      CoverPeriod cover,
      UprBasis basis,
      ReinsuranceFigures ri,
      ReserveParameterTerms terms) {
    PremiumBreakdown b = t.premium();
    ReinsuranceFigures split = ri == null ? ReinsuranceFigures.noCession(b.getOurNetPremium()) : ri;
    BigDecimal treaty = t.toBase(split.treatyPremium());
    BigDecimal fac = t.toBase(split.facPremium());
    BigDecimal riCommission =
        Percent.of(treaty, terms.treatyCommissionPct())
            .add(Percent.of(fac, terms.facCommissionPct()));
    return new Portfolio.Entry(
        t.ref().policyId(),
        t.endorsementNo(),
        t.documentNo(),
        t.kind(),
        keyOf(t),
        basis,
        cover.from(),
        cover.to(),
        t.approvalDate(),
        new PremiumAmounts(
            t.toBase(b.getOurNetPremium()),
            t.toBase(b.getCommission()),
            treaty,
            fac,
            riCommission));
  }

  private static boolean isFinancial(PremiumTransaction t) {
    return t.approvalDate() != null
        && (t.premium().getOurNetPremium().signum() != 0
            || t.premium().getCommission().signum() != 0);
  }
}
