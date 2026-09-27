package com.iortatechnxt.brokerverse.docgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentRenditionService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.DocumentText;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/** The business footer and signature captions of generated documents. */
class DocumentFooterTest {

  private static DocumentSpec spec(String footer) {
    return new DocumentSpec(
        "BDO Insurance and Reinsurance Brokers, Inc.",
        "Placement Slip",
        "PL-2026-000001",
        List.of(),
        List.of(),
        footer);
  }

  @Test
  void theFooterNamesTheDocumentAndTheCompanyAndNeverATemplateCode() {
    assertThat(DocumentText.pageFooter(spec("PLACEMENT_SLIP v1")))
        .isEqualTo("Placement Slip  |  BDO Insurance and Reinsurance Brokers, Inc.  |  Version 1")
        .doesNotContain("PLACEMENT_SLIP");
    assertThat(DocumentText.pageFooter(spec("QUOTATION_LETTER v2 / QUOTATION_TERMS v3")))
        .endsWith("Version 2 / Version 3");
    assertThat(DocumentText.pageFooter(spec(null)))
        .isEqualTo("Placement Slip  |  BDO Insurance and Reinsurance Brokers, Inc.");
    assertThat(new MergedText("PLACEMENT_SLIP", 4, "Placement Slip", "text").versionLabel())
        .isEqualTo("Version 4");
  }

  @Test
  void aTitlePrintedInCapitalsIsWrittenInTitleCaseInTheFooter() {
    DocumentSpec invoice =
        new DocumentSpec("BDOI", "SERVICE INVOICE", "SI-1", List.of(), List.of(), "Version 1");
    assertThat(DocumentText.pageFooter(invoice))
        .isEqualTo("Service Invoice  |  BDOI  |  Version 1");
  }

  @Test
  void aTableTakesRelativeColumnWidthsOrEqualOnes() {
    Table equal = new Table(null, List.of("A", "B"), List.of(), List.of());
    assertThat(equal.columnWeights()).containsExactly(1f, 1f);
    Table weighted = new Table(null, List.of("A", "B"), List.of(), List.of(), List.of(1f, 3f));
    assertThat(weighted.columnWeights()).containsExactly(1f, 3f);
    List<Float> oneWidth = List.of(1f);
    assertThatThrownBy(() -> new Table(null, List.of("A", "B"), List.of(), List.of(), oneWidth))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aOnePageDocumentSaysPageOneOfOneInPdfAndWord() throws IOException {
    DocumentSpec spec =
        new DocumentSpec(
            "BDOI",
            "Hold Cover Request",
            "ARN-1",
            List.of(new Text(null, "Please hold cover.")),
            List.of("Prepared by"),
            "Version 7");
    DocumentComposer composer =
        new DocumentComposer(
            Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneOffset.UTC),
            mock(DocumentRenditionService.class));
    try (PdfReader reader = new PdfReader(composer.pdf(spec))) {
      assertThat(reader.getNumberOfPages()).isEqualTo(1);
      // The total is a template drawn after the footer text, so it is the last character read.
      String page = new PdfTextExtractor(reader).getTextFromPage(1).strip();
      assertThat(page).contains("Page 1 of ").endsWith("Version 7" + "1");
    }
    try (XWPFDocument word = new XWPFDocument(new ByteArrayInputStream(composer.docx(spec)))) {
      String footer =
          word.getFooterList().stream()
              .flatMap(f -> f.getParagraphs().stream())
              .map(p -> p.getCTP().xmlText())
              .collect(Collectors.joining());
      assertThat(footer).contains("NUMPAGES").doesNotContain(">2<");
    }
  }

  @Test
  void aSignatureCarriesTheNameOfThePersonWhoSigns() {
    assertThat(DocumentSpec.signature("Prepared by", "Maria Santos"))
        .isEqualTo("Prepared by: Maria Santos");
    assertThat(DocumentSpec.signature("Approved by", null)).isEqualTo("Approved by");
  }
}
