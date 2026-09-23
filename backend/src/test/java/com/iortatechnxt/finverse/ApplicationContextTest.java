package com.iortatechnxt.finverse;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.report.core.ReportRegistry;
import com.iortatechnxt.finverse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ApplicationContextTest {

  @Autowired private ReportRegistry reports;

  @Test
  void contextStartsAndReportsAreRegistered() {
    assertThat(reports.catalogue()).extracting("code").contains("GL-TB", "GL-BS", "GL-PL");
  }
}
