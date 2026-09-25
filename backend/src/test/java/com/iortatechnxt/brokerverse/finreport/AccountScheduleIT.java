package com.iortatechnxt.brokerverse.finreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleColumn;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Basis;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Comparative;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Grouping;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.LayoutStatus;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.ScheduleFamily;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.SelectorKind;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Side;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleValues;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment.CommentKey;
import com.iortatechnxt.brokerverse.finreport.report.AccountScheduleReport;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleDefinitionService;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.report.core.ExportOptions;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.RowKind;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The account schedule engine on the seeded definitions of the BDOI report pack (FRBS 3.2.0,
 * Appendix A II-IV; report list #9, #45, #46): every definition runs and exports to Excel and PDF,
 * ageing buckets add up to the balance, definitions and commentary are maintained.
 */
@IntegrationTest
class AccountScheduleIT {

  private static final String OFFICER = "glofficer";
  private static final String LEAD = "gltl";

  @Autowired private ReportService reports;
  @Autowired private ScheduleDefinitionService definitions;
  @Autowired private OpsLedgerFixtures ops;
  @Autowired private AsUser as;

  private Map<String, String> params(String schedule) {
    Map<String, String> p = new HashMap<>();
    p.put("companyId", String.valueOf(ops.company()));
    p.put(AccountScheduleReport.SCHEDULE, schedule);
    p.put("asOf", LocalDate.now().toString());
    return p;
  }

  @Test
  void everySeededScheduleRunsAndExportsToExcelAndPdf() {
    List<ScheduleDefinition> all = as.run(OFFICER, () -> definitions.list(true));
    assertThat(all)
        .extracting(ScheduleDefinition::getCode)
        .contains("SCH-PR-PHP", "SCH-PR-USD", "SCH-COMMISSION-INCOME", "GARD-VARIANCE-SIE");
    for (ScheduleDefinition def : all) {
      ReportResult r =
          as.run(OFFICER, () -> reports.run(AccountScheduleReport.CODE, params(def.getCode())));
      assertThat(r.title()).startsWith(def.getCode());
      assertThat(r.columns()).hasSizeGreaterThanOrEqualTo(def.getColumns().size() + 2);
      for (ExportFormat format : List.of(ExportFormat.XLSX, ExportFormat.PDF)) {
        byte[] file =
            as.run(
                    OFFICER,
                    () ->
                        reports.export(
                            AccountScheduleReport.CODE,
                            params(def.getCode()),
                            format,
                            ExportOptions.NONE))
                .content();
        assertThat(file).isNotEmpty();
      }
    }
  }

  @Test
  void premiumReceivablesAreAgedPerPartyAndTheBucketsAddUpToTheBalance() {
    ops.motorInvoice();
    ReportResult r =
        as.run(OFFICER, () -> reports.run(AccountScheduleReport.CODE, params("SCH-PR-PHP")));
    List<ReportRow> rows = r.rows().stream().filter(x -> x.kind() == RowKind.DETAIL).toList();
    assertThat(rows).isNotEmpty();
    assertThat(r.columns()).extracting(c -> c.label()).contains("0-30 days", "Over 730 days");
    for (ReportRow row : rows) {
      BigDecimal buckets = BigDecimal.ZERO;
      for (Map.Entry<String, Object> cell : row.cells().entrySet()) {
        if (cell.getKey().startsWith("age")) {
          buckets = buckets.add((BigDecimal) cell.getValue());
        }
      }
      assertThat(buckets).isEqualByComparingTo((BigDecimal) row.cells().get("closing"));
    }
    assertThat(r.notes()).anyMatch(n -> n.contains("AQ05"));
  }

  @Test
  void definitionsAndCommentaryAreMaintained() {
    ops.motorInvoice();
    String code = "SCH-T" + System.nanoTime() % 1_000_000;
    ScheduleValues values =
        new ScheduleValues(
            "Test schedule",
            ScheduleFamily.OTHER,
            "Test",
            null,
            SelectorKind.ACCOUNT_PREFIX,
            "1210",
            Grouping.BRANCH,
            null,
            Side.DEBIT,
            Basis.BALANCE,
            null,
            Comparative.PREVIOUS_MONTH,
            true,
            false,
            LayoutStatus.CONFIRMED,
            true,
            List.of(
                new ScheduleColumn(10, Measure.CLOSING, "Balance"),
                new ScheduleColumn(20, Measure.COMPARATIVE, "Last month"),
                new ScheduleColumn(30, Measure.VARIANCE, "Change")));
    ScheduleDefinition created =
        as.run(LEAD, () -> definitions.create(code.toLowerCase(java.util.Locale.ROOT), values));
    assertThat(created.getCode()).isEqualTo(code);
    assertThatThrownBy(() -> as.run(LEAD, () -> definitions.create(code, values)))
        .hasMessageContaining(code);
    assertThatThrownBy(() -> as.run(LEAD, () -> definitions.create("bad code!", values)))
        .hasMessageContaining("upper-case");
    ReportResult r = as.run(OFFICER, () -> reports.run(AccountScheduleReport.CODE, params(code)));
    ReportRow firstRow =
        r.rows().stream().filter(x -> x.kind() == RowKind.DETAIL).findFirst().orElseThrow();
    String rowKey = String.valueOf(firstRow.cells().get("code"));
    String period = YearMonth.now().toString();
    CommentKey key = new CommentKey(ops.company(), code, period, rowKey);
    assertThat(as.run(OFFICER, () -> definitions.comment(key, "Higher collections")).getText())
        .isEqualTo("Higher collections");
    assertThat(as.run(OFFICER, () -> definitions.comment(key, "Updated")).getText())
        .isEqualTo("Updated");
    ReportResult commented =
        as.run(OFFICER, () -> reports.run(AccountScheduleReport.CODE, params(code)));
    assertThat(commented.rows())
        .filteredOn(x -> rowKey.equals(x.cells().get("code")))
        .first()
        .satisfies(x -> assertThat(x.cells().get("comment")).isEqualTo("Updated"));
    assertThat(as.run(OFFICER, () -> definitions.comments(ops.company(), code, period))).hasSize(1);
    assertThat(as.run(OFFICER, () -> (Object) definitions.comment(key, " "))).isNull();
    ScheduleValues inactive =
        new ScheduleValues(
            "Test schedule",
            ScheduleFamily.OTHER,
            "Test",
            "Changed",
            SelectorKind.REPORT_GROUP,
            "No Such Group",
            Grouping.ACCOUNT,
            "PHP",
            Side.DEBIT,
            Basis.MOVEMENT,
            null,
            Comparative.NONE,
            false,
            true,
            LayoutStatus.CONFIRMED,
            false,
            List.of(new ScheduleColumn(10, Measure.MOVEMENT, "Movement")));
    as.run(LEAD, () -> definitions.update(code, inactive));
    assertThat(as.run(OFFICER, () -> definitions.list(true)))
        .extracting(ScheduleDefinition::getCode)
        .doesNotContain(code);
    ReportResult empty =
        as.run(OFFICER, () -> reports.run(AccountScheduleReport.CODE, params(code)));
    assertThat(empty.notes()).anyMatch(n -> n.contains("No postable account"));
    assertThatThrownBy(() -> as.run(OFFICER, () -> definitions.comment(key, "x")))
        .hasMessageContaining("no commentary");
  }

  @Test
  void aPeriodStartingAfterItsEndIsRefused() {
    Map<String, String> p = params("GARD-OPEX");
    p.put("fromDate", LocalDate.now().plusDays(1).toString());
    assertThatThrownBy(() -> as.run(OFFICER, () -> reports.run(AccountScheduleReport.CODE, p)))
        .isNotNull();
  }
}
