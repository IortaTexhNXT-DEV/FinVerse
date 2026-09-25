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
 * @param cpc2 CPC2 incentive on packaged Fire and Motor products (DIS 3.29.2)
 * @param cpc2Vat output VAT on the CPC2 incentive
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
    @Column(name = "net_due", nullable = false, precision = 19, scale = 2) BigDecimal netDue,
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal cpc2,
    @Column(name = "cpc2_vat", nullable = false, precision = 19, scale = 2) BigDecimal cpc2Vat) {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  /** Nothing. */
  public static final RemittanceAmounts NONE =
      new RemittanceAmounts(ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);

  /**
   * Amounts without CPC2 (the extraction adds it with {@link #withCpc2}).
   *
   * @param paidAr paid AR remitted
   * @param commission realized commission retained
   * @param commissionVat VAT on the commission
   * @param wtax withholding tax on the commission
   * @param dtip due to insurer reduced
   * @param incentive early-remittance incentive
   * @param incentiveVat output VAT on the incentive
   * @param netDue net due before the incentive
   */
  public RemittanceAmounts(
      BigDecimal paidAr,
      BigDecimal commission,
      BigDecimal commissionVat,
      BigDecimal wtax,
      BigDecimal dtip,
      BigDecimal incentive,
      BigDecimal incentiveVat,
      BigDecimal netDue) {
    this(
        paidAr, commission, commissionVat, wtax, dtip, incentive, incentiveVat, netDue, ZERO, ZERO);
  }

  /**
   * The same amounts with the CPC2 incentive of the line (DIS 3.29.2).
   *
   * @param amount CPC2 incentive
   * @param vat output VAT on it
   * @return new amounts
   */
  public RemittanceAmounts withCpc2(BigDecimal amount, BigDecimal vat) {
    return new RemittanceAmounts(
        paidAr,
        commission,
        commissionVat,
        wtax,
        dtip,
        incentive,
        incentiveVat,
        netDue,
        amount,
        vat);
  }

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
        netDue.add(other.netDue),
        cpc2.add(other.cpc2),
        cpc2Vat.add(other.cpc2Vat));
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
   * CPC2 incentive and its VAT, deducted from the payment (DIS 3.29.2).
   *
   * @return CPC2 + VAT
   */
  public BigDecimal cpc2Total() {
    return cpc2.add(cpc2Vat);
  }

  /**
   * What Disbursement pays the insurer before deductions.
   *
   * @return net due less the early incentive, the CPC2 incentive and their VAT
   */
  public BigDecimal payable() {
    return netDue.subtract(incentiveTotal()).subtract(cpc2Total());
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
