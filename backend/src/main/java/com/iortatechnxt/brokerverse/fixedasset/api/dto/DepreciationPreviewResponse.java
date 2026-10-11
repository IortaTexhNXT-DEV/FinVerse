package com.iortatechnxt.brokerverse.fixedasset.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Depreciation of a period: the posted run when it exists, otherwise the proposal.
 *
 * @param period period (YYYY-MM)
 * @param posted whether the period is already posted
 * @param run posted run (null when not posted)
 * @param total total charge
 * @param lines charges per asset
 */
public record DepreciationPreviewResponse(
    String period,
    boolean posted,
    DepreciationRunResponse run,
    BigDecimal total,
    List<DepreciationLineResponse> lines) {

  /** Canonical constructor copying the lines. */
  public DepreciationPreviewResponse {
    lines = List.copyOf(lines);
  }

  /**
   * Builds a preview from lines, totalling them.
   *
   * @param period period
   * @param run posted run or null
   * @param lines lines
   * @return response
   */
  public static DepreciationPreviewResponse of(
      String period, DepreciationRunResponse run, List<DepreciationLineResponse> lines) {
    BigDecimal total =
        lines.stream()
            .map(DepreciationLineResponse::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new DepreciationPreviewResponse(period, run != null, run, total, lines);
  }
}
