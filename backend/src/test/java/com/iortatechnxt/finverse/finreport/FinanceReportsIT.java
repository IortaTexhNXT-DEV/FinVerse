package com.iortatechnxt.finverse.finreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.ReportRow;
import com.iortatechnxt.finverse.report.core.ReportService;
import com.iortatechnxt.finverse.report.core.RowKind;
import com.iortatechnxt.finverse.report.render.ExportFormat;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;

@IntegrationTest
class FinanceReportsIT {

  static final List<String> CODES =
      List.of(
          "FIN-MIS-BS",
          "FIN-MIS-BS-SCH",
          "FIN-MIS-IE-SCH",
          "FIN-MIS-IE",
          "FIN-TB-MAIN",
          "FIN-TB-DIVDEPT",
          "FIN-TB-SUB",
          "FIN-TB-POSTUNP",
          "FIN-TB-YTD",
          "FIN-GL-TXNLIST",
          "FIN-GL-LEDGER-LC",
          "FIN-GL-SUBLEDGER-LC",
          "FIN-GL-CONSSUM",
          "FIN-GL-MISSVCH",
          "FIN-GL-ALLOCJV",
          "FIN-GL-LEDGER-FC",
          "FIN-GL-SUBLEDGER-FC",
          "FIN-CB-POSITION",
          "FIN-GL-VOUCHER",
          "FIN-GL-PROCLIST",
          "FIN-GL-DAYBOOK",
          "FIN-ACT-SUM2",
          "FIN-ACT-DET");

  @Autowired private ReportService reports;
  @Autowired private TestData data;
  @Autowired private FinanceReportFixtures fixtures;

  @BeforeEach
  void seed() {
    fixtures.seed();
  }

  private Map<String, String> params(String... keyValues) {
    Map<String, String> m = new HashMap<>();
    m.put("companyId", data.company().getId().toString());
    m.put("fromDate", LocalDate.now().withDayOfYear(1).toString());
    for (int i = 0; i + 1 < keyValues.length; i += 2) {
      m.put(keyValues[i], keyValues[i + 1]);
    }
    return m;
  }

  private static List<String> labels(ReportResult r) {
    return r.rows().stream().map(ReportRow::label).filter(Objects::nonNull).toList();
  }

  private static BigDecimal total(ReportResult r, String key) {
    return r.rows().stream()
        .filter(row -> row.kind() == RowKind.TOTAL)
        .map(row -> (BigDecimal) row.cells().get(key))
        .findFirst()
        .orElseThrow();
  }

  @Test
  @WithUserDetails("fmanager")
  void everyFinanceReportRunsAndExportsInAllFormats() {
    assertThat(reports.catalogue()).extracting("code").containsAll(CODES);
    for (String code : CODES) {
      ReportResult result = reports.run(code, params());
      assertThat(result.code()).isEqualTo(code);
      assertThat(result.rows()).as(code).isNotEmpty();
      for (ExportFormat format : ExportFormat.values()) {
        var file = reports.export(code, params(), format);
        assertThat(file.content()).as(code + " " + format).isNotEmpty();
      }
    }
  }

  @Test
  @WithUserDetails("fmanager")
  void mainTrialBalanceBalancesAndRollsUpSubAccounts() {
    ReportResult tb = reports.run("FIN-TB-MAIN", params("includeZero", "true"));
    assertThat(tb.notes()).anyMatch(n -> n.startsWith("Trial balance agrees"));
    assertThat(total(tb, "closingDebit")).isEqualByComparingTo(total(tb, "closingCredit"));
    assertThat(tb.rows())
        .anyMatch(r -> "1100".equals(r.cells().get("code")))
        .noneMatch(r -> "1111".equals(r.cells().get("code")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"DIVISION", "MAIN_ACCOUNT"})
  @WithUserDetails("fmanager")
  void divisionTrialBalanceGroupsByDivisionOrMainAccount(String orderBy) {
    ReportResult tb = reports.run("FIN-TB-DIVDEPT", params("orderBy", orderBy, "mainFrom", "5000"));
    assertThat(tb.rows()).anyMatch(r -> "FIN".equals(r.cells().get("department")));
    assertThat(labels(tb))
        .anyMatch(l -> l.startsWith("DIVISION".equals(orderBy) ? "Division" : "Main A/c"));
  }

  @Test
  @WithUserDetails("fmanager")
  void subAccountTrialBalanceFiltersByDepartment() {
    ReportResult tb = reports.run("FIN-TB-SUB", params("costCenter", "FIN", "mainFrom", "5600"));
    assertThat(tb.rows()).anyMatch(r -> "5603".equals(r.cells().get("code")));
    assertThat(tb.rows()).noneMatch(r -> "5610".equals(r.cells().get("code")));
  }

  @Test
  @WithUserDetails("fmanager")
  void postedAndUnpostedTrialBalanceShowsUnpostedMovements() {
    ReportResult both = reports.run("FIN-TB-POSTUNP", params("showAllAccounts", "true"));
    assertThat(total(both, "unpostedDebit")).isGreaterThanOrEqualTo(new BigDecimal("3200.00"));
    ReportResult posted = reports.run("FIN-TB-POSTUNP", params("status", "POSTED"));
    assertThat(total(posted, "unpostedDebit")).isZero();
    ReportResult unposted = reports.run("FIN-TB-POSTUNP", params("status", "UNPOSTED"));
    assertThat(total(unposted, "postedDebit")).isZero();
  }

  @Test
  @WithUserDetails("fmanager")
  void missingVoucherListFindsGapWithinSeries() {
    ReportResult r = reports.run("FIN-GL-MISSVCH", params());
    assertThat(r.rows())
        .anyMatch(
            row ->
                (FinanceReportFixtures.GAP_SERIES + "-000002")
                        .equals(row.cells().get("missingFrom"))
                    && Long.valueOf(2).equals(row.cells().get("count")));
  }

  @Test
  @WithUserDetails("fmanager")
  void allocationJournalListsExpenseLinesUnderPrepaidAccount() {
    ReportResult r =
        reports.run("FIN-GL-ALLOCJV", params("prepaidFrom", "1600", "prepaidTo", "1699"));
    assertThat(labels(r)).anyMatch(l -> l.contains("1601"));
    assertThat(total(r, "lcValue")).isGreaterThanOrEqualTo(new BigDecimal("5000.00"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"COMPANY", "DIVISION", "DEPARTMENT"})
  @WithUserDetails("fmanager")
  void generalLedgerPrintsOpeningRunningAndClosing(String level) {
    ReportResult r = reports.run("FIN-GL-LEDGER-LC", params("level", level));
    assertThat(labels(r)).contains("Opening Balance", "Total transactions", "Closing balance");
    assertThat(labels(r)).contains("Control account summary");
  }

  @Test
  @WithUserDetails("fmanager")
  void foreignCurrencyLedgerKeepsCurrenciesApart() {
    ReportResult r = reports.run("FIN-GL-LEDGER-FC", params("mainFrom", "1100", "mainTo", "1199"));
    assertThat(labels(r)).anyMatch(l -> l.endsWith("/ USD"));
    assertThat(r.rows())
        .anyMatch(row -> new BigDecimal("1000.00").equals(row.cells().get("debit")));
  }

  @Test
  @WithUserDetails("fmanager")
  void subLedgerListsPartiesWithGroupAndGrandTotals() {
    ReportResult lc = reports.run("FIN-GL-SUBLEDGER-LC", params("partyFrom", "C-000201"));
    assertThat(labels(lc)).anyMatch(l -> l.contains("C-000201 - Luzon Steel"));
    assertThat(labels(lc)).anyMatch(l -> l.startsWith("** GROUP TOTAL **"));
    assertThat(labels(lc)).contains("** GRAND TOTAL **");
    ReportResult fc = reports.run("FIN-GL-SUBLEDGER-FC", params("partyTo", "C-000202"));
    assertThat(labels(fc)).anyMatch(l -> l.endsWith("/ PHP"));
  }

  @Test
  @WithUserDetails("fmanager")
  void bankPositionShowsBankAccountsByCurrency() {
    ReportResult r = reports.run("FIN-CB-POSITION", params());
    assertThat(r.rows()).anyMatch(row -> "USD".equals(row.cells().get("currency")));
    assertThat(r.rows()).noneMatch(row -> "5603".equals(row.cells().get("subAc")));
  }

  @Test
  @WithUserDetails("fmanager")
  void voucherPrintHasAmountInWordsAndSignatures() {
    ReportResult r = reports.run("FIN-GL-VOUCHER", params("txnFrom", "JV", "txnTo", "JV"));
    assertThat(labels(r)).anyMatch(l -> l.contains("Philippine Peso Fifteen Thousand and 00/100"));
    assertThat(labels(r)).anyMatch(l -> l.startsWith("Entered by: accountant"));
    ReportResult pending = reports.run("FIN-GL-VOUCHER", params("status", "UNPOSTED"));
    assertThat(labels(pending)).anyMatch(l -> l.contains("Authorised / Approved by: __________"));
  }

  @Test
  @WithUserDetails("fmanager")
  void listingsSummariseByTransactionCode() {
    ReportResult txn =
        reports.run(
            "FIN-GL-TXNLIST",
            params(
                "combine",
                "true",
                "orderBy",
                "DOCUMENT_NUMBER",
                "userId",
                "accountant",
                "status",
                "POSTED"));
    assertThat(labels(txn)).anyMatch(l -> l.startsWith("Transaction-wise Summary JV"));
    assertThat(labels(txn)).anyMatch(l -> l.startsWith("Report-wise Summary"));
    assertThat(total(txn, "debit")).isEqualByComparingTo(total(txn, "credit"));
    ReportResult proc = reports.run("FIN-GL-PROCLIST", params("status", "UNPOSTED"));
    assertThat(proc.rows()).anyMatch(row -> "U".equals(row.cells().get("statusFlag")));
    ReportResult day = reports.run("FIN-GL-DAYBOOK", params("combine", "true"));
    assertThat(labels(day)).contains("Book : All transaction codes");
    assertThat(day.rows()).noneMatch(row -> "U".equals(row.cells().get("statusFlag")));
  }

  @Test
  @WithUserDetails("fmanager")
  void consolidatedSummaryAppliesAmountLimitAndDailyOption() {
    ReportResult daily = reports.run("FIN-GL-CONSSUM", params("dailySummary", "true"));
    assertThat(daily.rows()).anyMatch(row -> row.cells().get("date") != null);
    ReportResult big = reports.run("FIN-GL-CONSSUM", params("amountOver", "50000"));
    assertThat(big.rows())
        .filteredOn(row -> row.kind() == RowKind.DETAIL)
        .allMatch(
            row ->
                ((BigDecimal) row.cells().get("debit"))
                        .add((BigDecimal) row.cells().get("credit"))
                        .compareTo(new BigDecimal("50000"))
                    > 0);
  }

  @Test
  @WithUserDetails("fmanager")
  void activityReportsAnalyseByLineOfBusinessOrCostCentre() {
    ReportResult byLob = reports.run("FIN-ACT-SUM2", params("activityCode", "FIRE"));
    assertThat(byLob.rows())
        .anyMatch(r -> String.valueOf(r.cells().get("activityCaption")).startsWith("FIRE"));
    ReportResult byCc =
        reports.run("FIN-ACT-DET", params("mainHead", "HEAD_2_COST_CENTRE", "combine", "true"));
    assertThat(byCc.rows())
        .anyMatch(
            r -> String.valueOf(r.cells().get("activityCaption")).startsWith("FIN - Finance"));
  }

  @Test
  @WithUserDetails("fmanager")
  void misBalanceSheetBalancesWithCompanyFormat() {
    ReportResult bs =
        reports.run("FIN-MIS-BS", params("formatId", "IC-BS", "lastYearBasis", "PREVIOUS_FY_END"));
    assertThat(bs.notes()).noneMatch(n -> n.startsWith("WARNING"));
    BigDecimal netAssets = lineValue(bs, "NET ASSETS", "thisYear");
    assertThat(netAssets).isEqualByComparingTo(lineValue(bs, "SHAREHOLDERS' FUNDS", "thisYear"));
    ReportResult standard = reports.run("FIN-MIS-BS", params());
    assertThat(lineValue(standard, "NET ASSETS", "thisYear"))
        .isEqualByComparingTo(lineValue(standard, "SHAREHOLDERS' FUNDS", "thisYear"));
    ReportResult schedules = reports.run("FIN-MIS-BS-SCH", params("formatId", "IC-BS"));
    assertThat(labels(schedules)).anyMatch(l -> l.startsWith("Schedule 1 : Cash"));
  }

  @Test
  @WithUserDetails("fmanager")
  void misIncomeStatementUsesIncomeFormatAndRejectsWrongFormat() {
    ReportResult ie =
        reports.run("FIN-MIS-IE", params("formatId", "IC-IE", "rounding", "THOUSANDS"));
    assertThat(lineValue(ie, "NET SURPLUS / (DEFICIT) RETAINED", "thisYearYtd")).isNotNull();
    ReportResult sch = reports.run("FIN-MIS-IE-SCH", params("formatId", "IC-IE"));
    assertThat(labels(sch)).anyMatch(l -> l.contains("Gross Premiums Written"));
    assertThatThrownBy(() -> reports.run("FIN-MIS-IE", params("formatId", "IC-BS")))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> reports.run("FIN-MIS-BS", params("formatId", "NOPE")))
        .isInstanceOf(BusinessRuleException.class);
  }

  private static BigDecimal lineValue(ReportResult r, String caption, String key) {
    return r.rows().stream()
        .filter(
            row -> caption.equals(row.label()) || caption.equals(row.cells().get("particulars")))
        .map(row -> (BigDecimal) row.cells().get(key))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No line " + caption));
  }
}
