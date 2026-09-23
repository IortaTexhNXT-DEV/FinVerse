package com.iortatechnxt.finverse.reserves.domain;

import java.time.LocalDate;

/**
 * Unearned premium of one premium transaction (policy issue or endorsement) at a valuation date.
 *
 * @param policyId policy id
 * @param endorsementNo 0 for the original issue, else the endorsement number
 * @param documentNo policy or endorsement number
 * @param kind NEW or the endorsement type
 * @param key reporting unit (branch, line of business, product, channel)
 * @param basis earning basis (DAYS_365, TWENTY_FOURTHS, EIGHTHS)
 * @param coverFrom first day of cover
 * @param coverTo last day of cover
 * @param approvalDate approval (accounting) date
 * @param units earning units at the valuation date
 * @param written amounts as written
 * @param unearned unearned part of the written amounts
 */
public record UprItem(
    Long policyId,
    int endorsementNo,
    String documentNo,
    String kind,
    ReserveKey key,
    String basis,
    LocalDate coverFrom,
    LocalDate coverTo,
    LocalDate approvalDate,
    EarningUnits units,
    PremiumAmounts written,
    PremiumAmounts unearned) {

  /**
   * Earned part of the written amounts.
   *
   * @return written − unearned
   */
  public PremiumAmounts earned() {
    return written.minus(unearned);
  }
}
