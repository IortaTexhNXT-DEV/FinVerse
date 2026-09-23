package com.iortatechnxt.finverse.tax.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One income payment subject to creditable (expanded) withholding tax: a supplier invoice or a
 * commission accrual, in base currency.
 *
 * @param partyCode payee party code
 * @param atc alphanumeric tax code
 * @param incomeNature nature of the income payment (printed on 2307 and the QAP)
 * @param date date the income became payable (invoice or commission accrual date)
 * @param income income payment (tax base)
 * @param tax tax withheld
 */
public record WithholdingEntry(
    String partyCode,
    String atc,
    String incomeNature,
    LocalDate date,
    BigDecimal income,
    BigDecimal tax) {}
