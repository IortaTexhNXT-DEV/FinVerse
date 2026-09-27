package com.iortatechnxt.brokerverse.alert.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.ledger.service.AccountBalance;
import com.iortatechnxt.brokerverse.ledger.service.BalanceQuery;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Ledger based exception checks per company: UNBALANCED_TB (debits and credits differ),
 * NEGATIVE_CASH_BALANCE (cash or bank account in credit) and SUSPENSE_BALANCE (suspense account
 * above the threshold amount).
 */
@Component
public class LedgerAlertCheck implements AlertCheck {

  /** Unbalanced trial balance. */
  public static final String UNBALANCED_TB = "UNBALANCED_TB";

  /** Negative cash balance. */
  public static final String NEGATIVE_CASH = "NEGATIVE_CASH_BALANCE";

  /** Suspense account balance. */
  public static final String SUSPENSE = "SUSPENSE_BALANCE";

  private static final String ACCOUNT = "GlAccount";

  private final OrganizationService organization;
  private final LedgerQueryService ledger;
  private final ChartOfAccountsService accounts;
  private final SystemParameterService parameters;
  private final AlertService alerts;

  /**
   * Creates the check.
   *
   * @param organization companies
   * @param ledger ledger balances
   * @param accounts chart of accounts
   * @param parameters business parameters (suspense accounts)
   * @param alerts alert service (thresholds)
   */
  public LedgerAlertCheck(
      OrganizationService organization,
      LedgerQueryService ledger,
      ChartOfAccountsService accounts,
      SystemParameterService parameters,
      AlertService alerts) {
    this.organization = organization;
    this.ledger = ledger;
    this.accounts = accounts;
    this.parameters = parameters;
    this.alerts = alerts;
  }

  @Override
  public List<AlertSignal> evaluate(LocalDate asOf) {
    List<AlertSignal> signals = new ArrayList<>();
    List<String> suspenseCodes = parameters.items(SystemParameterService.SUSPENSE_ACCOUNT_CODES);
    BigDecimal suspenseThreshold =
        alerts
            .activeCode(SUSPENSE)
            .map(c -> c.getThresholdAmount() == null ? BigDecimal.ZERO : c.getThresholdAmount())
            .orElse(null);
    for (Company company : organization.listCompanies()) {
      List<AccountBalance> balances = ledger.balances(BalanceQuery.asOf(company.getId(), asOf));
      Map<Long, GlAccount> chart =
          accounts.list(company.getId()).stream()
              .collect(Collectors.toMap(GlAccount::getId, Function.identity()));
      checkTrialBalance(company, balances, asOf, signals);
      for (AccountBalance balance : balances) {
        GlAccount account = chart.get(balance.accountId());
        if (account != null) {
          checkCash(company, account, balance.netBase(), signals);
          checkSuspense(
              company, account, balance.netBase(), suspenseCodes, suspenseThreshold, signals);
        }
      }
    }
    return signals;
  }

  private static void checkTrialBalance(
      Company company, List<AccountBalance> balances, LocalDate asOf, List<AlertSignal> signals) {
    BigDecimal difference =
        balances.stream().map(AccountBalance::netBase).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (difference.signum() != 0) {
      signals.add(
          new AlertSignal(
              UNBALANCED_TB,
              new AlertFacts(
                  company.getId(),
                  null,
                  "Company",
                  company.getCode(),
                  "Trial balance of "
                      + company.getCode()
                      + " as of "
                      + asOf
                      + " is out of balance by "
                      + difference,
                  difference.abs(),
                  UNBALANCED_TB + ":" + company.getId())));
    }
  }

  private static void checkCash(
      Company company, GlAccount account, BigDecimal net, List<AlertSignal> signals) {
    boolean cash =
        account.getAccountClass() == AccountClass.ASSET
            && account.getCategory() != null
            && account.getCategory().isBankCategory();
    if (cash && net.signum() < 0) {
      signals.add(
          signal(
              NEGATIVE_CASH,
              company,
              account,
              "Cash/bank account "
                  + account.getCode()
                  + " "
                  + account.getName()
                  + " has a credit balance of "
                  + net.abs(),
              net));
    }
  }

  private static void checkSuspense(
      Company company,
      GlAccount account,
      BigDecimal net,
      List<String> suspenseCodes,
      BigDecimal threshold,
      List<AlertSignal> signals) {
    boolean suspense =
        suspenseCodes.isEmpty()
            ? account.getName().toLowerCase(Locale.ROOT).contains("suspense")
            : suspenseCodes.contains(account.getCode());
    if (threshold != null && suspense && net.abs().compareTo(threshold) > 0) {
      signals.add(
          signal(
              SUSPENSE,
              company,
              account,
              "Suspense account "
                  + account.getCode()
                  + " carries a balance of "
                  + net
                  + " that must be cleared",
              net));
    }
  }

  private static AlertSignal signal(
      String code, Company company, GlAccount account, String message, BigDecimal net) {
    return new AlertSignal(
        code,
        new AlertFacts(
            company.getId(),
            null,
            ACCOUNT,
            account.getCode(),
            message,
            net.abs(),
            code + ":" + company.getId() + ":" + account.getCode()));
  }
}
