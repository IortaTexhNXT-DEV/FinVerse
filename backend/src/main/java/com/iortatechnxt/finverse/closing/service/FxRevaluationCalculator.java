package com.iortatechnxt.finverse.closing.service;

import com.iortatechnxt.finverse.closing.domain.RevaluationItem;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.ledger.service.AccountBalance;
import com.iortatechnxt.finverse.ledger.service.BalanceQuery;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Computes the revaluation of foreign currency balances as of a date.
 *
 * <p>Scope: postable balance sheet accounts flagged {@code revaluationRequired}; every branch and
 * every currency other than the company base currency. Revalued base = FC balance × CLOSING rate;
 * the difference to the booked base balance is the unrealized exchange gain or loss.
 */
@Component
public class FxRevaluationCalculator {

  private final LedgerQueryService ledger;
  private final ChartOfAccountsService accounts;
  private final OrganizationService organization;
  private final ClosingRates rates;

  /**
   * Creates the calculator.
   *
   * @param ledger ledger read model
   * @param accounts chart of accounts
   * @param organization organization service
   * @param rates closing rate lookup
   */
  public FxRevaluationCalculator(
      LedgerQueryService ledger,
      ChartOfAccountsService accounts,
      OrganizationService organization,
      ClosingRates rates) {
    this.ledger = ledger;
    this.accounts = accounts;
    this.organization = organization;
    this.rates = rates;
  }

  /**
   * Revalues all foreign currency balances of revaluation accounts.
   *
   * @param companyId company
   * @param asOf revaluation date
   * @return revalued items and currencies without a closing rate
   */
  public Result calculate(Long companyId, LocalDate asOf) {
    Company company = organization.getCompany(companyId);
    Map<Long, GlAccount> scope =
        accounts.list(companyId).stream()
            .filter(a -> a.isRevaluationRequired() && a.isPostable())
            .filter(a -> a.getAccountClass().isBalanceSheet())
            .collect(Collectors.toMap(GlAccount::getId, Function.identity()));
    List<RevaluationItem> items = new ArrayList<>();
    Set<String> missing = new TreeSet<>();
    if (scope.isEmpty()) {
      return new Result(items, missing);
    }
    Map<String, Optional<BigDecimal>> closing = new HashMap<>();
    for (Branch branch : organization.listBranches(companyId)) {
      for (AccountBalance b :
          ledger.balances(new BalanceQuery(companyId, branch.getId(), null, asOf, true))) {
        GlAccount account = scope.get(b.accountId());
        if (account == null || company.getBaseCurrency().equals(b.currency()) || !hasBalance(b)) {
          continue;
        }
        Optional<BigDecimal> rate =
            closing.computeIfAbsent(
                b.currency(), c -> rates.closing(company.getBaseCurrency(), c, asOf));
        rate.ifPresentOrElse(
            r -> items.add(item(branch.getId(), account, b, r)), () -> missing.add(b.currency()));
      }
    }
    items.sort(
        Comparator.comparing(RevaluationItem::accountCode)
            .thenComparing(RevaluationItem::branchId)
            .thenComparing(RevaluationItem::currency));
    return new Result(items, missing);
  }

  private static boolean hasBalance(AccountBalance b) {
    return b.netFc().signum() != 0 || b.netBase().signum() != 0;
  }

  private static RevaluationItem item(
      Long branchId, GlAccount account, AccountBalance b, BigDecimal rate) {
    BigDecimal fc = Money.round(b.netFc());
    return new RevaluationItem(
        branchId,
        account.getId(),
        account.getCode(),
        account.getName(),
        b.currency(),
        fc,
        Money.round(b.netBase()),
        rate,
        Money.convert(fc, rate));
  }

  /**
   * Revaluation result.
   *
   * @param items revalued balances
   * @param missingRates currencies with balances but no CLOSING rate (not revalued)
   */
  public record Result(List<RevaluationItem> items, Set<String> missingRates) {

    /** Canonical constructor copying collections. */
    public Result {
      items = List.copyOf(items);
      missingRates = Set.copyOf(missingRates);
    }

    /** Fails when a closing rate is missing (spec: missing exchange rate prevents GL posting). */
    public void requireRates() {
      if (!missingRates.isEmpty()) {
        throw new BusinessRuleException(
            "RATE_NOT_FOUND",
            "No CLOSING rate for " + String.join(", ", new TreeSet<>(missingRates)));
      }
    }

    /**
     * Whether any balance needs a revaluation journal.
     *
     * @return true when a postable item has a non-zero difference
     */
    public boolean needsPosting() {
      return items.stream().anyMatch(i -> i.postable() && i.difference().signum() != 0);
    }
  }
}
