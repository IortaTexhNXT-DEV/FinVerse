package com.iortatechnxt.brokerverse.closing;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationRun;
import com.iortatechnxt.brokerverse.closing.domain.FxRevaluationStatus;
import com.iortatechnxt.brokerverse.closing.domain.RevaluationItem;
import com.iortatechnxt.brokerverse.closing.service.CheckItem;
import com.iortatechnxt.brokerverse.closing.service.ClosingChecklistService;
import com.iortatechnxt.brokerverse.closing.service.FxRevaluationService;
import com.iortatechnxt.brokerverse.closing.service.FxRevaluationService.Preview;
import com.iortatechnxt.brokerverse.closing.service.OpenItemRevaluation.OpenItemLine;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.currency.domain.RateType;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.ledger.service.AccountBalance;
import com.iortatechnxt.brokerverse.ledger.service.BalanceQuery;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
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
import org.springframework.jdbc.core.JdbcTemplate;

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
  @Autowired private JdbcTemplate jdbc;

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
    insertOpenItems();
  }

  private void insertOpenItems() {
    jdbc.update(
        "insert into pty_party (company_id, code, name, party_type, default_currency, record_status,"
            + " created_at, created_by) values (?, 'TFX-P1', 'FX test party', 'REINSURER', 'USD',"
            + " 'ACTIVE', now(), 'test')",
        companyId);
    Long partyId =
        jdbc.queryForObject(
            "select id from pty_party where company_id = ? and code = 'TFX-P1'",
            Long.class,
            companyId);
    String sql =
        "insert into sl_open_item (company_id, branch_id, party_id, party_code, direction,"
            + " document_type, document_no, document_date, due_date, currency, amount, base_amount,"
            + " settled_amount, status, source_module, created_at, created_by)"
            + " values (?, ?, ?, 'TFX-P1', ?, 'INVOICE', ?, date '2026-03-05', date '2026-04-05',"
            + " ?, ?, ?, ?, ?, 'TEST', now(), 'test')";
    Long branch = companies.headOffice(companyId);
    jdbc.update(sql, companyId, branch, partyId, "DEBIT", "TFX-D1", "USD", 500, 25_000, 0, "OPEN");
    jdbc.update(
        sql,
        companyId,
        branch,
        partyId,
        "CREDIT",
        "TFX-C1",
        "USD",
        200,
        10_000,
        50,
        "PARTIALLY_SETTLED");
    jdbc.update(sql, companyId, branch, partyId, "DEBIT", "TFX-P1", "PHP", 900, 900, 0, "OPEN");
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
    assertOpenItems(preview.openItems(), closing(MARCH_END));

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
    Map<String, String> params =
        Map.of("companyId", companyId.toString(), "asOfDate", MARCH_END.toString());
    assertThat(asUser.run("fmanager", () -> reports.run("GL-FXREV", params).rows()))
        .hasSizeGreaterThan(2);
  }

  private static void assertOpenItems(List<OpenItemLine> items, BigDecimal rate) {
    assertThat(items)
        .extracting(OpenItemLine::documentNo)
        .containsExactlyInAnyOrder("TFX-D1", "TFX-C1");
    OpenItemLine receivable =
        items.stream().filter(i -> i.direction() == ItemDirection.DEBIT).findFirst().orElseThrow();
    BigDecimal revalued = Money.convert(new BigDecimal("500"), rate);
    assertThat(receivable.bookedBase()).isEqualByComparingTo("25000");
    assertThat(receivable.gainLoss())
        .isEqualByComparingTo(revalued.subtract(new BigDecimal("25000")));
    OpenItemLine payable =
        items.stream().filter(i -> i.direction() == ItemDirection.CREDIT).findFirst().orElseThrow();
    assertThat(payable.outstanding()).isEqualByComparingTo("150");
    assertThat(payable.bookedBase()).isEqualByComparingTo("7500");
    assertThat(payable.gainLoss())
        .isEqualByComparingTo(
            new BigDecimal("7500").subtract(Money.convert(new BigDecimal("150"), rate)));
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
