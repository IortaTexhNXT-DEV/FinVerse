package com.iortatechnxt.brokerverse.disbursement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** A payee request says what it is for in words, with the amount formatted. */
class PayeeRequestDetailsTest {

  @Test
  void theDetailsReadInWords() {
    assertThat(
            RequestIntakeService.payeeRequestDetails("SERVICE_FEE", "PHP", new BigDecimal("18450")))
        .isEqualTo("Service fee, PHP 18,450.00");
  }
}
