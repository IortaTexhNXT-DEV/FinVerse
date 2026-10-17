package com.iortatechnxt.brokerverse.attachment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Named patterns of nominated document names (BRNB.026; SNSRP-601). */
class DocumentNamingServiceTest {

  private final DocumentNamingService naming = new DocumentNamingService();

  @Test
  void theDefaultPatternKeepsItsSyntax() {
    assertThat(naming.nominate("ARN-2026-000123", "IDF", 1, "scan.PDF"))
        .isEqualTo("ARN-2026-000123_IDF_1.pdf");
    assertThat(naming.nominate(NamingPattern.DEFAULT, NamingFacts.of("A  B", null, 2, "file")))
        .isEqualTo("A--B_DOC_2");
    assertThat(NamingPattern.DEFAULT.syntax()).isEqualTo(DocumentNamingService.SYNTAX);
  }

  @Test
  void screeningDocumentsAreNamedByFormClientDateAndType() {
    NamingFacts facts =
        new NamingFacts(
            null,
            "KYC_REVIEW",
            "Dela Cruz, Juan",
            LocalDate.of(2026, 9, 15),
            "VALID_ID",
            1,
            "id front.JPG");
    assertThat(naming.nominate(NamingPattern.SCREENING, facts))
        .isEqualTo("KYC-REVIEW_DELA-CRUZ-JUAN_20260915_VALID-ID_1.jpg");
    assertThat(
            naming.nominate(
                NamingPattern.SCREENING, new NamingFacts(null, null, "", null, null, 3, null)))
        .isEqualTo("DOC_DOC_NODATE_DOC_3");
    assertThat(NamingPattern.SCREENING.syntax()).startsWith("<FORM_TYPE>_<CLIENT_NAME>");
  }
}
