package com.iortatechnxt.finverse.reinsurance;

import static com.iortatechnxt.finverse.reinsurance.RiFixtures.CHECKER;
import static com.iortatechnxt.finverse.reinsurance.RiFixtures.MAKER;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.CessionBasis;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.reinsurance.service.CessionService;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.ReportService;
import com.iortatechnxt.finverse.report.core.RowKind;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.underwriting.api.dto.EndorsementRequest;
import com.iortatechnxt.finverse.underwriting.domain.Endorsement;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.service.EndorsementService;
import com.iortatechnxt.finverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PolicyService;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A renewal belongs to the underwriting year in which its new period starts, and so do the later
 * endorsements of the renewed period: the premium transactions, the premium registers (PGIBR015 /
 * PGIBR016 group by UW year) and the reinsurance allocation (treaty programme of that year) all use
 * it, while the policy keeps the year of its original issue.
 */
@IntegrationTest
class RenewalUnderwritingYearIT {

  private static final int YEAR = 2040;
  private static final int RENEWED = YEAR + 1;
  private static final LocalDate RENEWAL_DATE = LocalDate.of(2026, 4, 1);
  private static final LocalDate ADDITIONAL_DATE = LocalDate.of(2026, 4, 15);

  @Autowired private RiFixtures fx;
  @Autowired private PolicyService policies;
  @Autowired private EndorsementService endorsements;
  @Autowired private PolicyApprovalService approvals;
  @Autowired private PolicyQueryService policyQueries;
  @Autowired private CessionService cessions;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  @Test
  void renewalAndLaterEndorsementsCarryTheYearOfTheRenewedPeriod() {
    RiFixtures.Reinsurers ri = fx.reinsurers();
    fx.authorized(fx.quotaShare(ri, "UYQS40", "FIRE", YEAR, "40", null));
    fx.authorized(fx.quotaShare(ri, "UYQS41", "FIRE", RENEWED, "20", null));
    Product fire = fx.product("FIRE");
    Policy policy = fx.policy(fire, YEAR, "PHP", List.of(fx.risk("10000000", "100000")));

    // Renewal into a 2041 period, approved (accounted) in 2026 like the original issue.
    Endorsement renewal =
        approve(
            policy,
            new EndorsementRequest(
                EndorsementType.RENEWAL,
                RENEWAL_DATE,
                LocalDate.of(RENEWED, 1, 1),
                "Renewal " + RENEWED,
                null,
                null,
                LocalDate.of(RENEWED, 1, 1),
                LocalDate.of(RENEWED, 12, 31)),
            RENEWAL_DATE);
    assertThat(renewal.getUwYear()).isEqualTo(RENEWED);
    Endorsement additional =
        approve(
            policy,
            new EndorsementRequest(
                EndorsementType.ADDITIONAL,
                ADDITIONAL_DATE,
                LocalDate.of(RENEWED, 6, 1),
                "Additional stock",
                new BigDecimal("20000"),
                new BigDecimal("2000000"),
                null,
                null),
            ADDITIONAL_DATE);
    assertThat(additional.getUwYear()).isEqualTo(RENEWED);
    assertThat(policies.get(policy.getId()).getUwYear()).isEqualTo(YEAR);

    List<PremiumTransaction> txns = policyQueries.policyTransactions(policy.getId());
    assertThat(txns).extracting(PremiumTransaction::uwYear).containsExactly(YEAR, RENEWED, RENEWED);
    assertThat(policyQueries.get(policy.getId()).currentUwYear()).isEqualTo(RENEWED);

    assertRegistersGroupByTransactionYear(fire, policy);
    assertCessionsUseTheProgrammeOfTheTransactionYear(policy);
  }

  private Endorsement approve(Policy policy, EndorsementRequest request, LocalDate date) {
    Endorsement draft = as.run("uw", () -> endorsements.create(policy.getId(), request));
    as.run("uw", () -> endorsements.submit(draft.getId()));
    return as.run(CHECKER, () -> approvals.approveEndorsement(draft.getId(), date));
  }

  private void assertRegistersGroupByTransactionYear(Product fire, Policy policy) {
    Map<String, String> params =
        Map.of(
            "companyId",
            fx.companyId().toString(),
            "fromDate",
            "2026-01-01",
            "toDate",
            "2026-12-31",
            "productCode",
            fire.getCode());
    List<ReportRow> register = as.run(CHECKER, () -> details(reports.run("PGIBR015", params)));
    assertThat(register)
        .filteredOn(r -> policy.getPolicyNo().equals(r.cells().get("policyNo")))
        .extracting(r -> r.cells().get("endtNo") + "@" + r.cells().get("uwYear"))
        .containsExactlyInAnyOrder("@" + YEAR, "1@" + RENEWED, "2@" + RENEWED);

    List<ReportRow> summary = as.run(CHECKER, () -> details(reports.run("PGIBR016", params)));
    assertThat(summary)
        .extracting(r -> r.cells().get("uwYear"))
        .containsExactlyInAnyOrder(String.valueOf(YEAR), String.valueOf(RENEWED));
    assertThat(summary)
        .filteredOn(r -> String.valueOf(RENEWED).equals(r.cells().get("uwYear")))
        .extracting(r -> (BigDecimal) r.cells().get("gross"))
        .singleElement()
        .satisfies(g -> assertThat(g).isEqualByComparingTo("120000"));
  }

  private void assertCessionsUseTheProgrammeOfTheTransactionYear(Policy policy) {
    List<Cession> ceded = as.run(MAKER, () -> cessions.cedePolicy(policy.getId()));
    assertThat(ceded).hasSize(3);
    Cession original = byEndorsement(ceded, 0);
    assertThat(original.getUwYear()).isEqualTo(YEAR);
    assertThat(original.getTreatyYear()).isEqualTo(YEAR);
    assertThat(quotaShareSi(original)).isEqualByComparingTo("4000000");

    Cession renewed = byEndorsement(ceded, 1);
    assertThat(renewed.getBasis()).isEqualTo(CessionBasis.FULL);
    assertThat(renewed.getUwYear()).isEqualTo(RENEWED);
    assertThat(renewed.getTreatyYear()).isEqualTo(RENEWED);
    assertThat(quotaShareSi(renewed)).isEqualByComparingTo("2000000");

    Cession endorsed = byEndorsement(ceded, 2);
    assertThat(endorsed.getBasis()).isEqualTo(CessionBasis.PRO_RATA);
    assertThat(endorsed.getUwYear()).isEqualTo(RENEWED);
    assertThat(endorsed.getTreatyYear()).isEqualTo(RENEWED);
    assertThat(quotaShareSi(endorsed)).isEqualByComparingTo("400000");
  }

  private static List<ReportRow> details(ReportResult result) {
    return result.rows().stream().filter(r -> r.kind() == RowKind.DETAIL).toList();
  }

  private static Cession byEndorsement(List<Cession> ceded, int endorsementNo) {
    return ceded.stream()
        .filter(c -> c.getEndorsementNo() == endorsementNo)
        .findFirst()
        .orElseThrow();
  }

  private static BigDecimal quotaShareSi(Cession c) {
    return c.getLines().stream()
        .filter(l -> l.getLayer() == RiLayer.QUOTA_SHARE)
        .map(l -> l.getSumInsured())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
