package com.iortatechnxt.finverse.tax.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.tax.domain.PayeeClass;
import com.iortatechnxt.finverse.tax.domain.TaxCode;
import com.iortatechnxt.finverse.tax.domain.TaxType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tax code.
 *
 * @param id id
 * @param companyId company
 * @param code code
 * @param name name
 * @param taxType tax type
 * @param atc ATC
 * @param payeeClass payee class
 * @param rate rate in percent
 * @param glAccountCode GL account
 * @param incomeNature nature of income
 * @param effectiveFrom first day of validity
 * @param effectiveTo last day of validity
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record TaxCodeResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    TaxType taxType,
    String atc,
    PayeeClass payeeClass,
    BigDecimal rate,
    String glAccountCode,
    String incomeNature,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param c tax code
   * @return response
   */
  public static TaxCodeResponse from(TaxCode c) {
    return new TaxCodeResponse(
        c.getId(),
        c.getCompanyId(),
        c.getCode(),
        c.getName(),
        c.getTaxType(),
        c.getAtc(),
        c.getPayeeClass(),
        c.getRate(),
        c.getGlAccountCode(),
        c.getIncomeNature(),
        c.getEffectiveFrom(),
        c.getEffectiveTo(),
        c.getRecordStatus(),
        c.getUpdatedBy() == null ? c.getCreatedBy() : c.getUpdatedBy(),
        c.getAuthorizedBy());
  }
}
