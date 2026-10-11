package com.iortatechnxt.brokerverse.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.common.util.AmountInWords;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AmountInWordsTest {

  @ParameterizedTest
  @CsvSource({
    "0, Zero",
    "7, Seven",
    "15, Fifteen",
    "40, Forty",
    "99, Ninety-Nine",
    "100, One Hundred",
    "1250, One Thousand Two Hundred Fifty",
    "1000000, One Million",
    "2005017, Two Million Five Thousand Seventeen",
    "3000000000, Three Billion"
  })
  void spellsWholeNumbers(long number, String words) {
    assertThat(AmountInWords.words(number)).isEqualTo(words);
  }

  @Test
  void spellsAmountWithCurrencyAndCents() {
    assertThat(AmountInWords.spell(new BigDecimal("-1250.5"), "Philippine Peso"))
        .isEqualTo("Philippine Peso One Thousand Two Hundred Fifty and 50/100 only");
  }

  @Test
  void spellsCentsOnlyAndLargeAmounts() {
    assertThat(AmountInWords.spell(new BigDecimal("0.05"), "USD"))
        .isEqualTo("USD Zero and 05/100 only");
    assertThat(AmountInWords.spell(new BigDecimal("2000013.00"), "PHP"))
        .isEqualTo("PHP Two Million Thirteen and 00/100 only");
  }
}
