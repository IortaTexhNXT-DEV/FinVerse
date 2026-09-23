package com.iortatechnxt.finverse.claims.api.dto;

import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.EstimateSide;
import com.iortatechnxt.finverse.claims.domain.MovementKind;
import com.iortatechnxt.finverse.claims.domain.MovementLine;
import com.iortatechnxt.finverse.claims.domain.MovementSource;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A line of the claim movement history.
 *
 * @param id id
 * @param movementDate accounting date
 * @param kind estimate change or paid
 * @param side payment or recovery
 * @param costType loss or expense
 * @param estimateType Reports Book type 1 to 4
 * @param amount100 amount at 100 %
 * @param amount company share
 * @param baseAmount company share in base currency
 * @param currency currency
 * @param reference movement reference
 * @param sourceType producing document type
 * @param journalBatchNo journal
 * @param narration narration
 */
public record MovementResponse(
    Long id,
    LocalDate movementDate,
    MovementKind kind,
    EstimateSide side,
    CostType costType,
    int estimateType,
    BigDecimal amount100,
    BigDecimal amount,
    BigDecimal baseAmount,
    String currency,
    String reference,
    MovementSource sourceType,
    String journalBatchNo,
    String narration) {

  /**
   * Maps a movement line.
   *
   * @param l line
   * @return response
   */
  public static MovementResponse from(MovementLine l) {
    return new MovementResponse(
        l.getId(),
        l.getMovementDate(),
        l.getKind(),
        l.getSide(),
        l.getCostType(),
        l.getEstimateType(),
        l.getAmount100(),
        l.getAmount(),
        l.getBaseAmount(),
        l.getCurrency(),
        l.getReference(),
        l.getSourceType(),
        l.getJournalBatchNo(),
        l.getNarration());
  }
}
