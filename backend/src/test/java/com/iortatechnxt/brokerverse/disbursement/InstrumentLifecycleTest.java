package com.iortatechnxt.brokerverse.disbursement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentLifecycle;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** The instrument status machine of each mode of payment (DIS 2.8, 3.26; design 7.2). */
class InstrumentLifecycleTest {

  private static final Instant NOW = Instant.parse("2026-09-25T02:00:00Z");

  @Test
  void aCheckIsPrintedReleasedAndNegotiatedOrStale() {
    Instrument check = new Instrument(1L, DisbursementMode.CHECK, BigDecimal.TEN, "PHP", 1L);
    assertThat(check.getStatus()).isEqualTo(InstrumentStatus.PENDING);
    assertThatThrownBy(() -> check.move(InstrumentStatus.RELEASED, NOW))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(check.move(InstrumentStatus.PRINTED, NOW)).isEqualTo(InstrumentStatus.PENDING);
    check.move(InstrumentStatus.RELEASED, NOW);
    check.move(InstrumentStatus.NEGOTIATED, NOW);
    assertThat(check.getNegotiatedAt()).isEqualTo(NOW);
    assertThat(InstrumentLifecycle.isFinal(DisbursementMode.CHECK, check.getStatus())).isTrue();
    assertThat(
            InstrumentLifecycle.allows(
                DisbursementMode.CHECK, InstrumentStatus.NEGOTIATED, InstrumentStatus.CANCELLED))
        .isFalse();
    assertThat(
            InstrumentLifecycle.allows(
                DisbursementMode.CHECK, InstrumentStatus.RELEASED, InstrumentStatus.STALE))
        .isTrue();
  }

  @Test
  void eachModeHasItsOwnStatuses() {
    assertThat(InstrumentLifecycle.initial(DisbursementMode.ONLINE_BANKING))
        .isEqualTo(InstrumentStatus.APPROVED);
    assertThat(InstrumentLifecycle.statusesOf(DisbursementMode.CTA))
        .containsExactlyInAnyOrder(
            InstrumentStatus.PENDING, InstrumentStatus.EXTRACTED, InstrumentStatus.CREDITED);
    assertThat(InstrumentLifecycle.statusesOf(DisbursementMode.ATD))
        .contains(InstrumentStatus.EMAILED, InstrumentStatus.DEBITED);
    assertThat(InstrumentLifecycle.statusesOf(DisbursementMode.MC_DD))
        .contains(InstrumentStatus.RECEIVED, InstrumentStatus.RELEASED);
    assertThat(InstrumentLifecycle.isFinal(DisbursementMode.MC_DD, InstrumentStatus.RELEASED))
        .isTrue();
    assertThat(InstrumentLifecycle.isFinal(DisbursementMode.CHECK, InstrumentStatus.RELEASED))
        .isFalse();
    assertThat(InstrumentLifecycle.isPaid(InstrumentStatus.CREDITED)).isTrue();
    assertThat(InstrumentLifecycle.isPaid(InstrumentStatus.PRINTED)).isFalse();
    Instrument tt = new Instrument(2L, DisbursementMode.TT, BigDecimal.ONE, "USD", 1L);
    tt.move(InstrumentStatus.PRINTED, NOW);
    tt.correct(InstrumentStatus.PENDING, NOW);
    assertThat(tt.getStatus()).isEqualTo(InstrumentStatus.PENDING);
    tt.move(InstrumentStatus.CANCELLED, NOW);
    assertThat(tt.getCancelledAt()).isEqualTo(NOW);
  }

  @Test
  void accountNumbersAreMaskedButTheLastFourDigits() {
    assertThat(PayeeAccount.mask("001234567890")).isEqualTo("********7890");
    assertThat(PayeeAccount.mask("1234")).isEqualTo("1234");
    assertThat(PayeeAccount.mask(null)).isNull();
  }
}
