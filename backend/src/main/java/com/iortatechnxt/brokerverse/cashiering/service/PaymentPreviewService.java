package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.service.ApplicationPlanner.InvoiceBalances;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationPlanner.Plan;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentMatcher.Match;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The live application preview of the over-the-counter screen (CSHID.020/022): what a payment would
 * match and how it would be applied by component, the 2% CWT the client withholds, the excess that
 * would stay unapplied and the BOOK rate of the currency. Nothing is saved.
 */
@Service
@Transactional(readOnly = true)
public class PaymentPreviewService {

  private final PaymentMatcher matcher;
  private final ApplicationService applier;
  private final CashieringSettings settings;

  /**
   * Creates the service.
   *
   * @param matcher matching
   * @param applier application engine (balances)
   * @param settings settings (BOOK rate, CWT percent)
   */
  public PaymentPreviewService(
      PaymentMatcher matcher, ApplicationService applier, CashieringSettings settings) {
    this.matcher = matcher;
    this.applier = applier;
    this.settings = settings;
  }

  /**
   * Previews a payment.
   *
   * @param companyId company
   * @param references references in order
   * @param amount amount, may be null (preview of the outstanding balance only)
   * @param currency currency, may be null for the invoice currency
   * @param date payment date
   * @return preview
   */
  public Preview preview(
      Long companyId, List<String> references, BigDecimal amount, String currency, LocalDate date) {
    Match match = matcher.match(companyId, references);
    BigDecimal left = amount == null ? BigDecimal.ZERO : amount;
    BigDecimal withheld = BigDecimal.ZERO;
    List<InvoicePreview> invoices = new ArrayList<>();
    for (OpsInvoice invoice : match.invoices()) {
      InvoiceBalances balances = applier.balancesOf(invoice);
      Plan plan =
          ApplicationService.receivable(invoice)
              ? ApplicationPlanner.plan(balances, left, false)
              : ApplicationPlanner.plan(
                  new InvoiceBalances(Map.of(), BigDecimal.ZERO), left, false);
      left = plan.excess();
      withheld = withheld.add(balances.withheld());
      invoices.add(InvoicePreview.of(invoice, plan, balances));
    }
    String ccy =
        currency != null
            ? currency
            : match.invoices().stream().findFirst().map(OpsInvoice::getCurrency).orElse("PHP");
    return new Preview(
        match.kind().name(),
        match.reference(),
        match.account() == null ? null : match.account().getArn(),
        match.clientCode(),
        invoices,
        withheld,
        left,
        ccy,
        settings.bookRate(companyId, ccy, date),
        settings.cwtApplicationPercent());
  }

  /**
   * The preview of a payment.
   *
   * @param match BOOKED, CANCELLED, PREBOOKED or NONE
   * @param reference reference that matched
   * @param prebookedArn pre-booked account, may be null
   * @param clientCode client, may be null
   * @param invoices application per invoice, oldest first
   * @param cwtWithheld 2% the client withholds (CSHID.020)
   * @param excess amount that would stay unapplied
   * @param currency currency
   * @param bookRate BOOK rate (CSHID.012-014)
   * @param cwtPercent applied percent of CWT accounts
   */
  public record Preview(
      String match,
      String reference,
      String prebookedArn,
      String clientCode,
      List<InvoicePreview> invoices,
      BigDecimal cwtWithheld,
      BigDecimal excess,
      String currency,
      BigDecimal bookRate,
      BigDecimal cwtPercent) {

    /** Defensive copy. */
    public Preview {
      invoices = List.copyOf(invoices);
    }
  }

  /**
   * The application preview of one invoice.
   *
   * @param invoiceNo invoice
   * @param arn account
   * @param assuredName assured
   * @param currency currency
   * @param cwt 2% CWT account
   * @param receivable whether it takes client payments
   * @param outstanding premium outstanding
   * @param balances balance per component
   * @param allocation amount applied per component in hierarchy order
   * @param applied total applied
   */
  public record InvoicePreview(
      String invoiceNo,
      String arn,
      String assuredName,
      String currency,
      boolean cwt,
      boolean receivable,
      BigDecimal outstanding,
      Map<LedgerComponent, BigDecimal> balances,
      Map<LedgerComponent, BigDecimal> allocation,
      BigDecimal applied) {

    /**
     * The preview of an invoice.
     *
     * @param invoice invoice
     * @param plan planned application
     * @param balances balances
     * @return preview
     */
    static InvoicePreview of(OpsInvoice invoice, Plan plan, InvoiceBalances balances) {
      return new InvoicePreview(
          invoice.getInvoiceNo(),
          invoice.getArn(),
          invoice.getAssuredName(),
          invoice.getCurrency(),
          invoice.isCwtFlag(),
          ApplicationService.receivable(invoice),
          invoice.premiumBalance(),
          balances.balances(),
          plan.allocation(),
          plan.applied());
    }
  }
}
