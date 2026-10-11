package com.iortatechnxt.brokerverse.tax.domain;

import java.time.LocalDate;

/**
 * Identity of a new return.
 *
 * @param companyId company
 * @param returnNo return number
 * @param formCode form code
 * @param worksheet computing worksheet
 * @param period filing period
 * @param dueDate due date
 */
public record ReturnHeader(
    Long companyId,
    String returnNo,
    String formCode,
    WorksheetKind worksheet,
    TaxPeriod period,
    LocalDate dueDate) {}
