package com.iortatechnxt.finverse.investment.api.dto;

import com.iortatechnxt.finverse.investment.domain.InvestmentRun;
import com.iortatechnxt.finverse.investment.domain.RunType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Posted month-end run.
 *
 * @param id id
 * @param runType type
 * @param period period (YYYY-MM)
 * @param periodEnd value date
 * @param holdingCount holdings posted
 * @param totalAmount total posted
 * @param createdBy user
 * @param createdAt time
 */
public record RunResponse(
    Long id,
    RunType runType,
    String period,
    LocalDate periodEnd,
    int holdingCount,
    BigDecimal totalAmount,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps an entity.
   *
   * @param r run
   * @return response
   */
  public static RunResponse from(InvestmentRun r) {
    return new RunResponse(
        r.getId(),
        r.getRunType(),
        r.getPeriod(),
        r.getPeriodEnd(),
        r.getHoldingCount(),
        r.getTotalAmount(),
        r.getCreatedBy(),
        r.getCreatedAt());
  }
}
