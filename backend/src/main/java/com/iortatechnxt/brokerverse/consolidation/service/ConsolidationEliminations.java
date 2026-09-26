package com.iortatechnxt.brokerverse.consolidation.service;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationGroup;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationLineType;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationLineValues;
import com.iortatechnxt.brokerverse.consolidation.domain.ConsolidationMember;
import com.iortatechnxt.brokerverse.consolidation.domain.IntercompanyRelationship;
import com.iortatechnxt.brokerverse.consolidation.domain.IntercompanyRelationship.Side;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Elimination rule set applied to translated member balances. Every rule produces a balanced set of
 * lines, so the consolidated trial balance stays balanced.
 *
 * <ul>
 *   <li><b>IC_BALANCE</b>: for each active inter-company relationship between members, the
 *       creditor's due-from and the debtor's due-to balances are reversed; a difference (typically
 *       exchange differences between the two books) goes to the translation reserve.
 *   <li><b>INVESTMENT_EQUITY</b>: the parent's investment in a subsidiary is eliminated against the
 *       subsidiary's share capital accounts; the minority share of that capital goes to
 *       non-controlling interest and the remainder to goodwill (negative = gain on bargain
 *       purchase).
 * </ul>
 *
 * <p>Mapped due-from / due-to accounts are assumed to be dedicated to the counterparty (as the
 * relationship master defines one pair of accounts per counterparty).
 */
public final class ConsolidationEliminations {

  /** Inter-company balance rule code. */
  public static final String IC_BALANCE = "IC_BALANCE";

  /** Investment / equity rule code. */
  public static final String INVESTMENT_EQUITY = "INVESTMENT_EQUITY";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final int WORK_SCALE = 10;

  private final ConsolidationGroup group;
  private final Map<String, BigDecimal> balances = new HashMap<>();
  private final UnaryOperator<String> accountNames;
  private final Map<String, AccountClass> classes = new HashMap<>();
  private final List<ConsolidationLineValues> out = new ArrayList<>();

  private ConsolidationEliminations(
      ConsolidationGroup group,
      List<ConsolidationLineValues> translated,
      UnaryOperator<String> accountNames) {
    this.group = group;
    this.accountNames = accountNames;
    for (ConsolidationLineValues l : translated) {
      balances.merge(key(l.companyId(), l.accountCode()), l.amount(), BigDecimal::add);
      classes.putIfAbsent(l.accountCode(), l.accountClass());
    }
  }

  /**
   * Builds the elimination lines.
   *
   * @param group consolidation group
   * @param translated translated lines of all members
   * @param relationships inter-company relationships
   * @param accountNames account code to name (group chart)
   * @return elimination lines
   */
  public static List<ConsolidationLineValues> build(
      ConsolidationGroup group,
      List<ConsolidationLineValues> translated,
      List<IntercompanyRelationship> relationships,
      UnaryOperator<String> accountNames) {
    ConsolidationEliminations e = new ConsolidationEliminations(group, translated, accountNames);
    List<Long> members = group.companyIds();
    for (IntercompanyRelationship rel : relationships) {
      if (rel.isActive()
          && members.contains(rel.getCompanyAId())
          && members.contains(rel.getCompanyBId())) {
        e.intercompany(rel, rel.getCompanyAId(), rel.getCompanyBId());
        e.intercompany(rel, rel.getCompanyBId(), rel.getCompanyAId());
      }
    }
    group.getMembers().forEach(e::investment);
    return List.copyOf(e.out);
  }

  private void intercompany(IntercompanyRelationship rel, Long creditorId, Long debtorId) {
    Side creditor = rel.sideOf(creditorId);
    Side debtor = rel.sideOf(debtorId);
    BigDecimal dueFrom = balance(creditorId, creditor.dueFromAccount());
    BigDecimal dueTo = balance(debtorId, debtor.dueToAccount());
    if (dueFrom.signum() == 0 && dueTo.signum() == 0) {
      return;
    }
    String what = "Eliminate inter-company balance";
    add(IC_BALANCE, creditorId, creditor.dueFromAccount(), dueFrom.negate(), what + " (due-from)");
    add(IC_BALANCE, debtorId, debtor.dueToAccount(), dueTo.negate(), what + " (due-to)");
    add(
        IC_BALANCE,
        null,
        group.getCtaAccount(),
        dueFrom.add(dueTo),
        "Inter-company difference to translation reserve");
  }

  private void investment(ConsolidationMember member) {
    List<String> equity = member.equityAccountCodes();
    if (member.getInvestmentAccount() == null || equity.isEmpty()) {
      return;
    }
    Long parent = group.getParentCompanyId();
    BigDecimal investment = balance(parent, member.getInvestmentAccount());
    BigDecimal capital = BigDecimal.ZERO;
    for (String code : equity) {
      BigDecimal b = balance(member.getCompanyId(), code);
      capital = capital.add(b.negate());
      add(
          INVESTMENT_EQUITY,
          member.getCompanyId(),
          code,
          b.negate(),
          "Eliminate subsidiary capital");
    }
    BigDecimal ownership =
        member.getOwnershipPct().divide(HUNDRED, WORK_SCALE, RoundingMode.HALF_EVEN);
    BigDecimal parentShare = Money.round(capital.multiply(ownership));
    BigDecimal minority = capital.subtract(parentShare);
    add(
        INVESTMENT_EQUITY,
        parent,
        member.getInvestmentAccount(),
        investment.negate(),
        "Eliminate investment in subsidiary");
    add(
        INVESTMENT_EQUITY,
        null,
        group.getNciAccount(),
        minority.negate(),
        "Non-controlling interest in capital");
    add(
        INVESTMENT_EQUITY,
        null,
        group.getGoodwillAccount(),
        investment.subtract(parentShare),
        "Goodwill on consolidation");
  }

  private BigDecimal balance(Long companyId, String accountCode) {
    return balances.getOrDefault(key(companyId, accountCode), Money.zero());
  }

  private void add(
      String rule, Long companyId, String accountCode, BigDecimal amount, String text) {
    if (amount.signum() == 0) {
      return;
    }
    out.add(
        new ConsolidationLineValues(
            ConsolidationLineType.ELIMINATION,
            rule,
            companyId,
            accountCode,
            accountNames.apply(accountCode),
            classes.getOrDefault(accountCode, defaultClass(accountCode)),
            Money.zero(),
            BigDecimal.ONE,
            Money.round(amount),
            text));
  }

  private AccountClass defaultClass(String accountCode) {
    return accountCode.equals(group.getGoodwillAccount())
        ? AccountClass.ASSET
        : AccountClass.EQUITY;
  }

  private static String key(Long companyId, String accountCode) {
    return companyId + "|" + accountCode;
  }
}
