package com.iortatechnxt.brokerverse.brokerclaims.setup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.report.LossReports;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService.SettlementAttributes;
import com.iortatechnxt.brokerverse.brokerclaims.setup.service.AttributeSetupService.StatusAttributes;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Validation of the status and settlement type attributes (FR-CL-040/043) and the loss ratio. */
class AttributeRulesTest {

  @Test
  void aStatusNeedsAPhaseOtherThanClosedAndAWaitingParty() {
    Map<String, String> values =
        AttributeRules.status(new StatusAttributes("temp_closed", "claimant", " 5 ", true));
    assertThat(values)
        .containsEntry("phase", "TEMP_CLOSED")
        .containsEntry("waiting_on", "CLAIMANT")
        .containsEntry("follow_up_days", "5")
        .containsEntry("awaiting_premium_remittance", "true");
    assertThat(AttributeRules.status(new StatusAttributes("NEW", "BDOI", null, false)))
        .containsEntry("follow_up_days", null)
        .containsEntry("awaiting_premium_remittance", null);
    assertThatThrownBy(
            () -> AttributeRules.status(new StatusAttributes("CLOSED", "INSURER", null, false)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Set the phase of the status");
    assertThatThrownBy(
            () -> AttributeRules.status(new StatusAttributes("NEW", "NOBODY", null, false)))
        .hasMessage("Select the party the claim waits on");
  }

  @Test
  void followUpDaysAreAWholeNumberFromOneTo365() {
    assertThat(AttributeRules.followUpDays("365")).isEqualTo("365");
    assertThat(AttributeRules.followUpDays("1")).isEqualTo("1");
    for (String bad : new String[] {"366", "0", "2.5", "-1", "abc", "1000"}) {
      assertThatThrownBy(() -> AttributeRules.followUpDays(bad))
          .as(bad)
          .hasMessage("Enter a whole number of days");
    }
  }

  @Test
  void aSettlementTypeNeedsItsOutcome() {
    assertThat(AttributeRules.settlement(new SettlementAttributes("settled", true, false)))
        .containsEntry("outcome", "SETTLED")
        .containsEntry("closes_claim", "true")
        .containsEntry("requires_settlement_amount", "false");
    assertThatThrownBy(
            () -> AttributeRules.settlement(new SettlementAttributes("PAID", true, true)))
        .hasMessage("Set the outcome of the settlement type");
  }

  @Test
  void theLossRatioIsLossesOverPremiumInPercent() {
    assertThat(LossReports.Ratio.ratio(new BigDecimal("25000"), new BigDecimal("50000")))
        .isEqualByComparingTo("50.00");
    assertThat(LossReports.Ratio.ratio(BigDecimal.ONE, BigDecimal.ZERO)).isNull();
    assertThat(LossReports.Ratio.ratio(null, BigDecimal.TEN)).isNull();
  }
}
