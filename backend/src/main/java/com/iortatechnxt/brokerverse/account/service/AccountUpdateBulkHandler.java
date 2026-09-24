package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountContact;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.AccountBulkSupport.Headers;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk update of existing accounts matched by ARN (BRNB.024/025/052): blank cells keep the current
 * value; the first risk item's sum insured and rate may be changed. The account is re-rated and
 * checked like an individual update; with Submit = Y a draft is submitted and a returned account
 * resubmitted.
 */
@Component
public class AccountUpdateBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "ACCOUNT_UPDATE";

  private static final Set<AccountStatus> UPDATABLE =
      EnumSet.of(AccountStatus.DRAFT, AccountStatus.RETURNED_TO_MARKETING, AccountStatus.SUBMITTED);

  private final AccountService accounts;
  private final AccountQueryService queries;
  private final AccountRules rules;
  private final ProductCatalogService catalog;
  private final AccountBulkSupport support;

  /**
   * Creates the handler.
   *
   * @param accounts accounts
   * @param queries account reads
   * @param rules draft resolution
   * @param catalog products and lines
   * @param support shared bulk helpers
   */
  public AccountUpdateBulkHandler(
      AccountService accounts,
      AccountQueryService queries,
      AccountRules rules,
      ProductCatalogService catalog,
      AccountBulkSupport support) {
    this.accounts = accounts;
    this.queries = queries;
    this.rules = rules;
    this.catalog = catalog;
    this.support = support;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Bulk account update";
  }

  @Override
  public String permission() {
    return "ACCOUNT_MAINTAIN";
  }

  @Override
  public String instructions() {
    return "One row per account, matched by ARN. Only filled-in cells change the account; blank"
        + " cells keep the current value. Accounts in Draft, Returned or Submitted status can be"
        + " updated. Submit = Y submits a draft or resubmits a returned account.";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(Headers.ARN, "Account Reference Number", "ARN-2026-000123"),
        AccountBulkSupport.date(Headers.PERIOD_FROM, "Period from", false),
        AccountBulkSupport.date(Headers.PERIOD_TO, "Period to", false),
        BulkColumn.optional(Headers.SEGMENT, "Market segment", ""),
        BulkColumn.optional(Headers.INSURER, "Insurer party code", ""),
        BulkColumn.optional(Headers.BRANCH, "Insurer branch code", ""),
        AccountBulkSupport.number(Headers.SUM_INSURED, "Sum insured of item 1", false, ""),
        AccountBulkSupport.number(Headers.RATE, "Premium rate % of item 1", false, ""),
        BulkColumn.optional(Headers.MORTGAGEE, "Mortgagee bank", ""),
        BulkColumn.optional(Headers.LOAN, "Loan application number", ""),
        BulkColumn.optional(Headers.PN, "PN numbers separated by ; (replace the list)", ""),
        BulkColumn.optional(Headers.CONTACT_EMAIL, "Account contact e-mail", ""),
        BulkColumn.optional(Headers.CONTACT_MOBILE, "Account contact mobile", ""),
        new BulkColumn(
            Headers.DIRECT_PAYMENT,
            "Paid directly to the insurer",
            false,
            BulkColumn.Type.YES_NO,
            ""),
        new BulkColumn(
            Headers.SUBMIT,
            "Submit / resubmit after the update",
            false,
            BulkColumn.Type.YES_NO,
            "N"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(Headers.ARN);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return support.safely(
        () -> {
          Account account = updatable(row, context);
          rules.resolve(context.companyId(), merged(account, row));
          return List.of();
        });
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    Account account = updatable(row, context);
    AccountStatus before = account.getStatus();
    accounts.update(account.getId(), merged(account, row));
    if (row.yes(Headers.SUBMIT)) {
      String comment = "Bulk update " + context.jobNo();
      if (before == AccountStatus.DRAFT) {
        accounts.submit(account.getId(), comment);
      } else if (before == AccountStatus.RETURNED_TO_MARKETING) {
        accounts.resubmit(account.getId(), comment);
      }
    }
    return account.getArn();
  }

  private Account updatable(BulkRow row, BulkContext context) {
    Account account = queries.requireByArn(row.text(Headers.ARN));
    if (!account.getCompanyId().equals(context.companyId())) {
      throw new BusinessRuleException(
          "ACCOUNT_OTHER_COMPANY", "Account " + account.getArn() + " belongs to another company");
    }
    if (!UPDATABLE.contains(account.getStatus())) {
      throw new BusinessRuleException(
          "ACCOUNT_NOT_EDITABLE", "Account " + account.getArn() + " is " + account.getStatus());
    }
    return account;
  }

  private AccountDraft merged(Account account, BulkRow row) {
    AccountDraft d =
        AccountFields.draftOf(
            account, catalog.requireLine(account.getLineCode()).getRiskItemKind());
    Mortgage m = d.mortgage();
    AccountContact c = d.contact();
    String insurer = or(row.text(Headers.INSURER), d.insurerCode());
    return new AccountDraft(
        d.clientId(),
        d.productCode(),
        or(row.text(Headers.SEGMENT), d.marketSegment()),
        d.sourceChannel(),
        insurer,
        or(row.text(Headers.BRANCH), d.insurerBranch()),
        or(row.date(Headers.PERIOD_FROM), d.periodFrom()),
        or(row.date(Headers.PERIOD_TO), d.periodTo()),
        d.multiYear(),
        d.termYears(),
        d.currency(),
        arrangement(row, d.paymentArrangement()),
        new Mortgage(
            or(row.text(Headers.MORTGAGEE), m.bank()),
            or(row.text(Headers.LOAN), m.loanApplicationNo()),
            row.text(Headers.PN) == null
                ? m.pnNumbers()
                : AccountBulkSupport.list(row.text(Headers.PN))),
        new AccountContact(
            c.name(),
            or(row.text(Headers.CONTACT_EMAIL), c.email()),
            or(row.text(Headers.CONTACT_MOBILE), c.mobile()),
            c.address()),
        items(d.items(), row.number(Headers.SUM_INSURED), row.number(Headers.RATE)),
        d.ratingBasis(),
        d.commissionRate(),
        d.ffyStart());
  }

  private static PaymentArrangement arrangement(BulkRow row, PaymentArrangement current) {
    if (row.text(Headers.DIRECT_PAYMENT) == null) {
      return current;
    }
    return row.yes(Headers.DIRECT_PAYMENT)
        ? PaymentArrangement.DIRECT_TO_INSURER
        : PaymentArrangement.VIA_BDOI;
  }

  private static List<RiskItemData> items(
      List<RiskItemData> items, BigDecimal sumInsured, BigDecimal rate) {
    if (items.isEmpty() || sumInsured == null && rate == null) {
      return items;
    }
    List<RiskItemData> result = new ArrayList<>(items);
    RiskItemData first = items.get(0);
    result.set(
        0,
        new RiskItemData(
            first.description(),
            or(sumInsured, first.sumInsured()),
            or(rate, first.rate()),
            first.biLimit(),
            first.pdLimit(),
            first.vehicle(),
            first.location(),
            first.person()));
    return result;
  }

  private static <T> T or(T value, T current) {
    return value != null ? value : current;
  }
}
