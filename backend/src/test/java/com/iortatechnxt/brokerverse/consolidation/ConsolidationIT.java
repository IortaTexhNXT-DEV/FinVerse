package com.iortatechnxt.brokerverse.consolidation;

import static com.iortatechnxt.brokerverse.support.TestCompanies.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.consolidation.api.dto.GroupRequest;
import com.iortatechnxt.brokerverse.consolidation.api.dto.IntercompanyTransactionRequest;
import com.iortatechnxt.brokerverse.consolidation.api.dto.RelationshipRequest;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationGroup;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationLineType;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationRun;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationRunLine;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationRunStatus;
import com.iortatechnxt.brokerverse.consolidation.domain.IntercompanyTransactionType;
import com.iortatechnxt.brokerverse.consolidation.service.ConsolidatedBalance;
import com.iortatechnxt.brokerverse.consolidation.service.ConsolidationGroupService;
import com.iortatechnxt.brokerverse.consolidation.service.ConsolidationRunService;
import com.iortatechnxt.brokerverse.consolidation.service.IntercompanyService;
import com.iortatechnxt.brokerverse.currency.domain.RateType;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsolidationIT {

  private static final LocalDate AS_OF = LocalDate.of(2026, 8, 31);
  private static final String GROUP = "TGRP";

  @Autowired private ConsolidationGroupService groups;
  @Autowired private ConsolidationRunService runs;
  @Autowired private IntercompanyService intercompany;
  @Autowired private CurrencyService currencies;
  @Autowired private ReportService reports;
  @Autowired private TestCompanies companies;
  @Autowired private AsUser asUser;

  private Long parent;
  private Long subsidiary;
  private ConsolidationGroup group;

  @BeforeAll
  void setUp() {
    parent = companies.create("TCP", "PHP").getId();
    subsidiary = companies.create("TCS", "USD").getId();
    companies.openYear(parent, 2026);
    companies.openYear(subsidiary, 2026);
    LocalDate jan = LocalDate.of(2026, 1, 5);
    companies.post(
        subsidiary,
        jan,
        "USD",
        List.of(
            line("1111", BalanceSide.DEBIT, "10000"), line("3100", BalanceSide.CREDIT, "10000")));
    companies.post(
        parent,
        jan,
        "USD",
        List.of(line("1506", BalanceSide.DEBIT, "8000"), line("3200", BalanceSide.CREDIT, "8000")));
    companies.post(
        subsidiary,
        LocalDate.of(2026, 2, 10),
        "USD",
        List.of(
            line("1111", BalanceSide.DEBIT, "5000"),
            line("4100", BalanceSide.CREDIT, "5000", null, "FIRE")));
    asUser.run(
        "fmanager",
        () -> {
          intercompany.createRelationship(
              new RelationshipRequest(parent, "1607", "2510", subsidiary, "1607", "2510"));
          return intercompany.post(
              new IntercompanyTransactionRequest(
                  IntercompanyTransactionType.CHARGE,
                  parent,
                  subsidiary,
                  LocalDate.of(2026, 3, 16),
                  "USD",
                  new BigDecimal("1000.00"),
                  "4700",
                  "5605",
                  "Management fee",
                  "FIN",
                  null));
        });
    group =
        asUser.run(
            "fmanager",
            () ->
                groups.create(
                    new GroupRequest(
                        GROUP,
                        "Test group",
                        parent,
                        "PHP",
                        "3450",
                        "3600",
                        "1850",
                        true,
                        List.of(
                            new GroupRequest.Member(
                                subsidiary, new BigDecimal("80"), "1506", List.of("3100"))))));
  }

  private static BigDecimal consolidated(ConsolidationRun run, String account) {
    ConsolidatedBalance b = ConsolidationRunService.trialBalance(run).get(account);
    return b == null ? BigDecimal.ZERO : b.consolidated();
  }

  @Test
  void translatesEliminatesAndBalances() {
    ConsolidationRun run = asUser.run("fmanager", () -> runs.run(group.getId(), AS_OF));
    assertThat(run.isBalanced()).isTrue();
    assertThat(run.getTotalDebit()).isPositive().isEqualByComparingTo(run.getTotalCredit());
    BigDecimal sum =
        ConsolidationRunService.trialBalance(run).values().stream()
            .map(ConsolidatedBalance::consolidated)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum).isEqualByComparingTo("0");

    BigDecimal closing = currencies.rateOn("PHP", "USD", RateType.CLOSING, AS_OF);
    ConsolidationRunLine bank =
        run.getLines().stream()
            .filter(l -> subsidiary.equals(l.getCompanyId()) && l.getAccountCode().equals("1111"))
            .findFirst()
            .orElseThrow();
    assertThat(bank.getLocalAmount()).isEqualByComparingTo("15000.00");
    assertThat(bank.getRate()).isEqualByComparingTo(closing);
    assertThat(bank.getAmount())
        .isEqualByComparingTo(Money.convert(new BigDecimal("15000"), closing));
    ConsolidationRunLine premium =
        run.getLines().stream()
            .filter(l -> subsidiary.equals(l.getCompanyId()) && l.getAccountCode().equals("4100"))
            .findFirst()
            .orElseThrow();
    assertThat(premium.getRate()).isNotEqualByComparingTo(closing);
    assertThat(run.getLines()).anyMatch(l -> l.getType() == ConsolidationLineType.CTA);

    assertThat(consolidated(run, "1607")).isEqualByComparingTo("0");
    assertThat(consolidated(run, "2510")).isEqualByComparingTo("0");
    assertThat(consolidated(run, "1506")).isEqualByComparingTo("0");
    assertThat(consolidated(run, "3100")).isEqualByComparingTo("0");
    assertThat(consolidated(run, "3600"))
        .isCloseTo(
            Money.round(new BigDecimal("-2000").multiply(closing)), within(new BigDecimal("0.01")));
  }

  @Test
  void rerunCancelsDraftAndFinalRunBlocksDuplicates() {
    LocalDate date = LocalDate.of(2026, 7, 31);
    ConsolidationRun first = asUser.run("fmanager", () -> runs.run(group.getId(), date));
    ConsolidationRun second = asUser.run("fmanager", () -> runs.run(group.getId(), date));
    assertThat(runs.get(first.getId()).getStatus()).isEqualTo(ConsolidationRunStatus.CANCELLED);
    ConsolidationRun finalRun = asUser.run("fmanager", () -> runs.finalizeRun(second.getId()));
    assertThat(finalRun.getStatus()).isEqualTo(ConsolidationRunStatus.FINAL);
    assertThat(finalRun.getFinalizedBy()).isEqualTo("fmanager");
    assertThatThrownBy(() -> asUser.run("fmanager", () -> runs.run(group.getId(), date)))
        .hasMessageContaining("already exists");
    assertThat(runs.list(group.getId())).hasSizeGreaterThanOrEqualTo(2);
    assertThat(runs.latest(GROUP, date).orElseThrow().getId()).isEqualTo(second.getId());
  }

  @Test
  void groupUpdateKeepsMembers() {
    ConsolidationGroup updated =
        asUser.run(
            "fmanager",
            () ->
                groups.update(
                    group.getId(),
                    new GroupRequest(
                        GROUP,
                        "Test group renamed",
                        parent,
                        "PHP",
                        "3450",
                        "3600",
                        "1850",
                        true,
                        List.of(
                            new GroupRequest.Member(
                                subsidiary, new BigDecimal("80"), "1506", List.of("3100"))))));
    assertThat(updated.getName()).isEqualTo("Test group renamed");
    assertThat(groups.getByCode(GROUP).getMembers()).hasSize(1);
    assertThat(groups.list()).extracting(ConsolidationGroup::getCode).contains(GROUP);
  }

  @Test
  void consolidationReportsRunAndExport() {
    asUser.run("fmanager", () -> runs.run(group.getId(), LocalDate.of(2026, 6, 30)));
    Map<String, String> params = Map.of("groupCode", GROUP, "asOfDate", "2026-06-30");
    asUser.run(
        "fmanager",
        () -> {
          for (String code : new String[] {"GL-CON-TB", "GL-CON-BS", "GL-CON-PL", "GL-CON-ELIM"}) {
            assertThat(reports.run(code, params).rows()).isNotEmpty();
            for (ExportFormat format : ExportFormat.values()) {
              assertThat(reports.export(code, params, format).content()).isNotEmpty();
            }
          }
          assertThat(reports.run("GL-CON-TB", Map.of("groupCode", GROUP)).notes())
              .anyMatch(n -> n.contains("in balance"));
          return null;
        });
  }
}
