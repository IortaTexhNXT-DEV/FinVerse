package com.iortatechnxt.brokerverse.payables.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Port of Payables: the input VAT rate of supplier invoices, read from the tax code master of the
 * company (tax type VAT_INPUT), so no VAT rate is written into the platform. Implemented by the tax
 * module.
 */
public interface InputVatRates {

  /**
   * The input VAT rate of a company on a date.
   *
   * @param companyId company
   * @param date invoice date
   * @return rate in percent (e.g. 12)
   * @throws com.iortatechnxt.brokerverse.common.exception.BusinessRuleException NO_INPUT_VAT_CODE
   *     when the company has no active input VAT tax code on that date
   */
  BigDecimal ratePercent(Long companyId, LocalDate date);
}
