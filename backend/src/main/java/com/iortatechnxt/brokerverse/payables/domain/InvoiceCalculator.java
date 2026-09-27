package com.iortatechnxt.brokerverse.payables.domain;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;

/**
 * Tax arithmetic of supplier invoices (Philippine practice).
 *
 * <ul>
 *   <li>Line amounts are entered net of VAT.
 *   <li>Input VAT = 12 % of the net amount when the supplier is VAT registered.
 *   <li>Expanded withholding tax (EWT) = supplier rate (percent) of the net amount, withheld from
 *       the payment and remitted to the tax authority.
 *   <li>Payable to the supplier = net + input VAT - EWT.
 * </ul>
 *
 * Each line is rounded to centavos; invoice totals are the sums of the rounded lines, so the
 * journals published per line always add up to the invoice.
 */
public final class InvoiceCalculator {

  /** Philippine VAT rate. */
  public static final BigDecimal VAT_RATE = new BigDecimal("0.12");

  private static final BigDecimal PERCENT = new BigDecimal("100");

  private InvoiceCalculator() {}

  /**
   * Input VAT of a line.
   *
   * @param net net amount
   * @param vatApplicable whether VAT applies
   * @return VAT, zero when not applicable
   */
  public static BigDecimal vat(BigDecimal net, boolean vatApplicable) {
    return vatApplicable ? Money.round(net.multiply(VAT_RATE)) : Money.zero();
  }

  /**
   * Withholding tax of a line.
   *
   * @param net net amount
   * @param ratePercent withholding rate in percent (e.g. 2 for 2 %), null = none
   * @return withholding tax
   */
  public static BigDecimal withholding(BigDecimal net, BigDecimal ratePercent) {
    if (ratePercent == null || ratePercent.signum() == 0) {
      return Money.zero();
    }
    return Money.round(net.multiply(ratePercent).divide(PERCENT, Money.RATE_SCALE, Money.ROUNDING));
  }

  /**
   * Amount payable to the supplier.
   *
   * @param net net amount
   * @param vat input VAT
   * @param withholding withholding tax
   * @return net + VAT - withholding
   */
  public static BigDecimal payable(BigDecimal net, BigDecimal vat, BigDecimal withholding) {
    return Money.round(net.add(vat).subtract(withholding));
  }
}
