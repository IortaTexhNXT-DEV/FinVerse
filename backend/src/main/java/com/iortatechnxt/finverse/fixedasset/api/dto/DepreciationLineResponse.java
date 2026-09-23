package com.iortatechnxt.finverse.fixedasset.api.dto;

import com.iortatechnxt.finverse.fixedasset.domain.DepreciationLine;
import com.iortatechnxt.finverse.fixedasset.domain.FixedAsset;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationService.Proposal;
import java.math.BigDecimal;

/**
 * A depreciation charge (proposed in a preview or posted in a run).
 *
 * @param assetId asset
 * @param tagNo tag number
 * @param description description
 * @param categoryCode category
 * @param branchId branch
 * @param costCenter cost centre
 * @param months months charged
 * @param amount charge
 * @param accumulatedAfter accumulated depreciation after the charge
 * @param netBookValueAfter net book value after the charge
 * @param batchNo posting journal (posted lines only)
 */
public record DepreciationLineResponse(
    Long assetId,
    String tagNo,
    String description,
    String categoryCode,
    Long branchId,
    String costCenter,
    int months,
    BigDecimal amount,
    BigDecimal accumulatedAfter,
    BigDecimal netBookValueAfter,
    String batchNo) {

  /**
   * Maps a proposal.
   *
   * @param p proposal
   * @return response
   */
  public static DepreciationLineResponse from(Proposal p) {
    FixedAsset a = p.asset();
    return new DepreciationLineResponse(
        a.getId(),
        a.getTagNo(),
        a.getDescription(),
        a.getCategory().getCode(),
        a.getBranchId(),
        a.getCostCenter(),
        p.months(),
        p.amount(),
        p.accumulatedAfter(),
        p.netBookValueAfter(),
        null);
  }

  /**
   * Maps a posted line.
   *
   * @param l line
   * @return response
   */
  public static DepreciationLineResponse from(DepreciationLine l) {
    FixedAsset a = l.getAsset();
    return new DepreciationLineResponse(
        a.getId(),
        a.getTagNo(),
        a.getDescription(),
        a.getCategory().getCode(),
        l.getBranchId(),
        a.getCostCenter(),
        l.getMonths(),
        l.getAmount(),
        l.getAccumulatedAfter(),
        l.getNetBookValueAfter(),
        l.getBatchNo());
  }
}
