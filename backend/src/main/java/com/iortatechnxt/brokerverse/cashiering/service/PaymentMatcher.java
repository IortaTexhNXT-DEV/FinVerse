package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds what a payment pays (CSHID.020, OQ12): the booked invoices of the Operations ledger by
 * invoice number, ARN, policy number or PN number; else a pre-booked account (not yet booked) by
 * ARN, policy or PN number. References are tried in the order the payment carries them.
 */
@Component
@Transactional(readOnly = true)
public class PaymentMatcher {

  private static final String BY_POLICY_OR_PN =
      "select invoice_no from ops_invoice where company_id = ? and (policy_no = ?"
          + " or ',' || replace(coalesce(pn_nos, ''), ' ', '') || ',' like '%,' || ? || ',%')"
          + " order by policy_year, id";

  private final InvoiceLedgerQueryService ledger;
  private final AccountQueryService accounts;
  private final JdbcTemplate jdbc;

  /**
   * Creates the matcher.
   *
   * @param ledger invoice ledger
   * @param accounts accounts (pre-booked look-up)
   * @param jdbc JDBC
   */
  public PaymentMatcher(
      InvoiceLedgerQueryService ledger, AccountQueryService accounts, JdbcTemplate jdbc) {
    this.ledger = ledger;
    this.accounts = accounts;
    this.jdbc = jdbc;
  }

  /**
   * Matches a payment's references.
   *
   * @param companyId company
   * @param references references in order
   * @return the match
   */
  public Match match(Long companyId, List<String> references) {
    for (String ref : references) {
      List<OpsInvoice> invoices = invoices(companyId, ref);
      if (!invoices.isEmpty()) {
        List<OpsInvoice> live = invoices.stream().filter(i -> !i.isCancelled()).toList();
        return live.isEmpty()
            ? new Match(Kind.CANCELLED, ref, invoices, null)
            : new Match(Kind.BOOKED, ref, live, null);
      }
      Optional<Account> preBooked = accounts.preBooked(companyId, ref).stream().findFirst();
      if (preBooked.isPresent()) {
        return new Match(Kind.PREBOOKED, ref, List.of(), preBooked.get());
      }
    }
    return new Match(Kind.NONE, null, List.of(), null);
  }

  /**
   * Booked invoices of a reference, oldest first.
   *
   * @param companyId company
   * @param ref invoice, ARN, policy or PN number
   * @return invoices (components loaded)
   */
  public List<OpsInvoice> invoices(Long companyId, String ref) {
    Optional<OpsInvoice> byNo = ledger.find(ref).filter(i -> i.getCompanyId().equals(companyId));
    if (byNo.isPresent()) {
      return List.of(loaded(byNo.get()));
    }
    List<OpsInvoice> byArn =
        ledger.forArn(ref).stream().filter(i -> i.getCompanyId().equals(companyId)).toList();
    if (!byArn.isEmpty()) {
      return byArn;
    }
    List<OpsInvoice> found = new ArrayList<>();
    for (String no : jdbc.queryForList(BY_POLICY_OR_PN, String.class, companyId, ref, ref)) {
      ledger.find(no).map(PaymentMatcher::loaded).ifPresent(found::add);
    }
    found.sort(Comparator.comparing(OpsInvoice::getPolicyYear));
    return found;
  }

  private static OpsInvoice loaded(OpsInvoice invoice) {
    invoice.loadCollections();
    return invoice;
  }

  /** What a payment matched. */
  public enum Kind {
    /** Booked invoices that take payments. */
    BOOKED,
    /** Only cancelled invoices. */
    CANCELLED,
    /** An account not booked yet. */
    PREBOOKED,
    /** Nothing. */
    NONE
  }

  /**
   * A match.
   *
   * @param kind kind
   * @param reference reference that matched, null for none
   * @param invoices invoices, oldest first
   * @param account pre-booked account, null unless PREBOOKED
   */
  public record Match(Kind kind, String reference, List<OpsInvoice> invoices, Account account) {

    /** Defensive copy. */
    public Match {
      invoices = List.copyOf(invoices);
    }

    /**
     * The client of the match.
     *
     * @return client code, null for none
     */
    public String clientCode() {
      if (account != null) {
        return account.getClientCode();
      }
      return invoices.isEmpty() ? null : invoices.get(0).getClientCode();
    }
  }
}
