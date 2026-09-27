package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Broker commission of a booked invoice (BRNB.027/100): rate and amount, VAT on the commission and
 * the withholding tax the insurer deducts from it. Amounts are negative on return invoices.
 *
 * @param rate commission rate in percent
 * @param commission commission amount
 * @param vatOnCommission output VAT on the commission
 * @param wtaxRate withholding tax rate in percent
 * @param wtaxAmount withholding tax on the commission
 */
@Embeddable
public record CommissionTerms(
    @Column(name = "commission_rate", nullable = false, precision = 19, scale = 8) BigDecimal rate,
    @Column(name = "commission", nullable = false, precision = 19, scale = 2) BigDecimal commission,
    @Column(name = "vat_on_commission", nullable = false, precision = 19, scale = 2)
        BigDecimal vatOnCommission,
    @Column(name = "wtax_rate", nullable = false, precision = 19, scale = 8) BigDecimal wtaxRate,
    @Column(name = "wtax_amount", nullable = false, precision = 19, scale = 2)
        BigDecimal wtaxAmount) {

  private static final int SCALE = 2;
  private static final int RATE_SCALE = 8;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /** Rates default to zero; amounts at scale 2. */
  public CommissionTerms {
    rate = (rate == null ? BigDecimal.ZERO : rate).setScale(RATE_SCALE, RoundingMode.HALF_UP);
    wtaxRate =
        (wtaxRate == null ? BigDecimal.ZERO : wtaxRate).setScale(RATE_SCALE, RoundingMode.HALF_UP);
    commission = money(commission);
    vatOnCommission = money(vatOnCommission);
    wtaxAmount = money(wtaxAmount);
  }

  /**
   * Terms with the withholding tax computed on the commission.
   *
   * @param rate commission rate in percent
   * @param commission commission
   * @param vatOnCommission VAT on the commission
   * @param wtaxRate withholding tax rate in percent
   * @return terms
   */
  public static CommissionTerms of(
      BigDecimal rate, BigDecimal commission, BigDecimal vatOnCommission, BigDecimal wtaxRate) {
    BigDecimal base = commission == null ? BigDecimal.ZERO : commission;
    BigDecimal tax =
        wtaxRate == null
            ? BigDecimal.ZERO
            : base.multiply(wtaxRate).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
    return new CommissionTerms(rate, commission, vatOnCommission, wtaxRate, tax);
  }

  /**
   * Commission plus its VAT: what the insurer owes the broker before withholding.
   *
   * @return receivable
   */
  public BigDecimal receivable() {
    return commission.add(vatOnCommission);
  }

  /**
   * Commission plus VAT less withholding tax: the net amount of the service invoice.
   *
   * @return net amount
   */
  public BigDecimal net() {
    return receivable().subtract(wtaxAmount);
  }

  /**
   * Amounts times a factor (share, refund factor); the rates are kept and the withholding tax is
   * recomputed.
   *
   * @param factor factor
   * @return scaled terms
   */
  public CommissionTerms times(BigDecimal factor) {
    return of(
        rate,
        commission.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP),
        vatOnCommission.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP),
        wtaxRate);
  }

  /**
   * The same terms with negative amounts (return invoice).
   *
   * @return negated terms
   */
  public CommissionTerms negate() {
    return new CommissionTerms(
        rate, commission.negate(), vatOnCommission.negate(), wtaxRate, wtaxAmount.negate());
  }

  private static BigDecimal money(BigDecimal amount) {
    return amount == null
        ? BigDecimal.ZERO.setScale(SCALE)
        : amount.setScale(SCALE, RoundingMode.HALF_UP);
  }
}
