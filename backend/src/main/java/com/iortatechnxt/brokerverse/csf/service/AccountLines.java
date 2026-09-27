package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.catalog.service.CatalogNames;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.AccountLine;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account lines of the Servicing View and the search results (FR-CSF-011, 012): the account with
 * its policy and PN numbers, the outstanding premium of its invoices, the payment status of its
 * latest invoice and the CSF status from the maintained mapping.
 */
@Component
@Transactional(readOnly = true)
public class AccountLines {

  private final InvoiceLedgerQueryService ledger;
  private final CsfStatusMapper mapper;
  private final CatalogNames names;

  /**
   * Creates the builder.
   *
   * @param ledger Operations invoice ledger
   * @param mapper CSF status mapping
   * @param names product and insurer names (agents do not read the catalog)
   */
  public AccountLines(InvoiceLedgerQueryService ledger, CsfStatusMapper mapper, CatalogNames names) {
    this.ledger = ledger;
    this.mapper = mapper;
    this.names = names;
  }

  /**
   * Lines of accounts, in the given order.
   *
   * @param accounts accounts
   * @return lines
   */
  public List<AccountLine> of(List<Account> accounts) {
    if (accounts.isEmpty()) {
      return List.of();
    }
    CsfStatusRules rules = mapper.rules();
    return accounts.stream().map(a -> line(rules, a)).toList();
  }

  private AccountLine line(CsfStatusRules rules, Account a) {
    List<OpsInvoice> invoices = ledger.forArn(a.getArn());
    BigDecimal balance =
        invoices.stream().map(OpsInvoice::premiumBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    String paymentStatus =
        invoices.isEmpty() ? null : invoices.get(invoices.size() - 1).getPaymentStatus().name();
    return new AccountLine(
        a.getId(),
        a.getArn(),
        a.getProductCode(),
        names.productName(a.getProductCode()),
        a.getLineCode(),
        a.getInsurerCode(),
        a.getInsurerCode() == null ? null : names.insurer(a.getCompanyId(), a.getInsurerCode()),
        a.getStatus().name(),
        mapper.statusOf(rules, a, balance).orElse(null),
        a.getPolicyNumbers(),
        a.getPnNumbers(),
        a.getLoanApplicationNo(),
        a.getPeriodFrom(),
        a.getPeriodTo(),
        a.getCurrency(),
        invoices.isEmpty() ? null : balance,
        paymentStatus,
        a.getMarketSegment(),
        a.getFreeFirstYear() != null && a.getFreeFirstYear().active(),
        a.isDirectPayment());
  }
}
