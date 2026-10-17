package com.iortatechnxt.brokerverse.renewal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import org.junit.jupiter.api.Test;

/** The audit trail of a disposition reads in words: the disposition, the reason and who gave it. */
class DispositionSummaryTest {

  @Test
  void theDispositionItsReasonAndItsSourceAreNamed() {
    CurrentDisposition d =
        new CurrentDisposition(
            RenewalDisposition.NOT_FOR_RENEWAL, "UNIT_SOLD", DispositionSource.USER, null, null);
    assertThat(RenewalDispositions.summary(d, null))
        .isEqualTo("Disposition Not for Renewal (Unit Sold), by User");
  }

  @Test
  void theMatrixVersionIsGivenInWords() {
    CurrentDisposition d =
        new CurrentDisposition(
            RenewalDisposition.FOR_RENEWAL, null, DispositionSource.MATRIX, null, null);
    assertThat(RenewalDispositions.summary(d, 3))
        .isEqualTo("Disposition For Renewal, by Decision matrix, decision matrix version 3")
        .doesNotContain("FOR_RENEWAL")
        .doesNotContain("MATRIX");
  }
}
