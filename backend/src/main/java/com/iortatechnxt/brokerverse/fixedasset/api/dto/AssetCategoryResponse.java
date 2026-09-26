package com.iortatechnxt.brokerverse.fixedasset.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetCategory;
import com.iortatechnxt.brokerverse.fixedasset.domain.DepreciationMethod;
import java.math.BigDecimal;

/**
 * Asset category view.
 *
 * @param id id
 * @param companyId company
 * @param code code
 * @param name name
 * @param assetAccount cost account
 * @param accumulatedDepreciationAccount accumulated depreciation account
 * @param depreciationExpenseAccount depreciation expense account
 * @param depreciationMethod method
 * @param usefulLifeMonths useful life
 * @param residualPercent residual %
 * @param recordStatus maker-checker status
 * @param createdBy creator
 * @param maker user who created or last maintained the record (unchanged by authorization)
 * @param authorizedBy checker
 */
public record AssetCategoryResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    String assetAccount,
    String accumulatedDepreciationAccount,
    String depreciationExpenseAccount,
    DepreciationMethod depreciationMethod,
    int usefulLifeMonths,
    BigDecimal residualPercent,
    RecordStatus recordStatus,
    String createdBy,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param c category
   * @return response
   */
  public static AssetCategoryResponse from(AssetCategory c) {
    return new AssetCategoryResponse(
        c.getId(),
        c.getCompanyId(),
        c.getCode(),
        c.getName(),
        c.getAssetAccount(),
        c.getAccumulatedDepreciationAccount(),
        c.getDepreciationExpenseAccount(),
        c.getDepreciationMethod(),
        c.getUsefulLifeMonths(),
        c.getResidualPercent(),
        c.getRecordStatus(),
        c.getCreatedBy(),
        c.getMaker(),
        c.getAuthorizedBy());
  }
}
