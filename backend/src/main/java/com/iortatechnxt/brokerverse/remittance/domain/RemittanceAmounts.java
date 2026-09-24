package com.iortatechnxt.brokerverse.remittance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * Amounts of a remittance line or batch (Annex III schedule columns, OPERATIONS_DESIGN 5 row 12):
 * paid AR remitted against the DTIP, realized commission and its VAT retained by BDOI, withholding
 * tax the insurer withholds on the commission, the DTIP reduced, the early-remittance incentive and
 * its VAT (RMTID.023) and the net due to the insurer before the incentive. Read-only once extracted
 * (RMTID.002 addendum).
 *
 * @param paidAr paid AR remitted
 * @param commission realized commission retained
 * @param commissionVat VAT on the commission
 * @param wtax withholding tax on the commission (withheld by the insurer)
 * @param dtip due to insurer reduced by the remittance
 * @param incentive early-remittance incentive
 * @param incentiveVat output VAT on the incentive
 * @param netDue paid AR + WTAX - commission - VAT (before the incentive)
 */
@Embeddable
public record RemittanceAmounts(
    @Column(name = "paid_ar", nullable = false, precision = 19, scale = 2) BigDecimal paidAr,
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal commission,
    @Column(name = "commission_vat", nullable = false, precision = 19, scale = 2)
        BigDecimal commissionVat,
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal wtax,
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal dtip,
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal incentive,
    @Column(name = "incentive_vat", nullable = false, precision = 19, scale = 2)
        BigDecimal incentiveVat,
    @Column(name = "net_due", nullable = false, precision = 19, scale = 2) BigDecimal netDue) {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  /** Nothing. */
  public static final RemittanceAmounts NONE =
      new RemittanceAmounts(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);

  /**
   * The sum of two amounts.
   *
   * @param other other amounts
   * @return sum
   */
  public RemittanceAmounts plus(RemittanceAmounts other) {
    return new RemittanceAmounts(
        paidAr.add(other.paidAr),
        commission.add(other.commission),
        commissionVat.add(other.commissionVat),
        wtax.add(other.wtax),
        dtip.add(other.dtip),
        incentive.add(other.incentive),
        incentiveVat.add(other.incentiveVat),
        netDue.add(other.netDue));
  }

  /**
   * Incentive and its VAT, deducted from the payment (OPERATIONS_DESIGN 5 row 13).
   *
   * @return incentive + VAT
   */
  public BigDecimal incentiveTotal() {
    return incentive.add(incentiveVat);
  }

  /**
   * What Disbursement pays the insurer.
   *
   * @return net due less the incentive and its VAT
   */
  public BigDecimal payable() {
    return netDue.subtract(incentiveTotal());
  }

  /**
   * Commission retained with its VAT (credit to the commission receivable).
   *
   * @return commission + VAT
   */
  public BigDecimal commissionReceivable() {
    return commission.add(commissionVat);
  }
}
