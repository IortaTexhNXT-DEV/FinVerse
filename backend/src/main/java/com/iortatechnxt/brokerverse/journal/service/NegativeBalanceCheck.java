package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.domain.NegativeBalancePolicy;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Negative balance control of a journal (FRBS 2.5.4, 2.8.4, 3.6.0b; AQ30): for every account whose
 * {@link NegativeBalancePolicy} is WARN or BLOCK, the base-currency balance after the journal (all
 * branches, as of the value date) is computed; a journal that moves an account to the side opposite
 * its natural side (debit for assets and expenses, credit for the others) gets a finding.
 */
@Component
public class NegativeBalanceCheck {

  private final LedgerQueryService ledger;

  /**
   * Creates the check.
   *
   * @param ledger ledger balances
   */
  public NegativeBalanceCheck(LedgerQueryService ledger) {
    this.ledger = ledger;
  }

  /**
   * Findings of a batch.
   *
   * @param batch journal (not yet posted)
   * @return one finding per account left with a negative balance
   */
  public List<Finding> check(JournalBatch batch) {
    Map<Long, GlAccount> accounts = new LinkedHashMap<>();
    Map<Long, BigDecimal> effect = new LinkedHashMap<>();
    for (JournalLine line : batch.getLines()) {
      GlAccount account = line.getAccount();
      if (account.getNegativeBalancePolicy() == NegativeBalancePolicy.ALLOW) {
        continue;
      }
      BigDecimal signed =
          line.getSide() == BalanceSide.DEBIT
              ? line.getBaseAmount()
              : line.getBaseAmount().negate();
      accounts.putIfAbsent(account.getId(), account);
      effect.merge(account.getId(), signed, BigDecimal::add);
    }
    List<Finding> findings = new ArrayList<>();
    effect.forEach(
        (accountId, change) -> {
          GlAccount account = accounts.get(accountId);
          BigDecimal after =
              ledger
                  .netBalance(batch.getCompanyId(), account.getId(), null, batch.getValueDate())
                  .add(change);
          if (isNegative(account, after) && isNegative(account, change)) {
            findings.add(new Finding(account.getCode(), account.getNegativeBalancePolicy(), after));
          }
        });
    return findings;
  }

  private static boolean isNegative(GlAccount account, BigDecimal netDebit) {
    return account.getAccountClass().normalBalance() == BalanceSide.DEBIT
        ? netDebit.signum() < 0
        : netDebit.signum() > 0;
  }

  /**
   * An account left with a balance on the wrong side.
   *
   * @param accountCode account
   * @param policy WARN or BLOCK
   * @param balanceAfter net debit balance after the journal
   */
  public record Finding(String accountCode, NegativeBalancePolicy policy, BigDecimal balanceAfter) {

    /**
     * Readable message.
     *
     * @return message
     */
    public String message() {
      String side = balanceAfter.signum() < 0 ? "credit" : "debit";
      return "Account "
          + accountCode
          + " would have a negative ("
          + side
          + ") balance of "
          + balanceAfter.abs().toPlainString();
    }
  }
}
