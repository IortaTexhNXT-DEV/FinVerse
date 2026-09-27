package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine.LineFacts;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Part;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Position;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads what remittance needs from the Operations ledger (RMTID.006/028/038): the position of an
 * invoice (paid AR, DTIP, commission, VAT, WTAX), the value date of its last applied payment and
 * the check holding period in banking days of the invoice's branch (RMTID.017/018, holidays of the
 * organization module).
 */
@Component
public class LedgerPositions {

  private final InvoiceLedgerQueryService ledger;
  private final OrganizationService organization;

  /**
   * Creates the reader.
   *
   * @param ledger ledger reads
   * @param organization branches and holidays
   */
  public LedgerPositions(InvoiceLedgerQueryService ledger, OrganizationService organization) {
    this.ledger = ledger;
    this.organization = organization;
  }

  /**
   * The ledger position of an invoice (components loaded).
   *
   * @param invoice invoice
   * @return position
   */
  public static Position position(OpsInvoice invoice) {
    BigDecimal premiumDue = BigDecimal.ZERO;
    BigDecimal paid = BigDecimal.ZERO;
    for (LedgerComponent c : LedgerComponent.applicationHierarchy()) {
      OpsInvoiceComponent row = invoice.component(c);
      premiumDue = premiumDue.add(row.due());
      paid = paid.add(row.netApplied());
    }
    return new Position(
        premiumDue,
        paid,
        invoice.component(LedgerComponent.BASIC).due(),
        part(invoice, LedgerComponent.DTIP),
        part(invoice, LedgerComponent.COMMISSION),
        part(invoice, LedgerComponent.COMMISSION_VAT),
        part(invoice, LedgerComponent.WTAX));
  }

  private static Part part(OpsInvoice invoice, LedgerComponent component) {
    OpsInvoiceComponent row = invoice.component(component);
    return new Part(row.due(), row.getRemitted(), row.getBalance());
  }

  /**
   * The value date of the last payment applied to an invoice.
   *
   * @param invoiceNo invoice
   * @return date, empty when nothing was applied
   */
  public Optional<LocalDate> lastPaidOn(String invoiceNo) {
    return ledger.movements(invoiceNo).stream()
        .filter(m -> m.getMovementType() == MovementType.APPLIED && m.getAmount().signum() > 0)
        .map(OpsInvoiceMovement::getValueDate)
        .max(Comparator.naturalOrder());
  }

  /**
   * The schedule facts of an invoice for a batch line.
   *
   * @param invoice invoice
   * @param lastPaidOn last payment date
   * @return facts
   */
  public static LineFacts facts(OpsInvoice invoice, LocalDate lastPaidOn) {
    var c = invoice.getClassification();
    return new LineFacts(
        invoice.getInvoiceNo(),
        invoice.getArn(),
        invoice.getEndorsementNo(),
        invoice.getPolicyNo(),
        invoice.getClientCode(),
        invoice.getAssuredName(),
        c.riskCode(),
        c.productLine(),
        c.segment(),
        c.inceptionDate(),
        c.expiryDate(),
        c.bookingDate(),
        lastPaidOn);
  }

  /** Counts banking days per branch within one extraction run (branches cached). */
  public final class HoldingPeriod {

    private final int days;
    private final LocalDate businessDate;
    private final Map<Long, Branch> branches = new HashMap<>();

    HoldingPeriod(int days, LocalDate businessDate) {
      this.days = days;
      this.businessDate = businessDate;
    }

    /**
     * Whether a payment of a date is still within the holding period (RMTID.017): fewer than the
     * required banking days of the branch have passed since the value date.
     *
     * @param branchId invoice branch
     * @param paidOn value date of the payment
     * @return true while it must be held
     */
    public boolean holds(Long branchId, LocalDate paidOn) {
      if (days <= 0) {
        return false;
      }
      Branch branch = branches.computeIfAbsent(branchId, organization::getBranch);
      int passed = 0;
      LocalDate day = paidOn.plusDays(1);
      while (!day.isAfter(businessDate) && passed < days) {
        if (organization.isWorkingDay(branch, day)) {
          passed++;
        }
        day = day.plusDays(1);
      }
      return passed < days;
    }
  }

  /**
   * A holding period calculator for one run.
   *
   * @param days banking days to hold ({@code REMIT_CHECK_HOLD_DAYS})
   * @param businessDate business date of the run
   * @return calculator
   */
  public HoldingPeriod holdingPeriod(int days, LocalDate businessDate) {
    return new HoldingPeriod(days, businessDate);
  }
}
