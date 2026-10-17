package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.opsledger.service.port.EarlyIncentiveRules;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRule;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.IncentiveBasis;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Remittance's early-remittance incentive rules offered to production reconciliation (PRCID.028,
 * RMTID.023): the rule of {@code rem_incentive_rule} that covers the invoice's insurer, product
 * line and segment on its booking date (inception when not booked), with its rate, window and
 * basis. Replaces the "no rule" default of {@code OpsPortDefaults}; the rates themselves stay
 * parked with BDOI (OQ23).
 */
@Service
public class RemittanceEarlyIncentiveRules implements EarlyIncentiveRules {

  private final IncentiveRuleService rules;

  /**
   * Creates the adapter.
   *
   * @param rules incentive rules
   */
  public RemittanceEarlyIncentiveRules(IncentiveRuleService rules) {
    this.rules = rules;
  }

  @Override
  public Optional<Terms> termsFor(Long companyId, Subject subject) {
    LocalDate on = subject.bookingDate() != null ? subject.bookingDate() : subject.inceptionDate();
    if (on == null || subject.insurerCode() == null) {
      return Optional.empty();
    }
    return rules
        .ruleFor(
            companyId,
            new IncentiveRuleService.Cover(
                subject.insurerCode(), subject.productLine(), subject.segment()),
            on)
        .map(RemittanceEarlyIncentiveRules::terms);
  }

  private static Terms terms(EarlyIncentiveRule rule) {
    return new Terms(
        rule.getRate(),
        rule.getWindowDays(),
        rule.getBasis() == IncentiveBasis.BOOKING ? Basis.BOOKING : Basis.INCEPTION,
        "REM-RULE-" + rule.getId());
  }
}
