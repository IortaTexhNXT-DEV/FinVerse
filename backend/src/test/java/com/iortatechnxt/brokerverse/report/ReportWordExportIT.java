package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Word export through the report API (client requirement 16): the catalogue offers Word for
 * documents and schedules only, the export returns a valid Word file, and batches zip Word files.
 */
@IntegrationTest
class ReportWordExportIT {

  private static final String DOCX = ExportFormat.DOCX.contentType();

  @Autowired private Api api;
  @Autowired private TestData data;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private Map<String, String> params() {
    return Map.of(
        "companyId", data.company().getId().toString(), "asOf", LocalDate.now().toString());
  }

  @Test
  void catalogueOffersWordForDocumentsAndSchedulesOnly() throws Exception {
    JsonNode catalogue = api.read(api.doGet("fmanager", "/api/v1/reports"));
    JsonNode bva = entry(catalogue, "GL-BVA");
    assertThat(bva.get("documentStyle").asBoolean()).isTrue();
    assertThat(bva.get("formats").toString()).contains("PDF", "XLSX", "DOCX");
    JsonNode tb = entry(catalogue, "GL-TB");
    assertThat(tb.get("documentStyle").asBoolean()).isFalse();
    assertThat(tb.get("formats").toString()).contains("PDF", "XLSX").doesNotContain("DOCX");
  }

  @Test
  void exportsADocumentReportAsWord() throws Exception {
    byte[] file =
        api.doPost(
                "fmanager",
                "/api/v1/reports/GL-BVA/export?format=DOCX&paper=A4&orientation=LANDSCAPE",
                params())
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", DOCX))
            .andExpect(header().string("Content-Disposition", containsString("GL-BVA.docx")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(file))) {
      assertThat(doc.getParagraphs().get(0).getText()).isEqualTo("Budget vs Actual");
      assertThat(doc.getTables().get(0).getRow(0).isRepeatHeader()).isTrue();
      assertThat(doc.getTables().get(0).getRow(0).getCell(1).getText()).isEqualTo("Account");
    }
  }

  @Test
  void scheduledFilesAreArchivedAsWord() {
    ReportRun run =
        as.run(
            "fmanager",
            () ->
                reports.generate(
                    "GL-COA",
                    Map.of("companyId", data.company().getId().toString()),
                    ExportFormat.DOCX,
                    null));
    assertThat(run.getFormat()).isEqualTo("DOCX");
    assertThat(run.getFileName()).isEqualTo("GL-COA.docx");
    assertThat(run.getContentType()).isEqualTo(DOCX);
    assertThat(run.getSizeBytes()).isPositive();
  }

  @Test
  void batchesZipWordFiles() throws Exception {
    JsonNode batch =
        api.read(
            api.doPost(
                    "fmanager",
                    "/api/v1/reports/batches",
                    Map.of(
                        "codes",
                        List.of("GL-BVA", "GL-COA"),
                        "parameters",
                        params(),
                        "format",
                        "DOCX",
                        "paper",
                        "LETTER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.format").value("DOCX")));
    byte[] zip =
        api.doGet("fmanager", "/api/v1/reports/batches/" + batch.get("id").asLong() + "/file")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(entries(zip)).containsExactly("01_GL-BVA.docx", "02_GL-COA.docx");
  }

  private static JsonNode entry(JsonNode catalogue, String code) {
    for (JsonNode e : catalogue) {
      if (code.equals(e.get("code").asText())) {
        return e;
      }
    }
    throw new AssertionError(code + " not in the catalogue");
  }

  private static List<String> entries(byte[] zip) throws IOException {
    List<String> names = new ArrayList<>();
    try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
      for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
        names.add(e.getName());
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(in.readAllBytes()))) {
          assertThat(doc.getTables()).isNotEmpty();
        } catch (IOException ex) {
          throw new AssertionError(e.getName() + " is not a Word file", ex);
        }
      }
    }
    return names;
  }
}
