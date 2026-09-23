package com.iortatechnxt.finverse.organization.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.organization.domain.Company;

/**
 * Company view.
 *
 * @param id id
 * @param code code
 * @param name name
 * @param baseCurrency base currency
 * @param taxId tax id
 * @param address address
 * @param fiscalYearStartMonth fiscal year start month
 * @param backValueDays back value days
 * @param forwardValueDays forward value days
 * @param retainedEarningsAccount retained earnings account code
 * @param recordStatus maker-checker status
 * @param createdBy maker
 * @param authorizedBy checker
 */
public record CompanyResponse(
    Long id,
    String code,
    String name,
    String baseCurrency,
    String taxId,
    String address,
    int fiscalYearStartMonth,
    int backValueDays,
    int forwardValueDays,
    String retainedEarningsAccount,
    RecordStatus recordStatus,
    String createdBy,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param c company
   * @return response
   */
  public static CompanyResponse from(Company c) {
    return new CompanyResponse(
        c.getId(),
        c.getCode(),
        c.getName(),
        c.getBaseCurrency(),
        c.getTaxId(),
        c.getAddress(),
        c.getFiscalYearStartMonth(),
        c.getBackValueDays(),
        c.getForwardValueDays(),
        c.getRetainedEarningsAccount(),
        c.getRecordStatus(),
        c.getCreatedBy(),
        c.getAuthorizedBy());
  }
}
