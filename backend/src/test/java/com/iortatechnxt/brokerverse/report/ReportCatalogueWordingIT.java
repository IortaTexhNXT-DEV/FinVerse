package com.iortatechnxt.brokerverse.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.util.BusinessText;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportRegistry;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The report catalogue served to the Report Centre shows business wording only: no requirement
 * references ("ADJID.021", "BRNB.110", "Annex II #14", "OQ42") and no design notes ("layout to
 * confirm") in any title or description (client feedback, 26-Sep-2026).
 */
@IntegrationTest
class ReportCatalogueWordingIT {

  @Autowired private ReportRegistry registry;

  @Test
  void catalogueTitlesAndDescriptionsCarryNoRequirementReferences() {
    List<String> findings =
        registry.catalogue().stream()
            .flatMap(
                (ReportMetadata m) ->
                    java.util.stream.Stream.of(
                        m.code() + " title: " + m.title(),
                        m.code() + " description: " + m.description()))
            .filter(text -> BusinessText.FORBIDDEN.matcher(text).find())
            .toList();
    assertThat(registry.catalogue()).isNotEmpty();
    assertThat(findings).isEmpty();
  }
}
