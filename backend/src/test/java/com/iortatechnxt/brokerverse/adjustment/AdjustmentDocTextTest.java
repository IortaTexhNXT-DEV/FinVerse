package com.iortatechnxt.brokerverse.adjustment;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.adjustment.service.DocText;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * The endorsement and validation slips show statuses and premium components by name, as the screens
 * do, never their codes.
 */
class AdjustmentDocTextTest {

  @Test
  void statusesReadAsLabels() {
    assertThat(DocText.label(PaymentStatus.PAID)).isEqualTo("Paid");
    assertThat(DocText.label(PaymentStatus.NOT_APPLICABLE)).isEqualTo("Not Applicable");
    assertThat(DocText.label(RemittanceStatus.WITH_OUTSTANDING_BALANCE))
        .isEqualTo("With Outstanding Balance");
    assertThat(DocText.label(null)).isEqualTo(DocText.NONE);
    assertThat(DisplayFormat.label("QS_SENT")).isEqualTo("QS Sent");
  }

  @Test
  void everyComponentHasAName() {
    assertThat(LedgerComponent.PREMIUM_TAX_VAT.label()).isEqualTo("Premium Tax / VAT");
    assertThat(LedgerComponent.BASIC.label()).isEqualTo("Basic Premium");
    assertThat(Arrays.stream(LedgerComponent.values()).map(LedgerComponent::label))
        .noneMatch(l -> l.contains("_"));
  }
}
