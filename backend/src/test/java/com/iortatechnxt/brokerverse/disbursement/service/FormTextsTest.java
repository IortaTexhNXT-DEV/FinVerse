package com.iortatechnxt.brokerverse.disbursement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** The Disbursement documents print dates as dd-MMM-yyyy and codes in words. */
class FormTextsTest {

  @Test
  void datesReadAsDayMonthYear() {
    assertThat(FormTexts.text(LocalDate.of(2026, 10, 3))).isEqualTo("03-Oct-2026");
    assertThat(FormTexts.text(null)).isEqualTo(FormTexts.DASH);
  }

  @Test
  void codesReadInWords() {
    assertThat(FormTexts.words("SUPPLIER")).isEqualTo("Supplier");
    assertThat(FormTexts.words("RULE")).isEqualTo("Rule");
    assertThat(FormTexts.words(null)).isEqualTo(FormTexts.DASH);
  }

  @Test
  void modesReadAsTheBankNamesThem() {
    assertThat(FormTexts.mode(DisbursementMode.CTA)).isEqualTo("Credit to account");
    assertThat(FormTexts.mode(DisbursementMode.MC_DD)).isEqualTo("Manager's check / demand draft");
    assertThat(FormTexts.mode(DisbursementMode.CHECK)).isEqualTo("Check");
  }
}
