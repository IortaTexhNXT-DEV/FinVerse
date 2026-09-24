package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportRunAction;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

/** View vs export permission and the archive of generated reports (CSHID.017/018). */
@IntegrationTest
class ReportArchiveIT {

  @Autowired private ReportService reports;
  @Autowired private ReportArchiveService archive;
  @Autowired private AsUser as;
  @Autowired private Api api;

  @Test
  void viewOnlyUsersRunButCannotExportAndExportsAreArchivedWithTheirFile() throws Exception {
    String note = "note-" + UUID.randomUUID();
    Map<String, String> params = Map.of("note", note);
    as.run("comptrol", () -> reports.run(TestArchivedReport.CODE, params));
    assertThatThrownBy(
            () ->
                as.run(
                    "comptrol",
                    () -> reports.export(TestArchivedReport.CODE, params, ExportFormat.CSV)))
        .isInstanceOf(AccessDeniedException.class);
    var file =
        as.run("cashier", () -> reports.export(TestArchivedReport.CODE, params, ExportFormat.CSV));
    assertThat(file.content()).isNotEmpty();

    var runs =
        as.run(
            "cashier",
            () -> archive.runs(TestArchivedReport.CODE, PageRequest.of(0, 50)).getContent());
    ReportRun exported =
        runs.stream()
            .filter(
                r -> r.getAction() == ReportRunAction.EXPORT && r.getParameters().contains(note))
            .findFirst()
            .orElseThrow();
    assertThat(exported.getCreatedBy()).isEqualTo("cashier");
    assertThat(exported.getRowCount()).isEqualTo(1);
    assertThat(runs)
        .anySatisfy(
            r -> {
              assertThat(r.getAction()).isEqualTo(ReportRunAction.VIEW);
              assertThat(r.getCreatedBy()).isEqualTo("comptrol");
            });
    assertThat(as.run("cashier", () -> archive.file(exported.getId())).content())
        .isEqualTo(file.content());
    assertThatThrownBy(() -> as.run("comptrol", () -> archive.file(exported.getId())))
        .isInstanceOf(AccessDeniedException.class);

    api.doGet("comptrol", "/api/v1/reports")
        .andExpect(jsonPath("$[?(@.code == 'OPS-TEST-ARCHIVED')].exportable").value(false));
    api.doGet("cashier", "/api/v1/reports")
        .andExpect(jsonPath("$[?(@.code == 'OPS-TEST-ARCHIVED')].exportable").value(true));
    api.doGet("cashier", "/api/v1/reports/runs/" + exported.getId() + "/file")
        .andExpect(status().isOk());
    api.doGet("comptrol", "/api/v1/reports/runs/" + exported.getId() + "/file")
        .andExpect(status().isForbidden());
  }

  @Test
  void usersWithoutTheReportSeeNoRuns() {
    assertThat(as.run("uw", () -> archive.runs(TestArchivedReport.CODE, PageRequest.of(0, 5))))
        .isEmpty();
  }
}
