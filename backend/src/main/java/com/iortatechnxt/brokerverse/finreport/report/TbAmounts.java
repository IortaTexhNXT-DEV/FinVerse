package com.iortatechnxt.brokerverse.finreport.report;

import com.iortatechnxt.brokerverse.finreport.service.MovementRow;
import java.math.BigDecimal;

/**
 * Opening balance and period debits / credits in base currency (rule R-TB).
 *
 * @param opening net opening balance, debit positive
 * @param debit period debits
 * @param credit period credits
 */
public record TbAmounts(BigDecimal opening, BigDecimal debit, BigDecimal credit) {

  /** Zero amounts. */
  public static final TbAmounts ZERO =
      new TbAmounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

  /**
   * Amounts of a movement row.
   *
   * @param r row
   * @return amounts
   */
  public static TbAmounts of(MovementRow r) {
    return new TbAmounts(r.openBase(), r.debitBase(), r.creditBase());
  }

  /**
   * Adds another set of amounts.
   *
   * @param o other
   * @return sum
   */
  public TbAmounts plus(TbAmounts o) {
    return new TbAmounts(opening.add(o.opening), debit.add(o.debit), credit.add(o.credit));
  }

  /**
   * Closing net balance.
   *
   * @return opening + debit - credit
   */
  public BigDecimal closing() {
    return opening.add(debit).subtract(credit);
  }

  /**
   * Closing balance on the debit side (rule R-TB).
   *
   * @return closing when positive, else zero
   */
  public BigDecimal closingDebit() {
    return closing().max(BigDecimal.ZERO);
  }

  /**
   * Closing balance on the credit side (rule R-TB).
   *
   * @return minus closing when negative, else zero
   */
  public BigDecimal closingCredit() {
    return closing().min(BigDecimal.ZERO).negate();
  }

  /**
   * Whether all amounts are zero.
   *
   * @return true when nothing to report
   */
  public boolean isZero() {
    return opening.signum() == 0 && debit.signum() == 0 && credit.signum() == 0;
  }
}
