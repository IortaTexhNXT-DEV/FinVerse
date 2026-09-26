package com.iortatechnxt.brokerverse.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.util.BusinessText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Requirement references and design notes are removed from business texts. */
class BusinessTextTest {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Days from request to completion of endorsement requests (ADJID.021; layout to confirm,"
            + " OQ42)|Days from request to completion of endorsement requests",
        "Withholding tax certificates tagged to ORs (CMRID.015)|Withholding tax certificates"
            + " tagged to ORs",
        "Incentive runs ending in the period (CMRID.003/005/006)|Incentive runs ending in the"
            + " period",
        "Payments received before booking (CSHID.020, Annex II #14, draft)|Payments received"
            + " before booking",
        "Commission ORs with their invoices (CSHID.023 Annex II #2)|Commission ORs with their"
            + " invoices",
        "Total production per branch (CMRID.014, RMTID.037).|Total production per branch.",
        "Clients due by the date (BRNB.110).|Clients due by the date.",
        "Retired (BRPM.011)|Retired",
        "Journal (for approval)|Journal (for approval)",
      })
  void removesReferences(String text, String expected) {
    assertThat(BusinessText.clean(text)).isEqualTo(expected);
    assertThat(BusinessText.FORBIDDEN.matcher(BusinessText.clean(text)).find()).isFalse();
  }

  @Test
  void keepsNullAndEmptyTexts() {
    assertThat(BusinessText.clean(null)).isNull();
    assertThat(BusinessText.clean("")).isEmpty();
  }
}
