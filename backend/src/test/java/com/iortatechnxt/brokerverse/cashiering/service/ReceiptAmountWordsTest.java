package com.iortatechnxt.brokerverse.cashiering.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** The paid amount in words of the printed AR and OR (FRS.CSH.02.06.02). */
class ReceiptAmountWordsTest {

  @Test
  void writesPesosAndCentavosAsOnTheSampleAr() {
    assertThat(ReceiptAmountWords.of(new BigDecimal("5541001.26"), "PHP"))
        .isEqualTo(
            "Five Million Five Hundred Forty One Thousand One Pesos and Twenty Six Centavos");
  }

  @Test
  void writesRoundAmountsWithoutCentavosAndOtherCurrencies() {
    assertThat(ReceiptAmountWords.of(new BigDecimal("1000000000.00"), "PHP"))
        .isEqualTo("One Billion Pesos");
    assertThat(ReceiptAmountWords.of(new BigDecimal("0.50"), "PHP"))
        .isEqualTo("Zero Pesos and Fifty Centavos");
    assertThat(ReceiptAmountWords.of(new BigDecimal("1215.10"), "USD"))
        .isEqualTo("One Thousand Two Hundred Fifteen US Dollars and Ten Cents");
    assertThat(ReceiptAmountWords.whole(110_019)).isEqualTo("One Hundred Ten Thousand Nineteen");
  }
}
