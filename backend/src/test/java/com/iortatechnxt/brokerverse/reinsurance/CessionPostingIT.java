package com.iortatechnxt.brokerverse.reinsurance;

import static com.iortatechnxt.brokerverse.reinsurance.RiFixtures.CHECKER;
import static com.iortatechnxt.brokerverse.reinsurance.RiFixtures.MAKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.reinsurance.api.dto.FacAssignRequest;
import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionBasis;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacStatus;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.brokerverse.reinsurance.domain.Treaty;
import com.iortatechnxt.brokerverse.reinsurance.service.AllocationPreviewRow;
import com.iortatechnxt.brokerverse.reinsurance.service.AllocationRunResult;
import com.iortatechnxt.brokerverse.reinsurance.service.AllocationRunService;
import com.iortatechnxt.brokerverse.reinsurance.service.CessionService;
import com.iortatechnxt.brokerverse.reinsurance.service.FacPlacementService;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyReinsuranceView;
import com.iortatechnxt.brokerverse.underwriting.service.ReinsuranceFigures;
import com.iortatechnxt.brokerverse.underwriting.service.TransactionRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cession engine end to end: full allocation on a fire quota share + surplus programme with a
 * facultative remainder, postings and ledger balances, facultative placement, pro-rata endorsement
 * and cancellation, the underwriting view, idempotency and the allocation run.
 */
@IntegrationTest
class CessionPostingIT {

  private static final int YEAR = 2031;

  @Autowired private RiFixtures fx;
  @Autowired private CessionService cessions;
  @Autowired private FacPlacementService placements;
  @Autowired private AllocationRunService runs;
  @Autowired private PolicyReinsuranceView view;
  @Autowired private OpenItemService openItems;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private static BigDecimal sum(Cession c, RiLayer layer, boolean si) {
    return c.getLines().stream()
        .filter(l -> l.getLayer() == layer)
        .map(l -> si ? l.getSumInsured() : l.getPremium())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Test
  void fullAllocationPostsTreatyPremiumAndCreatesTheFacultativeRequirement() {
    RiFixtures.Reinsurers ri = fx.reinsurers();
    Treaty qs = fx.authorized(fx.quotaShare(ri, "TQS31", "FIRE", YEAR, "40", null));
    fx.authorized(fx.surplus(ri, "TSP31", "FIRE", YEAR, "25000000", 3));
    Product fire = fx.product("FIRE");
    Policy policy = fx.policy(fire, YEAR, "PHP", List.of(fx.risk("120000000", "240000")));

    List<Cession> ceded = as.run(MAKER, () -> cessions.cedePolicy(policy.getId()));
    assertThat(ceded).hasSize(1);
    Cession c = ceded.get(0);
    assertThat(c.getBasis()).isEqualTo(CessionBasis.FULL);
    assertThat(c.getTreatyYear()).isEqualTo(YEAR);
    assertThat(sum(c, RiLayer.QUOTA_SHARE, true)).isEqualByComparingTo("10000000");
    assertThat(sum(c, RiLayer.RETENTION, true)).isEqualByComparingTo("15000000");
    assertThat(sum(c, RiLayer.SURPLUS, true)).isEqualByComparingTo("75000000");
    assertThat(sum(c, RiLayer.FAC, true)).isEqualByComparingTo("20000000");
    assertThat(sum(c, RiLayer.QUOTA_SHARE, false)).isEqualByComparingTo("20000");
    assertThat(sum(c, RiLayer.SURPLUS, false)).isEqualByComparingTo("150000");
    assertThat(sum(c, RiLayer.FAC, false)).isEqualByComparingTo("40000");
    assertThat(sum(c, RiLayer.RETENTION, false)).isEqualByComparingTo("30000");
    CessionLine qsLead =
        c.getLines().stream()
            .filter(l -> qs.getId().equals(l.getTreatyId()) && ri.lead().equals(l.getPartyCode()))
            .findFirst()
            .orElseThrow();
    assertThat(qsLead.getPremium()).isEqualByComparingTo("12000");
    assertThat(qsLead.getCommission()).isEqualByComparingTo("3600");
    assertThat(qsLead.getSharePct()).isEqualByComparingTo("5");

    String ref = "RI:CES:" + c.getCessionNo();
    assertThat(fx.posted("4200", ref)).isEqualByComparingTo("170000.00");
    assertThat(fx.posted("4400", ref)).isEqualByComparingTo("-47600.00");
    assertThat(fx.posted("2201", ref)).isEqualByComparingTo("-122400.00");
    assertThat(creditItems(ri.lead(), ref)).isEqualByComparingTo("71400.00");

    ReinsuranceFigures f =
        view.figures(List.of(TransactionRef.original(policy.getId()), new TransactionRef(-1L, 0)))
            .get(TransactionRef.original(policy.getId()));
    assertThat(f.treatyPremium()).isEqualByComparingTo("170000");
    assertThat(f.facPremium()).isEqualByComparingTo("40000");
    assertThat(f.netRetention()).isEqualByComparingTo("30000");
    assertThat(view.figures(List.of())).isEmpty();

    int items = openItems.partyItems(fx.companyId(), partyId(ri.lead())).size();
    List<Cession> again = as.run(MAKER, () -> cessions.cedePolicy(policy.getId()));
    assertThat(again.get(0).getId()).isEqualTo(c.getId());
    assertThat(openItems.partyItems(fx.companyId(), partyId(ri.lead()))).hasSize(items);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where dedup_key = ?",
                Integer.class,
                "RI_TREATY_CAPACITY:" + c.getCessionNo()))
        .isEqualTo(1);

    placeAndEndorse(ri, policy, c);
  }

  private void placeAndEndorse(RiFixtures.Reinsurers ri, Policy policy, Cession original) {
    FacPlacement f =
        placements.list(fx.companyId(), FacStatus.PROVISIONAL).stream()
            .filter(p -> p.getCession().getId().equals(original.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(f.getFacSi()).isEqualByComparingTo("20000000");
    FacAssignRequest slip =
        new FacAssignRequest(
            List.of(
                new FacAssignRequest.Line(ri.fac(), new BigDecimal("70"), new BigDecimal("15"))),
            "Test slip");
    as.run(MAKER, () -> placements.assign(f.getId(), slip));
    assertThatThrownBy(() -> as.run(MAKER, () -> placements.approve(f.getId(), null)))
        .isInstanceOf(BusinessRuleException.class);
    as.run(MAKER, () -> placements.submit(f.getId()));
    assertThatThrownBy(() -> as.run(MAKER, () -> placements.approve(f.getId(), null)))
        .hasMessageContaining("cannot be approved");
    as.run(CHECKER, () -> placements.reject(f.getId()));
    as.run(MAKER, () -> placements.submit(f.getId()));
    FacPlacement placed =
        as.run(CHECKER, () -> placements.approve(f.getId(), LocalDate.of(2026, 3, 20)));
    assertThat(placed.getStatus()).isEqualTo(FacStatus.PLACED);
    assertThat(placed.getPlacedSi()).isEqualByComparingTo("14000000");
    assertThat(placed.placementPct()).isEqualByComparingTo("70");
    assertThat(fx.posted("4200", "RI:FAC:" + placed.getPlacementNo()))
        .isEqualByComparingTo("28000.00");

    fx.endorse(
        policy,
        EndorsementType.ADDITIONAL,
        "12000",
        "6000000",
        LocalDate.of(YEAR, 2, 1),
        LocalDate.of(2026, 4, 15));
    Cession additional =
        as.run(MAKER, () -> cessions.cedePolicy(policy.getId())).stream()
            .filter(x -> x.getEndorsementNo() == 1)
            .findFirst()
            .orElseThrow();
    assertThat(additional.getBasis()).isEqualTo(CessionBasis.PRO_RATA);
    assertThat(sum(additional, RiLayer.QUOTA_SHARE, false)).isEqualByComparingTo("1000");
    assertThat(sum(additional, RiLayer.SURPLUS, false)).isEqualByComparingTo("7500");
    assertThat(sum(additional, RiLayer.FAC, false)).isEqualByComparingTo("1400");
    assertThat(sum(additional, RiLayer.RETENTION, false)).isEqualByComparingTo("2100");

    fx.endorse(
        policy,
        EndorsementType.CANCELLATION,
        null,
        null,
        LocalDate.of(YEAR, 6, 1),
        LocalDate.of(2026, 6, 1));
    Cession cancel =
        as.run(MAKER, () -> cessions.cedePolicy(policy.getId())).stream()
            .filter(x -> x.getEndorsementNo() == 2)
            .findFirst()
            .orElseThrow();
    assertThat(cancel.getOurPremium()).isNegative();
    assertThat(sum(cancel, RiLayer.QUOTA_SHARE, false)).isNegative();
    BigDecimal total =
        cancel.getLines().stream()
            .map(CessionLine::getPremium)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total).isEqualByComparingTo(cancel.getOurPremium());
    assertThat(
            openItems.partyItems(fx.companyId(), partyId(ri.lead())).stream()
                .anyMatch(i -> "REINSURANCE_PREMIUM_RETURN".equals(i.getDocumentType())))
        .isTrue();
    assertThat(cessions.ofPolicyNumber(fx.companyId(), policy.getPolicyNo())).hasSize(3);
    assertThat(cessions.get(cancel.getId()).getLines()).isNotEmpty();
  }

  @Test
  void allocationRunCedesPendingTransactionsOnce() {
    fx.authorized(fx.quotaShare(fx.reinsurers(), "TQS36", "MARINE", 2036, "50", "30000000"));
    Product marine = fx.product("MARINE");
    Policy usd = fx.policy(marine, 2036, "USD", List.of(fx.risk("1000000", "5000")));
    LocalDate day = RiFixtures.APPROVAL;

    List<AllocationPreviewRow> preview = runs.preview(fx.companyId(), day, day);
    AllocationPreviewRow row =
        preview.stream().filter(r -> r.policyId().equals(usd.getId())).findFirst().orElseThrow();
    assertThat(row.basis()).isEqualTo("FULL");
    assertThat(row.fac()).isPositive();
    assertThat(row.quotaShare().add(row.retention()).add(row.fac()))
        .isEqualByComparingTo(row.ourPremium());

    AllocationRunResult result =
        as.run(MAKER, () -> runs.post(fx.companyId(), day, day, JobTrigger.MANUAL));
    assertThat(result.ceded()).isPositive();
    assertThat(result.status()).isEqualTo("SUCCEEDED");
    assertThat(runs.preview(fx.companyId(), day, day))
        .noneMatch(r -> r.policyId().equals(usd.getId()));
    Cession c = cessions.ofPolicy(usd.getId()).get(0);
    assertThat(c.getCurrency()).isEqualTo("USD");
    assertThat(c.getExchangeRate()).isGreaterThan(BigDecimal.ONE);
    assertThat(c.getLines().get(0).getBasePremium())
        .isGreaterThan(c.getLines().get(0).getPremium());
  }

  private Long partyId(String code) {
    return jdbc.queryForObject(
        "select id from pty_party where company_id = ? and code = ?",
        Long.class,
        fx.companyId(),
        code);
  }

  private BigDecimal creditItems(String party, String refPrefix) {
    return openItems.partyItems(fx.companyId(), partyId(party)).stream()
        .filter(i -> i.getSourceReference() != null && i.getSourceReference().startsWith(refPrefix))
        .map(OpenItem::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
