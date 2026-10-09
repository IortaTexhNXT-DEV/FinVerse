package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccountRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the Create AR / OR screen needs (FRS.CSH.02.01.02 to 02.01.05): the settings of the record,
 * the bank accounts with the default of each currency, the receipting branches, the rate of the day
 * and the account search with BDOI's account table (premium and commission receivable, total
 * invoice, total paid, outstanding balance and the PR 2307 of an account with the BIR 2307 tag).
 */
@Service
@Transactional(readOnly = true)
public class RecordFormService {

  private static final int MAX_ACCOUNTS = 20;
  private static final String SEARCH =
      "select invoice_no from ops_invoice where company_id = ? and (lower(invoice_no) like ?"
          + " or lower(coalesce(arn, '')) like ? or lower(coalesce(policy_no, '')) like ?"
          + " or lower(coalesce(pn_nos, '')) like ?) order by invoice_no limit "
          + MAX_ACCOUNTS;

  private final ReceiptRecordService records;
  private final CashieringDecisions decisions;
  private final CashieringSettings settings;
  private final CashBankAccountRepository bankAccounts;
  private final PaymentMatcher matcher;
  private final ApplicationService applications;
  private final AccountQueryService accounts;
  private final RecordFacts facts;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records receipting branches
   * @param decisions settings of BDOI's decisions
   * @param settings rates and branches
   * @param bankAccounts the list Post to Bank Account
   * @param matcher invoices of a reference
   * @param applications balances of an invoice (PR 2307)
   * @param accounts pre-booked accounts
   * @param facts working days
   * @param jdbc account search
   * @param clock clock
   */
  public RecordFormService(
      ReceiptRecordService records,
      CashieringDecisions decisions,
      CashieringSettings settings,
      CashBankAccountRepository bankAccounts,
      PaymentMatcher matcher,
      ApplicationService applications,
      AccountQueryService accounts,
      RecordFacts facts,
      JdbcTemplate jdbc,
      Clock clock) {
    this.records = records;
    this.decisions = decisions;
    this.settings = settings;
    this.bankAccounts = bankAccounts;
    this.matcher = matcher;
    this.applications = applications;
    this.accounts = accounts;
    this.facts = facts;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * The settings and lists of the creation screen.
   *
   * @param companyId company
   * @return settings
   */
  public FormSettings settings(Long companyId) {
    LocalDate today = BusinessClock.today(clock);
    return new FormSettings(
        decisions.postingStep(ReceiptKind.AR),
        decisions.postingStep(ReceiptKind.OR),
        decisions.remarksRequired(),
        decisions.maxPaidAmount(),
        decisions.checkDateRule()
            ? facts.workingDaysBefore(companyId, decisions.checkHoldingDays())
            : null,
        decisions.checkHoldingDays(),
        bankAccounts.findByCompanyIdOrderByCurrencyAscNameAsc(companyId).stream()
            .filter(CashBankAccount::isActive)
            .map(
                b ->
                    new BankOption(
                        b.getCode(), b.getName(), b.getCurrency(), b.isDefaultForCurrency()))
            .toList(),
        branches(records.receiptingBranches(companyId, ReceiptKind.AR)),
        branches(records.receiptingBranches(companyId, ReceiptKind.OR)),
        settings.bookRate(companyId, "USD", today),
        today);
  }

  private static List<BranchOption> branches(List<Branch> list) {
    return list.stream().map(b -> new BranchOption(b.getId(), b.getCode(), b.getName())).toList();
  }

  /**
   * Accounts whose account, invoice, ARN, policy or PN number contains the text, in any case, with
   * BDOI's account table; an account not booked yet is listed as pre-booked.
   *
   * @param companyId company
   * @param text part of a number
   * @return accounts, at most 20
   */
  public List<AccountRow> accounts(Long companyId, String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    List<AccountRow> rows = new ArrayList<>();
    for (String no : jdbc.queryForList(SEARCH, String.class, companyId, like, like, like, like)) {
      matcher.invoices(companyId, no).stream()
          .filter(i -> i.getInvoiceNo().equals(no))
          .findFirst()
          .ifPresent(i -> rows.add(row(i)));
    }
    for (Account a : accounts.preBooked(companyId, text.strip())) {
      rows.add(AccountRow.prebooked(a.getArn(), a.getClientName()));
    }
    return rows;
  }

  /**
   * The account table row of an invoice (FRS.CSH.02.01.03).
   *
   * @param invoice invoice with its components
   * @return row
   */
  AccountRow row(OpsInvoice invoice) {
    BigDecimal total = BigDecimal.ZERO;
    for (OpsInvoiceComponent c : invoice.getComponents()) {
      if (c.getComponent().isPremiumReceivable()) {
        total = total.add(c.getBooked()).add(c.getAdjusted());
      }
    }
    BigDecimal outstanding = invoice.premiumBalance();
    BigDecimal pr2307 = invoice.isCwtFlag() ? applications.balancesOf(invoice).withheld() : null;
    return new AccountRow(
        invoice.getInvoiceNo(),
        invoice.getArn(),
        invoice.getPolicyNo(),
        invoice.getClientCode(),
        invoice.getAssuredName(),
        invoice.getInsurerCode(),
        invoice.getCurrency(),
        total,
        invoice.getCommission().add(invoice.getVatOnCommission()),
        total,
        total.subtract(outstanding),
        outstanding,
        invoice.isCwtFlag(),
        pr2307,
        false);
  }

  /**
   * Settings of the creation screen.
   *
   * @param arPostingStep ARs are issued at posting (C1)
   * @param orPostingStep ORs are issued at posting (C1)
   * @param remarksRequired remarks required (C25)
   * @param maxAmount highest paid amount
   * @param latestCheckDate latest check date accepted, null when the rule is off (C7)
   * @param holdingDays working days of the holding period
   * @param bankAccounts the list Post to Bank Account
   * @param arBranches receipting branches of ARs
   * @param orBranches receipting branches of ORs (Head Office)
   * @param usdRate rate of the day of USD
   * @param today business day
   */
  public record FormSettings(
      boolean arPostingStep,
      boolean orPostingStep,
      boolean remarksRequired,
      BigDecimal maxAmount,
      LocalDate latestCheckDate,
      int holdingDays,
      List<BankOption> bankAccounts,
      List<BranchOption> arBranches,
      List<BranchOption> orBranches,
      BigDecimal usdRate,
      LocalDate today) {

    /** Defensive copies. */
    public FormSettings {
      bankAccounts = List.copyOf(bankAccounts);
      arBranches = List.copyOf(arBranches);
      orBranches = List.copyOf(orBranches);
    }
  }

  /**
   * A bank account of the list.
   *
   * @param code code
   * @param name name
   * @param currency currency
   * @param defaultForCurrency default of its currency
   */
  public record BankOption(String code, String name, String currency, boolean defaultForCurrency) {}

  /**
   * A receipting branch.
   *
   * @param id id
   * @param code code (branch indicator)
   * @param name name
   */
  public record BranchOption(Long id, String code, String name) {}

  /**
   * A row of the account table.
   *
   * @param invoiceNo account (invoice) number
   * @param arn ARN
   * @param policyNo policy number
   * @param clientCode client
   * @param assuredName assured
   * @param insurerCode insurer
   * @param currency booked currency
   * @param premiumReceivable premium receivable amount
   * @param commissionReceivable commission receivable amount
   * @param totalInvoice total invoice amount
   * @param totalPaid total payments received
   * @param outstanding outstanding balance
   * @param bir2307 BIR 2307 tag
   * @param pr2307 PR 2307 amount, null without the tag
   * @param prebooked an account not booked yet
   */
  public record AccountRow(
      String invoiceNo,
      String arn,
      String policyNo,
      String clientCode,
      String assuredName,
      String insurerCode,
      String currency,
      BigDecimal premiumReceivable,
      BigDecimal commissionReceivable,
      BigDecimal totalInvoice,
      BigDecimal totalPaid,
      BigDecimal outstanding,
      boolean bir2307,
      BigDecimal pr2307,
      boolean prebooked) {

    static AccountRow prebooked(String arn, String client) {
      return new AccountRow(
          arn, arn, null, null, client, null, null, null, null, null, null, null, false, null,
          true);
    }
  }
}
