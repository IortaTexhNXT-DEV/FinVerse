package com.iortatechnxt.finverse.fixedasset.api.dto;

import com.iortatechnxt.finverse.fixedasset.domain.DepreciationRun;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Posted depreciation run.
 *
 * @param id id
 * @param period period (YYYY-MM)
 * @param periodEnd value date
 * @param assetCount assets charged
 * @param totalDepreciation total charge
 * @param createdBy user who posted
 * @param createdAt posting time
 */
public record DepreciationRunResponse(
    Long id,
    String period,
    LocalDate periodEnd,
    int assetCount,
    BigDecimal totalDepreciation,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps an entity.
   *
   * @param r run
   * @return response
   */
  public static DepreciationRunResponse from(DepreciationRun r) {
    return new DepreciationRunResponse(
        r.getId(),
        r.getPeriod(),
        r.getPeriodEnd(),
        r.getAssetCount(),
        r.getTotalDepreciation(),
        r.getCreatedBy(),
        r.getCreatedAt());
  }
}
