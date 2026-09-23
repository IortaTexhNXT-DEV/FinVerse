package com.iortatechnxt.finverse.fixedasset.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.fixedasset.domain.AssetStatus;
import com.iortatechnxt.finverse.fixedasset.domain.DepreciationMethod;
import com.iortatechnxt.finverse.fixedasset.domain.FixedAsset;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fixed asset view.
 *
 * @param id id
 * @param companyId company
 * @param branchId branch
 * @param categoryId category
 * @param categoryCode category code
 * @param categoryName category name
 * @param tagNo tag number
 * @param description description
 * @param costCenter cost centre
 * @param supplierCode supplier
 * @param acquisitionDate acquisition date
 * @param acquisitionCost cost
 * @param residualValue residual value
 * @param depreciationMethod method
 * @param usefulLifeMonths useful life
 * @param location location
 * @param custodian custodian
 * @param settlementAccount capitalization credit account
 * @param takeOn take-on flag
 * @param capitalizationDate capitalization / take-on date
 * @param accumulatedDepreciation accumulated depreciation
 * @param netBookValue net book value
 * @param monthsDepreciated months depreciated
 * @param lastDepreciationPeriod last period depreciated
 * @param status life-cycle status
 * @param capitalizationBatchNo capitalization journal
 * @param disposalDate disposal date
 * @param recordStatus maker-checker status
 * @param createdBy maker
 * @param authorizedBy checker
 */
public record FixedAssetResponse(
    Long id,
    Long companyId,
    Long branchId,
    Long categoryId,
    String categoryCode,
    String categoryName,
    String tagNo,
    String description,
    String costCenter,
    String supplierCode,
    LocalDate acquisitionDate,
    BigDecimal acquisitionCost,
    BigDecimal residualValue,
    DepreciationMethod depreciationMethod,
    int usefulLifeMonths,
    String location,
    String custodian,
    String settlementAccount,
    boolean takeOn,
    LocalDate capitalizationDate,
    BigDecimal accumulatedDepreciation,
    BigDecimal netBookValue,
    int monthsDepreciated,
    String lastDepreciationPeriod,
    AssetStatus status,
    String capitalizationBatchNo,
    LocalDate disposalDate,
    RecordStatus recordStatus,
    String createdBy,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param a asset
   * @return response
   */
  public static FixedAssetResponse from(FixedAsset a) {
    return new FixedAssetResponse(
        a.getId(),
        a.getCompanyId(),
        a.getBranchId(),
        a.getCategory().getId(),
        a.getCategory().getCode(),
        a.getCategory().getName(),
        a.getTagNo(),
        a.getDescription(),
        a.getCostCenter(),
        a.getSupplierCode(),
        a.getAcquisitionDate(),
        a.getAcquisitionCost(),
        a.getResidualValue(),
        a.getDepreciationMethod(),
        a.getUsefulLifeMonths(),
        a.getLocation(),
        a.getCustodian(),
        a.getSettlementAccount(),
        a.isTakeOn(),
        a.getCapitalizationDate(),
        a.getAccumulatedDepreciation(),
        a.netBookValue(),
        a.getMonthsDepreciated(),
        a.getLastDepreciationPeriod(),
        a.getStatus(),
        a.getCapitalizationBatchNo(),
        a.getDisposalDate(),
        a.getRecordStatus(),
        a.getCreatedBy(),
        a.getAuthorizedBy());
  }
}
