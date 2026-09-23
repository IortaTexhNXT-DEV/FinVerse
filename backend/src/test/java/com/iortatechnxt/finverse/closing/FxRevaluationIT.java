package com.iortatechnxt.finverse.closing;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.closing.domain.FxRevaluationRun;
import com.iortatechnxt.finverse.closing.domain.FxRevaluationStatus;
import com.iortatechnxt.finverse.closing.domain.RevaluationItem;
import com.iortatechnxt.finverse.closing.service.CheckItem;
import com.iortatechnxt.finverse.closing.service.ClosingChecklistService;
import com.iortatechnxt.finverse.closing.service.FxRevaluationService;
import com.iortatechnxt.finverse.closing.service.FxRevaluationService.Preview;
import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.currency.domain.RateType;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.finverse.ledger.service.AccountBalance;
import com.iortatechnxt.finverse.ledger.service.BalanceQuery;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.report.core.ReportService;
import com.iortatechnxt.finverse.report.render.ExportFormat;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestCompanies;
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
class FxRevaluationIT {

  private static final LocalDate MARCH_END = LocalDate.of(2026, 3, 31);
  private static final LocalDate APRIL_END = LocalDate.of(2026, 4, 30);
  private static final BigDecimal THOUSAND = new BigDecimal("1000");

  @Autowired private FxRevaluationService revaluation;
  @Autowired private ClosingChecklistService checklist;
  @Autowired private LedgerQueryService ledger;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private CurrencyService currencies;
  @Autowired private ReportService reports;
  @Autowired private TestCompanies companies;
  @Autowired private AsUser asUser;

  private Long companyId;
  private Long dollarBank;
  private Long gainLoss;

  @BeforeAll
  void setUp() {
    companyId = companies.create("TFX", "PHP").getId();
    companies.openYear(companyId, 2026);
    dollarBank = accounts.getByCode(companyId, "1113").getId();
    gainLoss = accounts.getByCode(companyId, "4602").getId();
    companies.post(
        companyId,
        LocalDate.of(2026, 3, 10),
        "PHP",
        List.of(
            new JournalLineRequest(
                "1113",
                BalanceSide.DEBIT,
                THOUSAND,
                "USD",
                new BigDecimal("50"),
                null,
                null,
                null,
                null,
                null,
                null),
            TestCompanies.line("1111", BalanceSide.CREDIT, "50000")));
  }

  private BigDecimal base(Long accountId, LocalDate date) {
    return ledger.netBalance(companyId, accountId, null, date);
  }

  private BigDecimal closing(LocalDate date) {
    return currencies.rateOn("PHP", "USD", RateType.CLOSING, date);
  }

  @Test
  void revaluesAtClosingRateOncePerPeriodAndAutoReverses() {
    Long march = companies.period(companyId, MARCH_END).getId();
    Preview preview = revaluation.preview(companyId, march);
    RevaluationItem item = preview.items().get(0);
    BigDecimal revalued = Money.convert(THOUSAND, closing(MARCH_END));
    assertThat(item.accountCode()).isEqualTo("1113");
    assertThat(item.fcBalance()).isEqualByComparingTo(THOUSAND);
    assertThat(item.bookedBase()).isEqualByComparingTo("50000");
    assertThat(item.revaluedBase()).isEqualByComparingTo(revalued);
    assertThat(item.difference()).isEqualByComparingTo(revalued.subtract(new BigDecimal("50000")));
    assertThat(preview.existingRunId()).isNull();

    FxRevaluationRun run =
        asUser.run("fmanager", () -> revaluation.post(companyId, march, false, null));
    assertThat(run.getJournalBatchNo()).startsWith("REV-");
    assertThat(run.getGainLossAccount()).isEqualTo("4602");
    assertThat(base(dollarBank, MARCH_END)).isEqualByComparingTo(revalued);
    assertThat(base(gainLoss, MARCH_END)).isEqualByComparingTo(item.difference().negate());
    AccountBalance usd =
        ledger.balances(new BalanceQuery(companyId, null, null, MARCH_END, true)).stream()
            .filter(b -> b.accountId().equals(dollarBank))
            .findFirst()
            .orElseThrow();
    assertThat(usd.netFc()).isEqualByComparingTo(THOUSAND);

    FxRevaluationRun again =
        asUser.run("fmanager", () -> revaluation.post(companyId, march, false, null));
    assertThat(again.getId()).isEqualTo(run.getId());
    assertThat(base(dollarBank, MARCH_END)).isEqualByComparingTo(revalued);

    Long april = companies.period(companyId, APRIL_END).getId();
    FxRevaluationRun aprilRun =
        asUser.run("fmanager", () -> revaluation.post(companyId, april, true, "4602"));
    assertThat(aprilRun.getStatus()).isEqualTo(FxRevaluationStatus.REVERSED);
    assertThat(aprilRun.getReversalBatchNo()).isNotNull();
    assertThat(base(dollarBank, APRIL_END))
        .isEqualByComparingTo(Money.convert(THOUSAND, closing(APRIL_END)));
    assertThat(base(dollarBank, APRIL_END.plusDays(1))).isEqualByComparingTo(revalued);

    CheckItem status =
        checklist.periodEnd(companyId, march).stream()
            .filter(c -> c.code().equals("FX_REVALUATION"))
            .findFirst()
            .orElseThrow();
    assertThat(status.passed()).isTrue();
    assertThat(revaluation.list(companyId)).hasSize(2);
    assertThat(revaluation.get(run.getId()).getLines()).hasSize(1);
  }

  @Test
  void registerReportRunsAndExports() {
    for (String date : new String[] {"2026-03-31", "2026-06-30"}) {
      Map<String, String> params = Map.of("companyId", companyId.toString(), "asOfDate", date);
      asUser.run(
          "fmanager",
          () -> {
            assertThat(reports.run("GL-FXREV", params).rows()).isNotEmpty();
            for (ExportFormat format : ExportFormat.values()) {
              assertThat(reports.export("GL-FXREV", params, format).content()).isNotEmpty();
            }
            return null;
          });
    }
  }
}
