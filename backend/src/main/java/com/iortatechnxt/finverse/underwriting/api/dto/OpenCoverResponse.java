package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.underwriting.domain.OpenCover;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Open cover view.
 *
 * @param id id
 * @param companyId company
 * @param branchId branch
 * @param openCoverNo number
 * @param productId product
 * @param productCode product code
 * @param customerCode client code
 * @param customerName client name
 * @param insuredName insured
 * @param periodFrom start
 * @param periodTo end
 * @param currency currency
 * @param limitPerShipment limit per shipment
 * @param annualLimit annual declarations limit
 * @param rate premium rate %
 * @param cargoDescription goods covered
 * @param recordStatus maker-checker status
 * @param createdBy creator
 * @param maker user who created or last maintained the record (unchanged by authorization)
 * @param authorizedBy checker
 */
public record OpenCoverResponse(
    Long id,
    Long companyId,
    Long branchId,
    String openCoverNo,
    Long productId,
    String productCode,
    String customerCode,
    String customerName,
    String insuredName,
    LocalDate periodFrom,
    LocalDate periodTo,
    String currency,
    BigDecimal limitPerShipment,
    BigDecimal annualLimit,
    BigDecimal rate,
    String cargoDescription,
    RecordStatus recordStatus,
    String createdBy,
    String maker,
    String authorizedBy) {

  /**
   * Maps an open cover (product and client loaded).
   *
   * @param c cover
   * @return response
   */
  public static OpenCoverResponse from(OpenCover c) {
    return new OpenCoverResponse(
        c.getId(),
        c.getCompanyId(),
        c.getBranchId(),
        c.getOpenCoverNo(),
        c.getProduct().getId(),
        c.getProduct().getCode(),
        c.getCustomer().getCode(),
        c.getCustomer().getName(),
        c.getInsuredName(),
        c.getPeriodFrom(),
        c.getPeriodTo(),
        c.getCurrency(),
        c.getLimitPerShipment(),
        c.getAnnualLimit(),
        c.getRate(),
        c.getCargoDescription(),
        c.getRecordStatus(),
        c.getCreatedBy(),
        c.getMaker(),
        c.getAuthorizedBy());
  }
}
