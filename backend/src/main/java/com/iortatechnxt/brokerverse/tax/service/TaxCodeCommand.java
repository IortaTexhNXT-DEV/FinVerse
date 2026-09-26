package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.tax.domain.PayeeClass;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Values of a tax code.
 *
 * @param companyId company
 * @param code code (the ATC for withholding codes; immutable after creation)
 * @param name name
 * @param taxType tax type
 * @param atc alphanumeric tax code (mandatory for EWT)
 * @param payeeClass payee class the ATC applies to (EWT)
 * @param rate rate in percent
 * @param glAccountCode GL account the tax is booked to
 * @param incomeNature nature of income printed on 2307 / QAP (EWT)
 * @param effectiveFrom first day of validity
 * @param effectiveTo last day of validity, null when open
 */
public record TaxCodeCommand(
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
    LocalDate effectiveTo) {}
