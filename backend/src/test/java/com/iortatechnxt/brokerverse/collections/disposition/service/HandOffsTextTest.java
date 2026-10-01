package com.iortatechnxt.brokerverse.collections.disposition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.OpsAction;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The refusal of a BIR 2307 hand-off names the two paths in words, never by their codes. */
class HandOffsTextTest {

  @Test
  void anUnknownPathIsRefusedInWords() {
    assertThatThrownBy(
            () ->
                HandOffs.validate(
                    OpsAction.CWT2307_REVERSAL, Map.of("path", "X"), LocalDate.of(2026, 10, 1)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(HandOffs.CWT_PATH)
        .hasMessageContaining("certificate received or paid in cash")
        .hasMessageNotContaining("CERTIFICATE");
  }
}
