package com.iortatechnxt.brokerverse.reserves;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.insurance.ClaimMovementType;
import com.iortatechnxt.brokerverse.insurance.OutstandingClaim;
import com.iortatechnxt.brokerverse.reserves.domain.IbnrMethod;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveType;
import com.iortatechnxt.brokerverse.reserves.domain.RunLine;
import com.iortatechnxt.brokerverse.reserves.domain.TakafulItem;
import com.iortatechnxt.brokerverse.reserves.domain.TakafulTerms;
import com.iortatechnxt.brokerverse.reserves.domain.ValuationRun;
import com.iortatechnxt.brokerverse.reserves.service.ReserveAnalysisService;
import com.iortatechnxt.brokerverse.reserves.service.TakafulSettingService;
import com.iortatechnxt.brokerverse.reserves.service.TriangleAnalysis;
import com.iortatechnxt.brokerverse.reserves.service.ValuationRunService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.underwriting.UwFixtures;
import com.iortatechnxt.brokerverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valuation with the claims and reinsurance ports present (test implementations): OSLR with the
 * reinsurers' share, chain-ladder IBNR from claim movements, treaty / FAC UPR and UCR, the takaful
 * surplus and their postings.
 */
@IntegrationTest
@Import(ReservesTestViews.class)
@Transactional
class ReservesWithClaimsAndReinsuranceIT {

  private static final LocalDate MARCH = LocalDate.of(2026, 3, 31);

  @Autowired private ReserveFixtures fx;
  @Autowired private UwFixtures uw;
  @Autowired private ValuationRunService runs;
  @Autowired private ReserveAnalysisService analysis;
  @Autowired private TakafulSettingService takaful;
  @Autowired private AsUser as;

  @AfterEach
  void clear() {
    ReservesTestViews.reset();
  }

  private static ClaimMovement movement(Policy p, String loss, String at, String amount) {
    return new ClaimMovement(
        p.getCompanyId(),
        p.getBranchId(),
        1L,
        "CL-1",
        p.getId(),
        "ENGG",
        LocalDate.parse(loss),
        LocalDate.parse(at),
        ClaimMovementType.RESERVE_CHANGE,
        "PHP",
        new BigDecimal(amount),
        new BigDecimal(amount),
        "M-" + at);
  }

  /** Short-term policy expiring at the end of March (takaful surplus in the March run). */
  private Policy expiringPolicy(Product product) {
    PolicyRequest base = uw.brokerRequest(product);
    LocalDate issue = LocalDate.of(2026, 1, 5);
    PolicyRequest request =
        new PolicyRequest(
            base.companyId(),
            base.branchId(),
            base.productId(),
            base.customerCode(),
            base.insuredName(),
            base.sourceType(),
            base.intermediaryCode(),
            issue,
            issue,
            MARCH,
            base.currency(),
            base.businessType(),
            base.sharePct(),
            base.coinsurerCode(),
            base.coinsuranceLeader(),
            base.discountRate(),
            base.loadingRate(),
            base.commissionRate(),
            base.risks());
    return uw.issue(request, issue);
  }

  @Test
  void claimsAndReinsuranceFeedOslrIbnrUcrAndTakaful() {
    fx.parameters("ENGG", ReserveFixtures.terms(IbnrMethod.CHAIN_LADDER, "0", "60"));
    Product product = uw.product("ENGG", false);
    Policy annual = uw.issue(uw.brokerRequest(product), UwFixtures.ISSUE);
    Policy expiring = expiringPolicy(product);
    ReservesTestViews.OUTSTANDING.add(
        new OutstandingClaim(
            annual.getCompanyId(),
            annual.getBranchId(),
            1L,
            "CL-1",
            annual.getId(),
            "ENGG",
            LocalDate.of(2026, 3, 12),
            LocalDate.of(2026, 3, 13),
            "PHP",
            new BigDecimal("50000"),
            new BigDecimal("50000")));
    ReservesTestViews.CLAIM_RI.put(1L, new BigDecimal("20000"));
    ReservesTestViews.MOVEMENTS.addAll(
        List.of(
            movement(annual, "2024-05-01", "2024-05-02", "100000"),
            movement(annual, "2024-05-01", "2025-02-02", "50000"),
            movement(annual, "2025-06-01", "2025-06-02", "200000"),
            movement(annual, "2026-03-12", "2026-03-13", "80000")));
    ReservesTestViews.POLICY_CLAIMS.put(expiring.getId(), new BigDecimal("1000"));
    as.run(
        ReserveFixtures.MAKER,
        () ->
            takaful.save(
                fx.companyId(),
                new TakafulTerms(
                    true, product.getCode(), new BigDecimal("70"), new BigDecimal("5"), "FIN")));
    as.run(ReserveFixtures.CHECKER, () -> takaful.authorize(fx.companyId()));
    BigDecimal ucrBefore = fx.balance("2400", MARCH);
    BigDecimal riUprBefore = fx.balance("1301", MARCH);
    BigDecimal surplusBefore = fx.balance("2502", MARCH);

    ValuationRun run = fx.posted(MARCH);

    List<ReserveLineValues> lines = ReserveAnalysisService.values(runs.get(run.getId()));
    ReserveLineValues oslr =
        lines.stream().filter(l -> l.type() == ReserveType.OSLR).findFirst().orElseThrow();
    assertThat(oslr.gross()).isEqualByComparingTo("50000");
    assertThat(oslr.ri()).isEqualByComparingTo("20000");
    assertThat(oslr.key().productCode()).isEqualTo(product.getCode());
    assertThat(fx.total(run, ReserveType.IBNR, RunLine::getGrossAmount)).isPositive();
    assertThat(fx.total(run, ReserveType.MFAD, RunLine::getRiAmount)).isPositive();
    BigDecimal riUpr = fx.total(run, ReserveType.UPR, RunLine::getRiAmount);
    BigDecimal ucr = fx.total(run, ReserveType.DAC, RunLine::getRiAmount);
    assertThat(riUpr).isPositive();
    assertThat(ucr).isPositive();
    assertThat(fx.balance("1301", MARCH).subtract(riUprBefore)).isEqualByComparingTo(riUpr);
    assertThat(fx.balance("2400", MARCH).subtract(ucrBefore)).isEqualByComparingTo(ucr.negate());
    assertThat(run.getRemarks()).doesNotContain("Claims module");

    List<TakafulItem> surplus = runs.takaful(run.getId());
    assertThat(surplus).hasSize(1);
    TakafulItem item = surplus.get(0);
    assertThat(item.claims()).isEqualByComparingTo("1000");
    assertThat(item.retakaful()).isPositive();
    assertThat(item.payable()).isPositive();
    assertThat(fx.balance("2502", MARCH).subtract(surplusBefore))
        .isEqualByComparingTo(item.payable().negate());

    TriangleAnalysis triangle = analysis.triangles(fx.companyId(), "ENGG", MARCH, null, null, null);
    assertThat(triangle.paid().rows()).hasSize(3);
    assertThat(triangle.ibnr()).isPositive();
  }
}
