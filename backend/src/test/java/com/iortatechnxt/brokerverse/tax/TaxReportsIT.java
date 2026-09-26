package com.iortatechnxt.brokerverse.tax;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.service.BirExportService;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleLine;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleResult;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Every tax and IC report runs and exports to PDF, Excel and CSV; the BIR list exports follow the
 * relief layout; the IC schedules agree with the ledger.
 */
@IntegrationTest
class TaxReportsIT {

  static final List<String> CODES =
      List.of(
          "TAX-VAT-2550Q",
          "TAX-SLS",
          "TAX-SLP",
          "TAX-EWT-1601EQ",
          "TAX-QAP",
          "TAX-2307-REG",
          "TAX-DST-2000",
          "TAX-PREMTAX",
          "TAX-REMIT",
          "IC-PREM-LOB",
          "IC-LOSS-LOB",
          "IC-COMM-LOB",
          "IC-NETWORTH",
          "IC-RBC",
          "IC-RESERVES",
          "IC-INVEST");

  private static final LocalDate DAY = LocalDate.of(2026, 8, 20);

  @Autowired private ReportService reports;
  @Autowired private BirExportService exports;
  @Autowired private IcScheduleService schedules;
  @Autowired private TaxFixtures fixtures;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  @BeforeEach
  void scenario() {
    fixtures.masters();
    fixtures.firePolicy(DAY, "60000");
    fixtures.supplierInvoice(DAY, "5000");
  }

  @Test
  void everyTaxReportRunsAndExportsInAllFormats() {
    as.run("fmanager", this::runAndExportEveryReport);
  }

  private Void runAndExportEveryReport() {
    assertThat(reports.catalogue()).extracting("code").containsAll(CODES);
    for (String code : CODES) {
      ReportResult result = reports.run(code, params());
      assertThat(result.code()).isEqualTo(code);
      for (ExportFormat format : ExportFormat.values()) {
        assertThat(reports.export(code, params(), format).content())
            .as(code + " " + format)
            .isNotEmpty();
      }
    }
    assertThat(reports.run("TAX-VAT-2550Q", params()).rows()).isNotEmpty();
    assertThat(reports.run("TAX-DST-2000", params()).rows()).isNotEmpty();
    assertThat(reports.run("IC-PREM-LOB", params()).rows()).isNotEmpty();
    assertThat(reports.run("IC-RBC", params()).notes()).anyMatch(n -> n.startsWith("RBC ratio"));
    return null;
  }

  @Test
  void birListsFollowTheReliefLayout() {
    Long company = fixtures.companyId();
    String sls = text(exports.sales(company, 2026, 3).content());
    assertThat(sls).startsWith("H,S,\"000123456\"").contains("\r\nD,S,\"301222333\"");
    String slp = text(exports.purchases(company, 2026, 3).content());
    assertThat(slp).startsWith("H,P,").contains("D,P,\"802111222\"");
    String qap = text(exports.alphalist(company, 2026, 3).content());
    assertThat(qap).startsWith("HQAP,H1601EQ,000123456,000,").contains("D1,1601EQ,");
    assertThat(qap.lines().reduce((a, b) -> b).orElseThrow()).startsWith("C1,1601EQ,000123456");
    assertThat(exports.sales(company, 2026, 3).fileName()).isEqualTo("000123456SLS2026Q3.csv");
  }

  @Test
  void icSchedulesAgreeWithTheLedger() {
    Long company = fixtures.companyId();
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 9, 30);
    IcScheduleResult premiums = schedules.compute(company, IcSchedule.PREMIUMS, from, to);
    assertThat(premiums.lines()).extracting(IcScheduleLine::businessLine).contains("FIRE");
    BigDecimal written = movement(company, "4100", from, to).negate();
    BigDecimal ceded = movement(company, "4200", from, to);
    assertThat(premiums.total()).isEqualByComparingTo(written.subtract(ceded));

    IcScheduleResult reserves = schedules.compute(company, IcSchedule.RESERVES, from, to);
    BigDecimal expected =
        balance(company, "2101", "2104", to)
            .negate()
            .subtract(balance(company, "1301", "1303", to));
    assertThat(reserves.total()).isEqualByComparingTo(expected);

    IcScheduleResult rbc = schedules.compute(company, IcSchedule.RBC, from, to);
    assertThat(rbc.rbc()).isNotNull();
    assertThat(rbc.rbc().netWorth())
        .isEqualByComparingTo(schedules.compute(company, IcSchedule.NET_WORTH, from, to).total());
    assertThat(rbc.rbc().requirement())
        .isEqualByComparingTo(
            rbc.lines().stream()
                .map(IcScheduleLine::requirement)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    assertThat(rbc.rbc().hurdle()).isEqualByComparingTo("100");
  }

  private Map<String, String> params() {
    return Map.of(
        "companyId", fixtures.companyId().toString(),
        "fromDate", "2026-07-01",
        "toDate", "2026-09-30",
        "year", "2026",
        "quarter", "3",
        "taxType", "FST");
  }

  private BigDecimal movement(Long company, String account, LocalDate from, LocalDate to) {
    return jdbc.queryForObject(
        """
        select coalesce(sum(e.debit_base - e.credit_base), 0) from gl_ledger_entry e
        join coa_account a on a.id = e.account_id
        where e.company_id = ? and a.code = ? and e.value_date between ? and ?
          and e.journal_type <> 'CLOSING'
        """,
        BigDecimal.class,
        company,
        account,
        from,
        to);
  }

  private BigDecimal balance(Long company, String fromCode, String toCode, LocalDate asOf) {
    return jdbc.queryForObject(
        """
        select coalesce(sum(b.debit_base - b.credit_base), 0) from gl_daily_balance b
        join coa_account a on a.id = b.account_id
        where b.company_id = ? and a.code between ? and ? and b.balance_date <= ?
        """,
        BigDecimal.class,
        company,
        fromCode,
        toCode,
        asOf);
  }

  private static String text(byte[] content) {
    return new String(content, StandardCharsets.UTF_8);
  }
}
