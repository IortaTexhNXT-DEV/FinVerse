package com.iortatechnxt.brokerverse.productmaint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbreport.domain.ReportVariant;
import com.iortatechnxt.brokerverse.nbreport.service.ReportVariantService;
import com.iortatechnxt.brokerverse.productmaint.domain.PlacementReport;
import com.iortatechnxt.brokerverse.productmaint.report.PmReportBuilder;
import com.iortatechnxt.brokerverse.productmaint.service.PlacementUpdateJob;
import com.iortatechnxt.brokerverse.productmaint.service.PlacementUpdateService;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Consolidated Placement Update Report (BDOI FRS FRPM.007.01): the weekly run on the configured
 * day and time, BDOI's columns and file name, the repository with preview and download (the Excel
 * file has the rows of the preview); and the report builder whose shared templates give another
 * authorized user the same result (FRPM.020.01).
 */
@IntegrationTest
class PlacementUpdateIT {

  private static final String BASE = "/api/v1/product-maintenance/placement-reports";

  @Autowired private PlacementUpdateJob job;
  @Autowired private PlacementUpdateService placements;
  @Autowired private ReportService reports;
  @Autowired private ReportVariantService variants;
  @Autowired private SystemParameterService parameters;
  @Autowired private TestData data;
  @Autowired private AsUser as;
  @Autowired private Api api;

  private static ZonedDateTime nextThursdayAt(LocalTime time) {
    LocalDate thursday =
        LocalDate.now(BusinessClock.zone())
            .plusWeeks(5)
            .with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
    return thursday.atTime(time).atZone(BusinessClock.zone());
  }

  private static List<List<String>> excelRows(byte[] xlsx) throws Exception {
    List<List<String>> out = new ArrayList<>();
    DataFormatter format = new DataFormatter();
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = wb.getSheetAt(0);
      boolean data = false;
      for (Row row : sheet) {
        String first = row.getCell(0) == null ? "" : format.formatCellValue(row.getCell(0));
        if (data && !first.isBlank()) {
          List<String> cells = new ArrayList<>();
          for (int i = 0; i < 12; i++) {
            cells.add(row.getCell(i) == null ? "" : format.formatCellValue(row.getCell(i)));
          }
          out.add(cells);
        }
        data = data || "Item No.".equals(first);
      }
    }
    return out;
  }

  @Test
  void theWeeklyRunListsTheQuotationRequestsAndTheExcelFileHasThePreviewRows() throws Exception {
    Long company = data.company().getId();
    ZonedDateTime early = nextThursdayAt(LocalTime.of(7, 30));
    assertThat(placements.due(company, early)).isFalse();
    ZonedDateTime due = nextThursdayAt(LocalTime.of(8, 5));
    assertThat(placements.due(company, due)).isTrue();
    assertThat(job.run(due).itemsProcessed()).isPositive();
    assertThat(placements.due(company, due)).isFalse();
    assertThat(job.run(due.plusHours(1)).itemsProcessed()).isZero();

    PlacementReport report =
        as.run(
            "tsuhead",
            () ->
                placements
                    .list(company, org.springframework.data.domain.PageRequest.of(0, 5))
                    .getContent()
                    .get(0));
    String mmddyyyy =
        String.format("%02d%02d%d", due.getMonthValue(), due.getDayOfMonth(), due.getYear());
    assertThat(report.getFileName()).isEqualTo("PlacementUpdate_" + mmddyyyy + ".xlsx");
    assertThat(report.getPeriodFrom()).isEqualTo(due.toLocalDate().minusDays(6));
    assertThat(report.getTriggerType()).isEqualTo(PlacementUpdateService.SCHEDULED);
    assertThat(report.getRecordCount()).isPositive();

    JsonNode preview =
        api.read(
            api.doGet("tsuhead", BASE + "/" + report.getId() + "/preview")
                .andExpect(status().isOk()));
    assertThat(preview.path("columns").findValuesAsText("label"))
        .containsExactly(
            "Item No.",
            "Insured's Name",
            "Marketing Segment",
            "Team",
            "Account Officer",
            "TSU Handler",
            "Line of Insurance",
            "Sub-Line",
            "PRF Received Date",
            "PRF Received Time",
            "Aging (Days)",
            "Status");
    JsonNode rows = preview.path("rows");
    assertThat(rows).hasSize(report.getRecordCount());
    assertThat(rows.get(0).path("status").asText()).isNotBlank();

    byte[] xlsx =
        api.download("tsuhead", BASE + "/" + report.getId() + "/file")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    List<List<String>> excel = excelRows(xlsx);
    assertThat(excel).hasSize(rows.size());
    for (int i = 0; i < rows.size(); i++) {
      assertThat(excel.get(i).get(0)).isEqualTo(rows.get(i).path("itemNo").asText());
      assertThat(excel.get(i).get(1)).isEqualTo(rows.get(i).path("insured").asText());
      assertThat(excel.get(i).get(11)).isEqualTo(rows.get(i).path("status").asText());
    }

    JsonNode list = api.read(api.doGet("tsuhead", BASE + "?companyId=" + company));
    assertThat(list.path("content").findValuesAsText("trigger")).contains("Scheduled");
    api.doGet("ao", BASE + "?companyId=" + company).andExpect(status().isForbidden());
    JsonNode made =
        api.read(api.doPost("tsuhead", BASE + "/generate", Map.of("companyId", company)));
    assertThat(made.path("trigger").asText()).isEqualTo("On request");
    JsonNode settings = api.read(api.doGet("tsuhead", BASE + "/settings"));
    assertThat(settings.path("day").asText()).isEqualTo("THURSDAY");
    assertThat(settings.path("time").asText()).isEqualTo("08:00");
  }

  @Test
  void theScheduleFollowsTheConfiguredDay() {
    Long company = data.company().getId();
    ZonedDateTime thursday = nextThursdayAt(LocalTime.of(9, 0)).plusWeeks(1);
    as.run("admin", () -> parameters.update("PLACEMENT_UPDATE_DAY", "FRIDAY"));
    try {
      assertThat(placements.due(company, thursday)).isFalse();
      assertThat(placements.due(company, thursday.plusDays(1))).isTrue();
    } finally {
      as.run("admin", () -> parameters.update("PLACEMENT_UPDATE_DAY", "THURSDAY"));
    }
  }

  @Test
  void aSharedReportTemplateGivesAnotherAuthorizedUserTheSameResult() {
    Long company = data.company().getId();
    Map<String, String> params =
        Map.of(
            "companyId",
            String.valueOf(company),
            PmReportBuilder.SOURCE,
            "QUOTATION_REQUESTS",
            PmReportBuilder.COLUMNS,
            "REFERENCE,CLIENT,LINE,STATUS,AGING",
            "sortBy",
            "CLIENT");
    ReportVariant saved =
        as.run(
            "tsuhead",
            () ->
                variants.save(
                    PmReportBuilder.CODE, "Open quotations by client", params, true, false));
    ReportResult mine = as.run("tsuhead", () -> reports.run(PmReportBuilder.CODE, params));
    assertThat(mine.columns())
        .extracting("label")
        .containsExactly(
            "Reference No.", "Client / Insured", "Line of Insurance", "Status", "Aging (Days)");
    assertThat(mine.rows()).isNotEmpty();

    ReportVariant shared =
        as.run(
            "tsulead",
            () ->
                variants.visible(PmReportBuilder.CODE).stream()
                    .filter(v -> v.getId().equals(saved.getId()))
                    .findFirst()
                    .orElseThrow());
    // The template keeps the report's choices; the company comes from the user's workspace.
    Map<String, String> theirs =
        new java.util.HashMap<>(as.run("tsulead", () -> variants.parameters(shared)));
    theirs.put("companyId", String.valueOf(company));
    ReportResult other = as.run("tsulead", () -> reports.run(PmReportBuilder.CODE, theirs));
    assertThat(other.rows().stream().map(ReportRow::cells).toList())
        .isEqualTo(mine.rows().stream().map(ReportRow::cells).toList());
  }
}
