package com.iortatechnxt.brokerverse.docgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class DocTemplateServiceIT {

  @Autowired private DocTemplateService templates;
  @Autowired private AsUser as;

  @Test
  void mergesTheVersionInForceOnTheDate() {
    LocalDate future = LocalDate.of(2099, 1, 1);
    int before = templates.current("HOLD_COVER_REQUEST", LocalDate.now()).getVersionNo();
    as.run(
        "badmin",
        () ->
            templates.newVersion(
                "HOLD_COVER_REQUEST",
                "Hold cover",
                "Hold {{reference}} from {{ startDate }} ({{missing}}).",
                future));
    MergedText today =
        templates.merge(
            "HOLD_COVER_REQUEST",
            LocalDate.now(),
            Map.of("reference", "ARN-1", "startDate", "2026-10-01"));
    assertThat(today.versionNo()).isEqualTo(before);
    assertThat(today.text()).contains("ARN-1", "2026-10-01");
    MergedText later =
        templates.merge(
            "HOLD_COVER_REQUEST", future, Map.of("reference", "ARN-$1", "startDate", "D"));
    assertThat(later.text()).isEqualTo("Hold ARN-$1 from D ().");
    assertThat(later.versionTag()).isEqualTo("HOLD_COVER_REQUEST v" + (before + 1));
    assertThat(templates.all()).extracting("code").contains("QUOTATION_LETTER", "INSURANCE_ADVICE");
    assertThatThrownBy(() -> templates.current("NO_SUCH", LocalDate.now()))
        .hasMessageContaining("NO_SUCH");
    assertThatThrownBy(
            () -> as.run("badmin", () -> templates.newVersion("EPOLICY_EMAIL", "t", " ", future)))
        .extracting("code")
        .isEqualTo("TEMPLATE_BODY_REQUIRED");
  }
}
