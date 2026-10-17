package com.iortatechnxt.brokerverse.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;

class ContentDispositionsTest {

  @Test
  void asciiNameIsSentPlainAndAsFilenameStar() {
    assertThat(ContentDispositions.attachment("E2E-IT-maintenance-invoice.pdf"))
        .isEqualTo(
            "attachment; filename=\"E2E-IT-maintenance-invoice.pdf\";"
                + " filename*=UTF-8''E2E-IT-maintenance-invoice.pdf")
        .doesNotContain("=?UTF-8?");
  }

  @Test
  void nonAsciiNameGetsAsciiFallbackAndPercentEncodedUtf8() {
    String header = ContentDispositions.attachment("Résumé 2026 – ₱.pdf");

    assertThat(header)
        .isEqualTo(
            "attachment; filename=\"R_sum_ 2026 _ _.pdf\";"
                + " filename*=UTF-8''R%C3%A9sum%C3%A9%202026%20%E2%80%93%20%E2%82%B1.pdf");
    // Spring's own parser (RFC 6266) decodes the exact name again.
    assertThat(ContentDisposition.parse(header).getFilename()).isEqualTo("Résumé 2026 – ₱.pdf");
  }

  @Test
  void quotesAreEscapedAndBlankNamesGetADefault() {
    assertThat(ContentDispositions.attachment("a \"b\".txt"))
        .startsWith("attachment; filename=\"a \\\"b\\\".txt\";")
        .endsWith("filename*=UTF-8''a%20%22b%22.txt");
    assertThat(ContentDispositions.attachment(" ")).endsWith("filename*=UTF-8''download");
  }
}
