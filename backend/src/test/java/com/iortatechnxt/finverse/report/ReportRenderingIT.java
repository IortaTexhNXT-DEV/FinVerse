package com.iortatechnxt.finverse.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.report.core.ReportService;
import com.iortatechnxt.finverse.report.render.ExportFormat;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import com.iortatechnxt.finverse.system.service.SystemParameterService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;

@IntegrationTest
class ReportRenderingIT {

  @Autowired private ReportService reports;
  @Autowired private TestData data;
  @Autowired private SystemParameterService parameters;

  private Map<String, String> companyParams() {
    return Map.of("companyId", data.company().getId().toString());
  }

  @Test
  @WithUserDetails("fmanager")
  void everyGlReportRunsAndExportsInAllFormats() {
    for (String code :
        new String[] {"GL-TB", "GL-BS", "GL-PL", "GL-DETAIL", "GL-JRNL", "GL-COA", "GL-CCY"}) {
      var result = reports.run(code, companyParams());
      assertThat(result.code()).isEqualTo(code);
      for (ExportFormat format : ExportFormat.values()) {
        var file = reports.export(code, companyParams(), format);
        assertThat(file.content()).isNotEmpty();
        assertThat(file.fileName()).endsWith(format.extension());
      }
    }
  }

  @Test
  @WithUserDetails("fmanager")
  void pdfStartsWithPdfSignatureAndPrintsTheConfiguredFooter() throws IOException {
    var file = reports.export("GL-COA", companyParams(), ExportFormat.PDF);
    assertThat(new String(file.content(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    // Other tests may change the parameter: compare with its current value.
    String footer = parameters.text(SystemParameterService.REPORT_FOOTER_TEXT, "").strip();
    assertThat(footer).isNotBlank();
    try (PdfReader reader = new PdfReader(file.content())) {
      assertThat(new PdfTextExtractor(reader).getTextFromPage(1)).contains(footer);
    }
  }

  @Test
  @WithUserDetails("auditor")
  void auditorSeesAuditReport() {
    assertThat(reports.catalogue()).extracting("code").contains("CTL-AUDIT");
  }
}
