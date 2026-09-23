package com.iortatechnxt.finverse.reserves.api.dto;

import com.iortatechnxt.finverse.reserves.domain.TakafulItem;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Takaful surplus of one policy (PGIBR074 FinVerse rule).
 *
 * @param policyNo policy
 * @param insuredName insured
 * @param businessLine line of business
 * @param productCode product
 * @param expiryDate expiry
 * @param gross gross contribution
 * @param discount discount
 * @param loading loading
 * @param commission commission
 * @param claims incurred claims
 * @param applicable applicable contribution
 * @param retakaful retakaful contribution
 * @param tax tax
 * @param payable surplus payable
 */
public record TakafulItemResponse(
    String policyNo,
    String insuredName,
    String businessLine,
    String productCode,
    LocalDate expiryDate,
    BigDecimal gross,
    BigDecimal discount,
    BigDecimal loading,
    BigDecimal commission,
    BigDecimal claims,
    BigDecimal applicable,
    BigDecimal retakaful,
    BigDecimal tax,
    BigDecimal payable) {

  /**
   * Maps an item.
   *
   * @param i item
   * @return response
   */
  public static TakafulItemResponse from(TakafulItem i) {
    return new TakafulItemResponse(
        i.policyNo(),
        i.insuredName(),
        i.key().businessLine(),
        i.key().productCode(),
        i.expiryDate(),
        i.gross(),
        i.discount(),
        i.loading(),
        i.commission(),
        i.claims(),
        i.applicable(),
        i.retakaful(),
        i.tax(),
        i.payable());
  }
}
