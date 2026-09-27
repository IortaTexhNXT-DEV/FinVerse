package com.iortatechnxt.brokerverse.consolidation.service;

import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.consolidation.domain.IntercompanyRelationship;
import com.iortatechnxt.brokerverse.consolidation.domain.IntercompanyRelationship.Side;
import com.iortatechnxt.brokerverse.ledger.service.AccountBalance;
import com.iortatechnxt.brokerverse.ledger.service.BalanceQuery;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inter-company reconciliation: compares each company's due-from balance with the counterparty's
 * due-to balance, per currency, in transaction currency (so exchange rate differences between the
 * two books do not show as mismatches).
 */
@Service
@Transactional(readOnly = true)
public class IntercompanyReconciliationService {

  private final IntercompanyService intercompany;
  private final LedgerQueryService ledger;
  private final ChartOfAccountsService accounts;

  /**
   * Creates the service.
   *
   * @param intercompany relationship source
   * @param ledger ledger read model
   * @param accounts chart of accounts
   */
  public IntercompanyReconciliationService(
      IntercompanyService intercompany,
      LedgerQueryService ledger,
      ChartOfAccountsService accounts) {
    this.intercompany = intercompany;
    this.ledger = ledger;
    this.accounts = accounts;
  }

  /**
   * Reconciles all active relationships (optionally those of one company) as of a date.
   *
   * @param companyId company or null for all
   * @param asOf as-of date
   * @return one line per relationship, direction and currency
   */
  public List<ReconciliationLine> reconcile(Long companyId, LocalDate asOf) {
    Map<Long, Map<String, Map<String, AccountBalance>>> cache = new HashMap<>();
    List<ReconciliationLine> lines = new ArrayList<>();
    for (IntercompanyRelationship rel : intercompany.relationships(companyId)) {
      if (rel.isActive()) {
        lines.addAll(direction(rel, rel.getCompanyAId(), rel.getCompanyBId(), asOf, cache));
        lines.addAll(direction(rel, rel.getCompanyBId(), rel.getCompanyAId(), asOf, cache));
      }
    }
    return lines;
  }

  private List<ReconciliationLine> direction(
      IntercompanyRelationship rel,
      Long creditorId,
      Long debtorId,
      LocalDate asOf,
      Map<Long, Map<String, Map<String, AccountBalance>>> cache) {
    Side creditor = rel.sideOf(creditorId);
    Side debtor = rel.sideOf(debtorId);
    Map<String, AccountBalance> dueFrom =
        balances(creditorId, asOf, cache).getOrDefault(creditor.dueFromAccount(), Map.of());
    Map<String, AccountBalance> dueTo =
        balances(debtorId, asOf, cache).getOrDefault(debtor.dueToAccount(), Map.of());
    List<ReconciliationLine> lines = new ArrayList<>();
    for (String currency : new TreeSet<>(union(dueFrom, dueTo))) {
      AccountBalance from = dueFrom.get(currency);
      AccountBalance to = dueTo.get(currency);
      lines.add(
          new ReconciliationLine(
              rel.getId(),
              creditorId,
              debtorId,
              creditor.dueFromAccount(),
              debtor.dueToAccount(),
              currency,
              from == null ? Money.zero() : from.netFc(),
              to == null ? Money.zero() : to.netFc().negate(),
              from == null ? Money.zero() : from.netBase(),
              to == null ? Money.zero() : to.netBase().negate()));
    }
    return lines;
  }

  private static List<String> union(Map<String, ?> a, Map<String, ?> b) {
    List<String> keys = new ArrayList<>(a.keySet());
    keys.addAll(b.keySet());
    return keys;
  }

  /** Balances of one company by account code and currency (loaded once per company). */
  private Map<String, Map<String, AccountBalance>> balances(
      Long companyId, LocalDate asOf, Map<Long, Map<String, Map<String, AccountBalance>>> cache) {
    return cache.computeIfAbsent(
        companyId,
        id -> {
          Map<Long, String> codes = new HashMap<>();
          for (GlAccount a : accounts.list(id)) {
            codes.put(a.getId(), a.getCode());
          }
          Map<String, Map<String, AccountBalance>> out = new HashMap<>();
          for (AccountBalance b : ledger.balances(new BalanceQuery(id, null, null, asOf, true))) {
            out.computeIfAbsent(codes.get(b.accountId()), k -> new HashMap<>())
                .put(b.currency(), b);
          }
          return out;
        });
  }

  /**
   * Due-from against due-to of one direction and currency.
   *
   * @param relationshipId relationship
   * @param creditorCompanyId company holding the receivable
   * @param debtorCompanyId company holding the payable
   * @param dueFromAccount creditor's due-from account
   * @param dueToAccount debtor's due-to account
   * @param currency transaction currency
   * @param dueFromFc receivable in transaction currency (debit positive)
   * @param dueToFc payable in transaction currency (credit positive)
   * @param dueFromBase receivable in the creditor's base currency
   * @param dueToBase payable in the debtor's base currency
   */
  public record ReconciliationLine(
      Long relationshipId,
      Long creditorCompanyId,
      Long debtorCompanyId,
      String dueFromAccount,
      String dueToAccount,
      String currency,
      BigDecimal dueFromFc,
      BigDecimal dueToFc,
      BigDecimal dueFromBase,
      BigDecimal dueToBase) {

    /**
     * Difference in transaction currency.
     *
     * @return due-from minus due-to
     */
    public BigDecimal difference() {
      return dueFromFc.subtract(dueToFc);
    }

    /**
     * Whether both sides agree.
     *
     * @return true when the difference is zero
     */
    public boolean matched() {
      return difference().signum() == 0;
    }
  }
}
