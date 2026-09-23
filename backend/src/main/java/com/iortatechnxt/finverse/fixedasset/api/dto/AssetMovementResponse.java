package com.iortatechnxt.finverse.fixedasset.api.dto;

import com.iortatechnxt.finverse.fixedasset.domain.AssetMovement;
import com.iortatechnxt.finverse.fixedasset.domain.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Asset movement view.
 *
 * @param id id
 * @param movementType type
 * @param movementDate date
 * @param fromBranchId sending / owning branch
 * @param toBranchId receiving branch
 * @param cost cost
 * @param accumulatedDepreciation accumulated depreciation
 * @param netBookValue net book value
 * @param proceeds proceeds
 * @param gainLoss gain (positive) or loss (negative)
 * @param reference reference
 * @param remarks remarks
 * @param batchNo journal
 * @param createdBy user
 */
public record AssetMovementResponse(
    Long id,
    MovementType movementType,
    LocalDate movementDate,
    Long fromBranchId,
    Long toBranchId,
    BigDecimal cost,
    BigDecimal accumulatedDepreciation,
    BigDecimal netBookValue,
    BigDecimal proceeds,
    BigDecimal gainLoss,
    String reference,
    String remarks,
    String batchNo,
    String createdBy) {

  /**
   * Maps an entity.
   *
   * @param m movement
   * @return response
   */
  public static AssetMovementResponse from(AssetMovement m) {
    return new AssetMovementResponse(
        m.getId(),
        m.getMovementType(),
        m.getMovementDate(),
        m.getFromBranchId(),
        m.getToBranchId(),
        m.getCost(),
        m.getAccumulatedDepreciation(),
        m.getNetBookValue(),
        m.getProceeds(),
        m.getGainLoss(),
        m.getReference(),
        m.getRemarks(),
        m.getBatchNo(),
        m.getCreatedBy());
  }
}
