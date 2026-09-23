package com.iortatechnxt.finverse.accounting.service;

import com.iortatechnxt.finverse.accounting.domain.AccountingRule;
import com.iortatechnxt.finverse.accounting.domain.AccountingRuleLine;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Applies a rule to an event: one journal line per rule line with a non-zero amount. Negative
 * amounts (refunds, reserve releases) post to the opposite side with the absolute value.
 */
@Component
public class JournalLineBuilder {

  /**
   * Builds journal lines.
   *
   * @param rule rule
   * @param event event
   * @return lines (never empty)
   */
  public List<JournalLineRequest> build(AccountingRule rule, BusinessEvent event) {
    List<JournalLineRequest> lines = new ArrayList<>();
    for (AccountingRuleLine rl : rule.getLines()) {
      BigDecimal amount = event.amount(rl.getAmountComponent());
      if (amount.signum() == 0) {
        continue;
      }
      lines.add(
          new JournalLineRequest(
              accountFor(rl, event),
              amount.signum() > 0 ? rl.getSide() : rl.getSide().opposite(),
              amount.abs(),
              event.currency(),
              null,
              event.branchId(),
              event.costCenter(),
              event.businessLine(),
              rl.isPartyLine() ? event.partyCode() : null,
              event.reference(),
              rl.getNarration() != null ? rl.getNarration() : event.narration()));
    }
    if (lines.isEmpty()) {
      throw new BusinessRuleException(
          "EMPTY_ACCOUNTING_EVENT",
          "Event " + event.eventType() + " has no amounts to account for");
    }
    return lines;
  }

  private static String accountFor(AccountingRuleLine line, BusinessEvent event) {
    if (!line.isAccountRole()) {
      return line.getAccountCode();
    }
    String role = line.getAccountCode().substring(AccountingRuleLine.ROLE_PREFIX.length());
    String code = event.accounts().get(role);
    if (code == null || code.isBlank()) {
      throw new BusinessRuleException(
          "MISSING_ACCOUNT_ROLE",
          "Event " + event.eventType() + " must supply an account for role " + role);
    }
    return code;
  }
}
