package com.iortatechnxt.brokerverse.eb;

import static com.iortatechnxt.brokerverse.eb.EbFixtures.AO;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.hmo;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.realPdf;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck.AlertSignal;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.bor.service.BorService;
import com.iortatechnxt.brokerverse.eb.cycle.service.CycleService;
import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.franchise.service.FranchiseExpiryJob;
import com.iortatechnxt.brokerverse.eb.franchise.service.FranchiseService;
import com.iortatechnxt.brokerverse.eb.home.service.EbHomeService;
import com.iortatechnxt.brokerverse.eb.service.EbAlertCheck;
import com.iortatechnxt.brokerverse.eb.soa.service.EbSoaService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The EB monitoring (FR-EB-032, 053, 062): the daily alert check raises franchise decisions
 * overdue, outcomes not advised and SOAs not validated in time; the job EB_FRANCHISE_EXPIRY expires
 * a request left without a decision after the grace days; EB Home counts the open member changes
 * and SOAs.
 */
@IntegrationTest
class EbMonitoringIT {

  @Autowired private EbFixtures fx;
  @Autowired private CycleService cycles;
  @Autowired private BorService bor;
  @Autowired private FranchiseService franchises;
  @Autowired private EbFranchiseRequestRepository requests;
  @Autowired private FranchiseExpiryJob expiry;
  @Autowired private EbAlertCheck check;
  @Autowired private EbSoaService soas;
  @Autowired private EbHomeService home;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private Clock clock;

  private List<EbFranchiseRequest> requested() {
    EbProgramme p = fx.programme(false, List.of(hmo(null, null)));
    EbCycle cycle = fx.cycle(p, BusinessType.NEW_BUSINESS, 2028);
    as.run(AO, () -> cycles.start(fx.company(), cycle.getId()));
    EbBor version = as.run(AO, () -> bor.upload(fx.company(), cycle.getId(), realPdf("bor.pdf")));
    LocalDate today = BusinessClock.today(clock);
    as.run(
        "ebtl",
        () ->
            bor.validate(
                fx.company(),
                version.getId(),
                new EbBor.Checklist(true, true, true, today.minusDays(1), today.plusYears(1))));
    as.run(AO, () -> cycles.remarket(fx.company(), cycle.getId()));
    return as.run(
        AO, () -> franchises.request(fx.company(), cycle.getId(), List.of("INS-MGIC", "INS-LAC")));
  }

  private static boolean raised(List<AlertSignal> signals, String code, String reference) {
    return signals.stream()
        .anyMatch(s -> s.code().equals(code) && reference.equals(s.facts().entityId()));
  }

  @Test
  void overdueFranchiseDecisionsAreAlertedThenExpiredAndLateAdvicesAlerted() {
    List<EbFranchiseRequest> sent = requested();
    EbFranchiseRequest waiting = sent.get(0);
    LocalDate today = BusinessClock.today(clock);
    jdbc.update(
        "update eb_franchise_request set due_date = ? where id = ?",
        today.minusDays(30),
        waiting.getId());
    assertThat(raised(check.evaluate(today), "EB_FRANCHISE_OVERDUE", waiting.getFranchiseNo()))
        .isTrue();

    as.run(
        AO,
        () ->
            franchises.decide(
                fx.company(),
                sent.get(1).getId(),
                new FranchiseService.DecisionInput(true, null, null, null, realPdf("ok.pdf"))));
    jdbc.update(
        "update eb_franchise_request set advice_due_date = ? where id = ?",
        today.minusDays(3),
        sent.get(1).getId());
    assertThat(
            raised(check.evaluate(today), "EB_FRANCHISE_ADVICE_LATE", sent.get(1).getFranchiseNo()))
        .isTrue();

    assertThat(expiry.execute(today).message()).contains("expired");
    assertThat(requests.findById(waiting.getId()).orElseThrow().getStatus())
        .isEqualTo(EbFranchiseRequest.Status.EXPIRED);
    assertThat(expiry.name()).isEqualTo("EB_FRANCHISE_EXPIRY");
    assertThat(expiry.description()).isNotBlank();
  }

  @Test
  void anSoaNotValidatedInTimeIsAlertedAndCountedOnEbHome() {
    EbProgramme p = fx.programme(true, List.of(hmo(LocalDate.of(2026, 12, 31), null)));
    EbSoa soa =
        as.run(
            "ebproc",
            () ->
                soas.receive(
                    fx.company(),
                    p.getId(),
                    new EbSoa.Intake(
                        "INS-MGIC",
                        "SOA-" + EbFixtures.token(),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31),
                        new BigDecimal("1000"),
                        null,
                        null,
                        null),
                    List.of(),
                    realPdf("late.pdf")));
    LocalDate today = BusinessClock.today(clock);
    jdbc.update("update eb_soa set received_on = ? where id = ?", today.minusDays(20), soa.getId());
    assertThat(raised(check.evaluate(today), "EB_SOA_VALIDATION_LATE", soa.getSoaNo())).isTrue();
    assertThat(home.counts(fx.company())).containsKeys("memberChangesOpen", "soaToValidate");
    assertThat(home.counts(fx.company()).get("soaToValidate")).isPositive();
  }
}
