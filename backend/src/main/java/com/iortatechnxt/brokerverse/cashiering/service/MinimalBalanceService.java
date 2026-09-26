package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.MinimalBalanceRule;
import com.iortatechnxt.brokerverse.cashiering.domain.MinimalBalanceRuleRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPosting.PostingContext;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal balances (CSHID.016, Cashiering summary 5.f, OQ11) against the maintained rule table:
 *
 * <ul>
 *   <li>premium receivable balances up to the maximum are reversed ({@code
 *       OPS_MINIMAL_BALANCE_REVERSAL}, ledger {@code MIN_BAL}) unless they equal the 2% CWT the
 *       client withholds, the DST charged or the whole premium; never twice for an invoice;
 *   <li>unapplied and excess payments up to the maximum go to AP overages ({@code
 *       OPS_EXCESS_TO_OVERAGES}) and their item is closed.
 * </ul>
 *
 * <p>This automatic sweep (up to PHP 10) is separate from Adjustment's {@code MINIMAL_BALANCE_FILE}
 * upload (PHP 10-100 write-offs): an invoice already written off ({@code WRITTEN_OFF} flag) is
 * never swept.
 */
@Service
@Transactional
public class MinimalBalanceService {

  private static final String CANDIDATES =
      "select i.invoice_no from ops_invoice i join ops_invoice_component c on c.invoice_id = i.id"
          + " where i.payment_status = 'PARTIALLY_PAID' and not i.cancelled and not i.written_off"
          + " and c.component in ('BASIC', 'DST', 'PREMIUM_TAX_VAT', 'LGT', 'FST', 'OTHER')"
          + " and not exists (select 1 from csh_minimal_balance m where m.kind = 'PREMIUM'"
          + " and m.subject_ref = i.invoice_no)"
          + " group by i.invoice_no having sum(c.balance) > 0 and sum(c.balance) <= ?"
          + " order by i.invoice_no";

  private static final String LOG =
      "insert into csh_minimal_balance (company_id, kind, subject_ref, invoice_no, client_code,"
          + " sales_unit, amount, components, journal_batch_no, swept_on, swept_at, swept_by)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private final MinimalBalanceRuleRepository rules;
  private final UnappliedRepository items;
  private final UnappliedService unapplied;
  private final InvoiceLedgerQueryService ledgerQuery;
  private final InvoiceLedgerService ledger;
  private final ApplicationService applier;
  private final CashieringPosting posting;
  private final JdbcTemplate jdbc;
  private final ItemTransactions transactions;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rules minimal balance rules
   * @param items unapplied items
   * @param unapplied unapplied workbench
   * @param ledgerQuery invoice ledger reads
   * @param ledger invoice ledger writes
   * @param applier application engine (2% expected)
   * @param posting accounting events
   * @param jdbc JDBC
   * @param transactions one transaction per item
   * @param currentUser current user
   * @param clock clock
   */
  public MinimalBalanceService(
      MinimalBalanceRuleRepository rules,
      UnappliedRepository items,
      UnappliedService unapplied,
      InvoiceLedgerQueryService ledgerQuery,
      InvoiceLedgerService ledger,
      ApplicationService applier,
      CashieringPosting posting,
      JdbcTemplate jdbc,
      ItemTransactions transactions,
      CurrentUser currentUser,
      Clock clock) {
    this.rules = rules;
    this.items = items;
    this.unapplied = unapplied;
    this.ledgerQuery = ledgerQuery;
    this.ledger = ledger;
    this.applier = applier;
    this.posting = posting;
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The rule table.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<MinimalBalanceRule> rules() {
    return rules.findAll();
  }

  /**
   * Runs both sweeps.
   *
   * @param businessDate business date
   * @return premium balances reversed and excess items moved to overages
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Sweep sweep(LocalDate businessDate) {
    int premium = 0;
    Optional<MinimalBalanceRule> premiumRule = active(MinimalBalanceRule.PREMIUM);
    if (premiumRule.isPresent()) {
      MinimalBalanceRule rule = premiumRule.get();
      for (String invoiceNo : jdbc.queryForList(CANDIDATES, String.class, rule.getMaxAmount())) {
        if (transactions.run(
            "MINP:" + invoiceNo,
            () -> reversePremium(ledgerQuery.require(invoiceNo), rule, businessDate))) {
          premium++;
        }
      }
    }
    int excess = 0;
    Optional<MinimalBalanceRule> excessRule = active(MinimalBalanceRule.EXCESS);
    if (excessRule.isPresent()) {
      List<Long> ids =
          items.smallBalances(Unapplied.STAGE_INITIAL, excessRule.get().getMaxAmount()).stream()
              .map(Unapplied::getId)
              .toList();
      for (Long id : ids) {
        if (transactions.run(
            "MINX:" + id, () -> toOverages(items.findById(id).orElseThrow(), businessDate))) {
          excess++;
        }
      }
    }
    return new Sweep(premium, excess);
  }

  private boolean reversePremium(OpsInvoice invoice, MinimalBalanceRule rule, LocalDate date) {
    invoice.loadCollections();
    BigDecimal balance = invoice.premiumBalance();
    if (excluded(invoice, rule, balance)) {
      return false;
    }
    Map<LedgerComponent, BigDecimal> reversed = new EnumMap<>(LedgerComponent.class);
    for (LedgerComponent c : LedgerComponent.applicationHierarchy()) {
      BigDecimal b = invoice.component(c).getBalance();
      if (b.signum() > 0) {
        reversed.put(c, b);
      }
    }
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("TOTAL", balance);
    amounts.putAll(CashieringPosting.prAmounts(reversed, false));
    String ref = "MINP:" + invoice.getInvoiceNo();
    String batch =
        posting.publish(
            ApplicationService.context(invoice, date, "Minimal balance reversal"),
            CashieringPosting.MINIMAL_BALANCE,
            ref,
            amounts);
    ledger.post(
        new MovementRequest(
            invoice.getInvoiceNo(),
            MovementType.MIN_BAL,
            CashieringSettings.MODULE,
            ref,
            date,
            reversed,
            new DocumentRefs(null, null, null, batch),
            "Minimal balance (CSHID.016)"));
    log(
        new LogRow(
            invoice.getCompanyId(),
            MinimalBalanceRule.PREMIUM,
            invoice.getInvoiceNo(),
            invoice.getInvoiceNo(),
            invoice.getClientCode(),
            invoice.getClassification().salesUnit(),
            balance,
            reversed.keySet().toString(),
            batch),
        date);
    return true;
  }

  private boolean excluded(OpsInvoice invoice, MinimalBalanceRule rule, BigDecimal balance) {
    BigDecimal due = ApplicationService.premiumDue(invoice);
    boolean cwt =
        rule.isExcludeCwt()
            && invoice.isCwtFlag()
            && balance.compareTo(applier.balancesOf(invoice).withheld()) == 0;
    boolean dst =
        rule.isExcludeDst() && balance.compareTo(invoice.component(LedgerComponent.DST).due()) == 0;
    boolean whole = rule.isExcludeWholePremium() && balance.compareTo(due) == 0;
    return invoice.isWrittenOff() || cwt || dst || whole;
  }

  private boolean toOverages(Unapplied item, LocalDate date) {
    BigDecimal amount = item.getBalance();
    String batch =
        posting.publish(
            new PostingContext(
                item.getCompanyId(),
                item.getBranchId(),
                date,
                item.getCurrency(),
                item.getReference(),
                item.getClientCode(),
                null,
                null,
                "Minimal excess to AP overages " + item.getReference(),
                null),
            CashieringPosting.EXCESS_TO_OVERAGES,
            "MINX:" + item.getId(),
            Map.of(CashieringPosting.AMOUNT, amount));
    unapplied.close(item, "sweep_overages", "Minimal balance " + amount + " to AP overages");
    log(
        new LogRow(
            item.getCompanyId(),
            MinimalBalanceRule.EXCESS,
            item.getReference(),
            item.getInvoiceNo(),
            item.getClientCode(),
            item.getSalesUnit(),
            amount,
            null,
            batch),
        date);
    return true;
  }

  private void log(LogRow row, LocalDate date) {
    jdbc.update(
        LOG,
        row.companyId(),
        row.kind(),
        row.subjectRef(),
        row.invoiceNo(),
        row.clientCode(),
        row.salesUnit(),
        row.amount(),
        row.components(),
        row.batch(),
        date,
        Timestamp.from(clock.instant()),
        currentUser.username());
  }

  private Optional<MinimalBalanceRule> active(String kind) {
    return rules.findById(kind).filter(MinimalBalanceRule::isActive);
  }

  private record LogRow(
      Long companyId,
      String kind,
      String subjectRef,
      String invoiceNo,
      String clientCode,
      String salesUnit,
      BigDecimal amount,
      String components,
      String batch) {}

  /**
   * Outcome of a sweep.
   *
   * @param premium premium balances reversed
   * @param excess excess items moved to overages
   */
  public record Sweep(int premium, int excess) {}
}
