package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import java.math.BigDecimal;

/**
 * Commission of an invoice.
 *
 * @param rate commission rate, percent
 * @param commission commission
 * @param vatOnCommission VAT on the commission
 * @param wtaxRate withholding tax rate, percent
 * @param wtaxAmount withholding tax
 * @param net commission plus VAT less withholding tax
 */
public record CommissionDto(
    BigDecimal rate,
    BigDecimal commission,
    BigDecimal vatOnCommission,
    BigDecimal wtaxRate,
    BigDecimal wtaxAmount,
    BigDecimal net) {

  /**
   * Maps commission terms.
   *
   * @param c terms
   * @return DTO
   */
  public static CommissionDto from(CommissionTerms c) {
    return new CommissionDto(
        c.rate(), c.commission(), c.vatOnCommission(), c.wtaxRate(), c.wtaxAmount(), c.net());
  }
}
