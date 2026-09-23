package com.iortatechnxt.finverse.accounting.service;

import com.iortatechnxt.finverse.accounting.domain.AccountingRule;
import com.iortatechnxt.finverse.accounting.domain.AccountingRuleRepository;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import java.util.Comparator;
import org.springframework.stereotype.Component;

/** Selects the accounting rule for an event: most specific matching rule, then priority. */
@Component
public class RuleResolver {

  private final AccountingRuleRepository rules;

  /**
   * Creates the resolver.
   *
   * @param rules rule repository
   */
  public RuleResolver(AccountingRuleRepository rules) {
    this.rules = rules;
  }

  /**
   * Resolves the rule for an event.
   *
   * @param event event
   * @return rule
   */
  public AccountingRule resolve(BusinessEvent event) {
    return rules.findByCompanyIdAndEventType(event.companyId(), event.eventType()).stream()
        .filter(r -> r.matches(event.businessLine(), event.currency(), event.valueDate()))
        .min(
            Comparator.comparingInt(AccountingRule::specificity)
                .reversed()
                .thenComparingInt(AccountingRule::getPriority))
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "NO_ACCOUNTING_RULE",
                    "No active accounting rule for event "
                        + event.eventType()
                        + " (line of business "
                        + event.businessLine()
                        + ", currency "
                        + event.currency()
                        + ")"));
  }
}
