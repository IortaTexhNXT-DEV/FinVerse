package com.iortatechnxt.brokerverse.docgen;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import java.util.List;
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
    assertThat(DocumentComposer.pageFooter(spec("PLACEMENT_SLIP v1")))
        .isEqualTo("Placement Slip  |  BDO Insurance and Reinsurance Brokers, Inc.  |  Version 1")
        .doesNotContain("PLACEMENT_SLIP");
    assertThat(DocumentComposer.pageFooter(spec("QUOTATION_LETTER v2 / QUOTATION_TERMS v3")))
        .endsWith("Version 2 / Version 3");
    assertThat(DocumentComposer.pageFooter(spec(null)))
        .isEqualTo("Placement Slip  |  BDO Insurance and Reinsurance Brokers, Inc.");
    assertThat(new MergedText("PLACEMENT_SLIP", 4, "Placement Slip", "text").versionLabel())
        .isEqualTo("Version 4");
  }

  @Test
  void aSignatureCarriesTheNameOfThePersonWhoSigns() {
    assertThat(DocumentSpec.signature("Prepared by", "Maria Santos"))
        .isEqualTo("Prepared by: Maria Santos");
    assertThat(DocumentSpec.signature("Approved by", null)).isEqualTo("Approved by");
  }
}
