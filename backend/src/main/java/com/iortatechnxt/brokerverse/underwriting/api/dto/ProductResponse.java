package com.iortatechnxt.brokerverse.underwriting.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.domain.TaxRates;
import com.iortatechnxt.brokerverse.underwriting.domain.UprBasis;
import java.math.BigDecimal;

/**
 * Product view.
 *
 * @param id id
 * @param companyId company
 * @param code code
 * @param name name
 * @param businessLine line of business
 * @param defaultCommissionRate default commission %
 * @param uprBasis UPR basis
 * @param dstRate DST %
 * @param vatRate VAT %
 * @param lgtRate LGT %
 * @param fstRate FST %
 * @param premiumTaxRate premium tax %
 * @param policyFee policy fee
 * @param openCoverAllowed open covers allowed
 * @param recordStatus maker-checker status
 * @param createdBy maker
 * @param updatedBy last modifier
 * @param authorizedBy checker
 */
public record ProductResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    String businessLine,
    BigDecimal defaultCommissionRate,
    UprBasis uprBasis,
    BigDecimal dstRate,
    BigDecimal vatRate,
    BigDecimal lgtRate,
    BigDecimal fstRate,
    BigDecimal premiumTaxRate,
    BigDecimal policyFee,
    boolean openCoverAllowed,
    RecordStatus recordStatus,
    String createdBy,
    String updatedBy,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param p product
   * @return response
   */
  public static ProductResponse from(Product p) {
    TaxRates t = p.taxRates();
    return new ProductResponse(
        p.getId(),
        p.getCompanyId(),
        p.getCode(),
        p.getName(),
        p.getBusinessLine(),
        p.getDefaultCommissionRate(),
        p.getUprBasis(),
        t.dst(),
        t.vat(),
        t.lgt(),
        t.fst(),
        t.premiumTax(),
        p.getPolicyFee(),
        p.isOpenCoverAllowed(),
        p.getRecordStatus(),
        p.getCreatedBy(),
        p.getUpdatedBy(),
        p.getAuthorizedBy());
  }
}
