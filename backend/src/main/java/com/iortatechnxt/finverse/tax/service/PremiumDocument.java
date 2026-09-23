package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.tax.domain.TaxType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tax facts of one approved premium transaction (policy issue or endorsement), converted to base
 * currency at the exchange rate recorded on the transaction. Return premiums are negative.
 *
 * @param sourceType POLICY (original issue) or ENDORSEMENT
 * @param policyId policy id (drill-down)
 * @param documentNo policy or endorsement number
 * @param date approval (accounting) date
 * @param customerCode client party code
 * @param customerName client name
 * @param intermediaryCode agent / broker code, null for direct business
 * @param intermediaryName agent / broker name
 * @param businessLine line of business
 * @param premium company's net premium (the base of every premium tax)
 * @param vat output VAT
 * @param dst documentary stamp tax
 * @param lgt local government tax
 * @param fst fire service tax
 * @param premiumTax premium (percentage) tax
 * @param commission commission accrued
 * @param withholding tax withheld on the commission
 */
public record PremiumDocument(
    String sourceType,
    Long policyId,
    String documentNo,
    LocalDate date,
    String customerCode,
    String customerName,
    String intermediaryCode,
    String intermediaryName,
    String businessLine,
    BigDecimal premium,
    BigDecimal vat,
    BigDecimal dst,
    BigDecimal lgt,
    BigDecimal fst,
    BigDecimal premiumTax,
    BigDecimal commission,
    BigDecimal withholding) {

  /**
   * Amount of a premium levy.
   *
   * @param type DST, PREMIUM_TAX, LGT or FST
   * @return tax amount
   */
  public BigDecimal levy(TaxType type) {
    return switch (type) {
      case DST -> dst;
      case LGT -> lgt;
      case FST -> fst;
      case PREMIUM_TAX -> premiumTax;
      default -> throw new IllegalArgumentException("Not a premium levy: " + type);
    };
  }
}
