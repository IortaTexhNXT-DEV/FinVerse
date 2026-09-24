package com.iortatechnxt.brokerverse.quotation.api.dto;

import com.iortatechnxt.brokerverse.quotation.domain.QuotationVersion;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A version of a quotation (BRNB.020).
 *
 * @param versionNo version number
 * @param frozen submitted (read-only)
 * @param frozenBy submitter
 * @param frozenAt submission time
 * @param totalSumInsured total sum insured
 * @param grossPremium gross premium
 * @param createdBy author
 * @param createdAt created
 * @param updatedAt last change
 */
public record QuotationVersionResponse(
    int versionNo,
    boolean frozen,
    String frozenBy,
    Instant frozenAt,
    BigDecimal totalSumInsured,
    BigDecimal grossPremium,
    String createdBy,
    Instant createdAt,
    Instant updatedAt) {

  /**
   * Maps a version.
   *
   * @param v version
   * @return response
   */
  public static QuotationVersionResponse from(QuotationVersion v) {
    return new QuotationVersionResponse(
        v.getVersionNo(),
        v.isFrozen(),
        v.getFrozenBy(),
        v.getFrozenAt(),
        v.getTotalSumInsured(),
        v.getGrossPremium(),
        v.getCreatedBy(),
        v.getCreatedAt(),
        v.getUpdatedAt());
  }
}
