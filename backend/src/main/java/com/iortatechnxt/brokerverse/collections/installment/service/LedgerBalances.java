package com.iortatechnxt.brokerverse.collections.installment.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only view of the Operations invoice ledger for the plans, promises, escalations and billing
 * statements (COLLECTIONS_DESIGN principle 1: Collections never keeps a second copy of the money).
 * The outstanding of an invoice is its premium receivable balance, whatever settled it (applied
 * payment, 2307 reclass, direct payment reversal, write-off). The worklist item of wave C1-A is not
 * read: an invoice is a collection account when it is a client receivable of the company.
 */
@Component
@Transactional(readOnly = true)
public class LedgerBalances {

  /** Parameter: net outstanding above which an invoice is followed up (BRCLXN.005, CQ03). */
  public static final String THRESHOLD = "CLX_MIN_BALANCE_THRESHOLD";

  private final InvoiceLedgerQueryService ledger;
  private final SystemParameterService parameters;

  /**
   * Creates the reader.
   *
   * @param ledger invoice ledger
   * @param parameters business parameters
   */
  public LedgerBalances(InvoiceLedgerQueryService ledger, SystemParameterService parameters) {
    this.ledger = ledger;
    this.parameters = parameters;
  }

  /**
   * A collection account of the company: an invoice of the ledger that is a client receivable (not
   * a direct payment, not cancelled).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @return invoice with its components
   */
  public OpsInvoice requireReceivable(Long companyId, String invoiceNo) {
    OpsInvoice invoice =
        ledger
            .find(invoiceNo == null ? "" : invoiceNo.strip())
            .filter(i -> i.getCompanyId().equals(companyId))
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "CLX_INVOICE_UNKNOWN", "Invoice " + invoiceNo + " is not in the ledger"));
    if (invoice.isDpFlag() || invoice.isCancelled()) {
      throw new BusinessRuleException(
          "CLX_INVOICE_NOT_COLLECTIBLE",
          "Invoice "
              + invoice.getInvoiceNo()
              + " is "
              + (invoice.isDpFlag() ? "paid directly to the insurer" : "cancelled")
              + " and has no premium to collect");
    }
    return invoice;
  }

  /**
   * The ledger invoice, if any.
   *
   * @param invoiceNo invoice
   * @return invoice
   */
  public Optional<OpsInvoice> find(String invoiceNo) {
    return ledger.find(invoiceNo);
  }

  /**
   * Outstanding premium receivable of an invoice.
   *
   * @param invoiceNo invoice
   * @return balance, empty when the invoice is not in the ledger
   */
  public Optional<BigDecimal> outstanding(String invoiceNo) {
    return ledger.find(invoiceNo).map(OpsInvoice::premiumBalance);
  }

  /**
   * Payments applied to the premium receivable of an invoice with a value date in a window, net of
   * reversed applications (BRCLXN.055).
   *
   * @param invoiceNo invoice
   * @param from first value date
   * @param to last value date
   * @return net amount and the last value date
   */
  public Payments paymentsBetween(String invoiceNo, LocalDate from, LocalDate to) {
    BigDecimal total = BigDecimal.ZERO;
    LocalDate last = null;
    for (OpsInvoiceMovement m : ledger.movements(invoiceNo)) {
      if (!m.getComponent().isPremiumReceivable()
          || m.getValueDate().isBefore(from)
          || m.getValueDate().isAfter(to)) {
        continue;
      }
      if (m.getMovementType() == MovementType.APPLIED) {
        total = total.add(m.getAmount());
        last = last == null || m.getValueDate().isAfter(last) ? m.getValueDate() : last;
      } else if (m.getMovementType() == MovementType.UNAPPLIED) {
        total = total.subtract(m.getAmount());
      }
    }
    return new Payments(total, last);
  }

  /**
   * The minimum balance followed up (CLX_MIN_BALANCE_THRESHOLD).
   *
   * @return threshold
   */
  public BigDecimal threshold() {
    return new BigDecimal(parameters.text(THRESHOLD, "10.00"));
  }

  /**
   * Whether nothing is left to collect: the outstanding is at or below the threshold.
   *
   * @param outstanding balance
   * @return true when collected
   */
  public boolean collected(BigDecimal outstanding) {
    return outstanding.compareTo(threshold()) <= 0;
  }

  /**
   * Payments found in a window.
   *
   * @param total net amount applied
   * @param lastDate value date of the last application, null when none
   */
  public record Payments(BigDecimal total, LocalDate lastDate) {}
}
