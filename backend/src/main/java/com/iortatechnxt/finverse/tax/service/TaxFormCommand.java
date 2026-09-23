package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.tax.domain.FilingFrequency;
import com.iortatechnxt.finverse.tax.domain.TaxAuthority;
import com.iortatechnxt.finverse.tax.domain.WorksheetKind;
import java.time.LocalDate;

/**
 * Values of a tax form of the filing calendar.
 *
 * @param companyId company
 * @param code form code (immutable after creation)
 * @param name name
 * @param authority authority
 * @param frequency filing frequency
 * @param worksheet computing worksheet (NONE for reminder-only forms)
 * @param dueMonthsAfter due-date month offset after the period-end month
 * @param dueDay due day of month (31 = last day)
 * @param payableAccountCode tax payable account cleared by the remittance
 * @param creditAccountCode credit account applied on remittance (input VAT), null when none
 * @param trackFiling whether returns and alerts are managed in FinVerse
 * @param effectiveFrom first period start the calendar tracks
 */
public record TaxFormCommand(
    Long companyId,
    String code,
    String name,
    TaxAuthority authority,
    FilingFrequency frequency,
    WorksheetKind worksheet,
    int dueMonthsAfter,
    int dueDay,
    String payableAccountCode,
    String creditAccountCode,
    boolean trackFiling,
    LocalDate effectiveFrom) {}
