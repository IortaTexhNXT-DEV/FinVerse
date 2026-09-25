package com.iortatechnxt.brokerverse.docgen;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Business documents in both formats (client requirement 16): a composed PDF is downloaded again as
 * Word with the same content, Word is rendered directly, and document templates go to Word and
 * back.
 */
@IntegrationTest
class DocumentWordIT {

  private static final String DOCX = DocumentFormat.DOCX.contentType();
  private static final String RENDITIONS = "/api/v1/doc-renditions";

  @Autowired private DocumentComposer composer;
  @Autowired private MockMvc mvc;
  @Autowired private Api api;
  @Autowired private UserDetailsService users;

  private static DocumentSpec spec(String reference) {
    return new DocumentSpec(
        "BDO Insurance and Reinsurance Brokers, Inc.",
        "Placement Slip",
        reference,
        List.of(
            new Text(null, "Please place the risk below.\n\nKindly send the e-policy."),
            new Fields(
                "Account", List.of(new Field("Client", "Santos & Co"), new Field("ARN", null))),
            new Table(
                "Risk items",
                List.of("#", "Risk", "Premium"),
                List.of(List.of("1", "Warehouse", "12,500.00"), List.of("2", "Stock")),
                List.of(2))),
        List.of("Prepared by", "Approved by"),
        "PLACEMENT_SLIP v1");
  }

  private ResultActions upload(String user, String url, String name, byte[] content)
      throws Exception {
    return mvc.perform(
        multipart(url)
            .file(new MockMultipartFile("file", name, "application/octet-stream", content))
            .with(user(users.loadUserByUsername(user))));
  }

  private static String text(XWPFDocument doc) {
    return doc.getParagraphs().stream()
        .map(XWPFParagraph::getText)
        .collect(Collectors.joining("\n"));
  }

  @Test
  void aComposedPdfIsDownloadedAgainAsWordWithTheSameContent() throws Exception {
    String reference = "PL-" + UUID.randomUUID();
    byte[] pdf = composer.pdf(spec(reference));
    assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

    api.doGet("ao", RENDITIONS + "/" + Sha256.hex(pdf))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.title").value("Placement Slip"));
    api.doGet("ao", RENDITIONS + "/" + Sha256.hex(new byte[] {1}))
        .andExpect(jsonPath("$.available").value(false));
    api.doGet("ao", RENDITIONS + "/not-a-hash").andExpect(jsonPath("$.available").value(false));

    byte[] word =
        upload("ao", RENDITIONS + "/word", "slip.pdf", pdf)
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", DOCX))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(word))) {
      assertThat(text(doc))
          .contains("Placement Slip")
          .contains("Reference: " + reference)
          .contains("Please place the risk below.")
          .contains("Kindly send the e-policy.")
          .contains("Account")
          .contains("Risk items");
      List<XWPFTable> tables = doc.getTables();
      assertThat(tables).hasSize(3);
      assertThat(tables.get(0).getRow(0).getCell(1).getText()).isEqualTo("Santos & Co");
      assertThat(tables.get(0).getRow(1).getCell(1).getText()).isEmpty();
      assertThat(tables.get(1).getRow(0).isRepeatHeader()).isTrue();
      assertThat(tables.get(1).getRow(0).getCell(2).getText()).isEqualTo("Premium");
      assertThat(tables.get(1).getRow(0).getCell(0).getColor()).isEqualToIgnoringCase("004EA8");
      assertThat(tables.get(1).getRow(1).getCell(2).getText()).isEqualTo("12,500.00");
      assertThat(tables.get(1).getRow(2).getCell(2).getText()).isEmpty();
      assertThat(tables.get(2).getRow(0).getCell(1).getText()).isEqualTo("Approved by");
      assertThat(doc.getHeaderList().get(0).getText()).contains("BDO Insurance and Reinsurance");
      assertThat(doc.getFooterList().get(0).getText())
          .contains("Confidential  |  PLACEMENT_SLIP v1")
          .contains("Page");
    }

    upload(
            "ao",
            RENDITIONS + "/word",
            "other.pdf",
            "%PDF-1.4 not composed".getBytes(StandardCharsets.US_ASCII))
        .andExpect(status().isNotFound());
    upload("ao", RENDITIONS + "/word", "empty.pdf", new byte[0])
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rendersBothFormatsDirectly() throws IOException {
    DocumentSpec spec = spec("PL-" + UUID.randomUUID());
    assertThat(composer.render(spec, DocumentFormat.PDF))
        .startsWith("%PDF-".getBytes(StandardCharsets.US_ASCII));
    try (XWPFDocument doc =
        new XWPFDocument(new ByteArrayInputStream(composer.render(spec, DocumentFormat.DOCX)))) {
      assertThat(text(doc)).contains("Placement Slip", spec.reference());
      assertThat(doc.getHeaderList().get(0).getAllPictures()).hasSize(1);
    }
  }

  @Test
  void aTemplateGoesToWordAndBackAsTheDraftOfANewVersion() throws Exception {
    byte[] word =
        api.doGet("badmin", "/api/v1/doc-templates/QUOTATION_LETTER/versions/1/docx")
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", DOCX))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    upload("badmin", "/api/v1/doc-templates/QUOTATION_LETTER/docx", "letter.docx", word)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Insurance quotation"))
        .andExpect(
            jsonPath("$.body").value(org.hamcrest.Matchers.startsWith("Dear {{clientName}},")))
        .andExpect(jsonPath("$.missingPlaceholders").isEmpty())
        .andExpect(jsonPath("$.addedPlaceholders").isEmpty());

    byte[] edited = wordFile("Quotation letter", "Dear {{client}},", "Valid until {{validUntil}}.");
    upload("badmin", "/api/v1/doc-templates/QUOTATION_LETTER/docx", "edited.docx", edited)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Quotation letter"))
        .andExpect(jsonPath("$.body").value("Dear {{client}},\n\nValid until {{validUntil}}."))
        .andExpect(jsonPath("$.missingPlaceholders[0]").value("clientName"))
        .andExpect(jsonPath("$.addedPlaceholders[0]").value("client"));

    upload("badmin", "/api/v1/doc-templates/QUOTATION_LETTER/docx", "x.docx", new byte[] {1, 2})
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("TEMPLATE_WORD_INVALID"));
    upload("badmin", "/api/v1/doc-templates/QUOTATION_LETTER/docx", "t.docx", wordFile("Only"))
        .andExpect(status().isUnprocessableEntity());
    upload("badmin", "/api/v1/doc-templates/NO_SUCH/docx", "t.docx", edited)
        .andExpect(status().isNotFound());
    upload("ao", "/api/v1/doc-templates/QUOTATION_LETTER/docx", "t.docx", edited)
        .andExpect(status().isForbidden());
    api.doGet("badmin", "/api/v1/doc-templates/QUOTATION_LETTER/versions/999/docx")
        .andExpect(status().isNotFound());
  }

  private static byte[] wordFile(String... paragraphs) throws IOException {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      for (String p : paragraphs) {
        doc.createParagraph().createRun().setText(p);
      }
      doc.write(out);
      return out.toByteArray();
    }
  }
}
