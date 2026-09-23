package com.iortatechnxt.finverse.consolidation.service;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationLineType;
import com.iortatechnxt.finverse.consolidation.domain.ConsolidationLineValues;
import com.iortatechnxt.finverse.ledger.service.AccountBalance;
import com.iortatechnxt.finverse.ledger.service.BalanceQuery;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.period.service.PeriodService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Translates a member company's trial balance into the consolidation currency.
 *
 * <p>Assets, liabilities and equity at the closing rate, income and expense at the average rate;
 * the currency translation adjustment (CTA) that results is booked to the group's translation
 * reserve so the translated trial balance still balances. Memorandum accounts are excluded.
 */
@Service
@Transactional(readOnly = true)
public class TranslationService {

  private final LedgerQueryService ledger;
  private final ChartOfAccountsService accounts;
  private final OrganizationService organization;
  private final PeriodService periods;
  private final TranslationRates rates;

  /**
   * Creates the service.
   *
   * @param ledger ledger read model
   * @param accounts chart of accounts
   * @param organization organization service
   * @param periods period service
   * @param rates translation rates
   */
  public TranslationService(
      LedgerQueryService ledger,
      ChartOfAccountsService accounts,
      OrganizationService organization,
      PeriodService periods,
      TranslationRates rates) {
    this.ledger = ledger;
    this.accounts = accounts;
    this.organization = organization;
    this.periods = periods;
    this.rates = rates;
  }

  /**
   * Translates one member.
   *
   * @param companyId member company
   * @param groupCurrency consolidation currency
   * @param ctaAccount translation reserve account code
   * @param asOf as-of date
   * @return translated lines (TRANSLATED and, when not zero, one CTA line)
   */
  public List<ConsolidationLineValues> translate(
      Long companyId, String groupCurrency, String ctaAccount, LocalDate asOf) {
    Company company = organization.requireActiveCompany(companyId);
    LocalDate yearStart = periods.yearContaining(companyId, asOf).getStartDate();
    List<AccountBalance> balances = ledger.balances(BalanceQuery.asOf(companyId, asOf));
    BigDecimal check =
        balances.stream().map(AccountBalance::netBase).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (check.signum() != 0) {
      throw new BusinessRuleException(
          "CONSOLIDATION_TB_UNBALANCED",
          "Trial balance of " + company.getCode() + " is out of balance by " + check);
    }
    String currency = company.getBaseCurrency();
    BigDecimal closing = rates.closing(groupCurrency, currency, asOf);
    BigDecimal average = rates.average(groupCurrency, currency, yearStart, asOf);
    Map<Long, GlAccount> byId =
        accounts.list(companyId).stream()
            .collect(Collectors.toMap(GlAccount::getId, Function.identity()));
    List<ConsolidationLineValues> lines = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    for (AccountBalance b : balances) {
      GlAccount a = byId.get(b.accountId());
      if (b.netBase().signum() != 0 && a.getAccountClass() != AccountClass.MEMORANDUM) {
        boolean balanceSheet = a.getAccountClass().isBalanceSheet();
        ConsolidationLineValues line =
            translated(company, a, b.netBase(), balanceSheet ? closing : average, balanceSheet);
        total = total.add(line.amount());
        lines.add(line);
      }
    }
    lines.sort(Comparator.comparing(ConsolidationLineValues::accountCode));
    if (total.signum() != 0) {
      lines.add(
          new ConsolidationLineValues(
              ConsolidationLineType.CTA,
              null,
              companyId,
              ctaAccount,
              "Currency translation adjustment",
              AccountClass.EQUITY,
              Money.zero(),
              BigDecimal.ONE,
              total.negate(),
              company.getCode() + " translation difference " + currency + "->" + groupCurrency));
    }
    return lines;
  }

  private static ConsolidationLineValues translated(
      Company company, GlAccount a, BigDecimal local, BigDecimal rate, boolean closing) {
    return new ConsolidationLineValues(
        ConsolidationLineType.TRANSLATED,
        null,
        company.getId(),
        a.getCode(),
        a.getName(),
        a.getAccountClass(),
        Money.round(local),
        rate,
        Money.convert(local, rate),
        company.getCode() + (closing ? " at closing rate" : " at average rate"));
  }
}
