package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits a payment over the outstanding premium components of an invoice (CSHID.020/022): in the
 * hierarchy DST, premium tax / VAT, LGT, FST, other charges, basic premium, up to 100% of the
 * balance, or for a 2% CWT account up to the balance less the 2% the client withholds (98% by
 * default). What cannot be applied is the excess.
 */
public final class ApplicationPlanner {

  private static final BigDecimal HUNDRED = new BigDecimal("100");

  private ApplicationPlanner() {}

  /**
   * Plans an application.
   *
   * @param invoice outstanding balances, premium due and CWT facts
   * @param amount money available
   * @param dstOnly apply to DST only (DST payment application disposition)
   * @return allocation, applied amount and excess
   */
  public static Plan plan(InvoiceBalances invoice, BigDecimal amount, boolean dstOnly) {
    BigDecimal outstanding = BigDecimal.ZERO;
    for (LedgerComponent c : LedgerComponent.applicationHierarchy()) {
      outstanding = outstanding.add(positive(invoice.balances().get(c)));
    }
    BigDecimal cap = outstanding.subtract(invoice.withheld()).max(BigDecimal.ZERO);
    BigDecimal available = amount.min(cap).max(BigDecimal.ZERO);
    Map<LedgerComponent, BigDecimal> allocation = new LinkedHashMap<>();
    List<LedgerComponent> order =
        dstOnly ? List.of(LedgerComponent.DST) : LedgerComponent.applicationHierarchy();
    BigDecimal left = available;
    for (LedgerComponent c : order) {
      BigDecimal take = positive(invoice.balances().get(c)).min(left);
      if (take.signum() > 0) {
        allocation.put(c, take);
        left = left.subtract(take);
      }
    }
    BigDecimal applied = available.subtract(left);
    return new Plan(allocation, applied, amount.subtract(applied), cap);
  }

  /**
   * The part of the premium a 2% CWT client withholds and that stays outstanding until its BIR 2307
   * certificate is reclassified (CSHID.020/027).
   *
   * @param premiumDue premium receivable due (booked and adjusted, after any reclass)
   * @param reclassified amount already reclassified to PR2307
   * @param appliedPercent share applied (98)
   * @return withheld amount still expected, zero for a fully reclassified invoice
   */
  public static BigDecimal withheld(
      BigDecimal premiumDue, BigDecimal reclassified, BigDecimal appliedPercent) {
    BigDecimal base = premiumDue.add(reclassified);
    BigDecimal share = HUNDRED.subtract(appliedPercent).divide(HUNDRED);
    return Money.round(base.multiply(share)).subtract(reclassified).max(BigDecimal.ZERO);
  }

  private static BigDecimal positive(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
  }

  /**
   * What an invoice still expects.
   *
   * @param balances balance per component
   * @param withheld amount a 2% CWT client withholds, zero otherwise
   */
  public record InvoiceBalances(Map<LedgerComponent, BigDecimal> balances, BigDecimal withheld) {

    /** Defensive copy. */
    public InvoiceBalances {
      balances = Map.copyOf(balances);
    }
  }

  /**
   * A planned application.
   *
   * @param allocation amount per component in hierarchy order
   * @param applied total applied
   * @param excess money left
   * @param cap most the invoice could take
   */
  public record Plan(
      Map<LedgerComponent, BigDecimal> allocation,
      BigDecimal applied,
      BigDecimal excess,
      BigDecimal cap) {

    /** Keeps the hierarchy order. */
    public Plan {
      allocation = Collections.unmodifiableMap(new LinkedHashMap<>(allocation));
    }
  }
}
