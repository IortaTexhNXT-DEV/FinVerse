package com.iortatechnxt.brokerverse.tax.api.dto;

import com.iortatechnxt.brokerverse.tax.domain.TaxAuthority;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.CalendarEntry;
import java.time.LocalDate;

/**
 * Tax calendar entry.
 *
 * @param formCode form
 * @param formName form name
 * @param authority authority
 * @param worksheet worksheet
 * @param periodStart period start
 * @param periodEnd period end
 * @param periodLabel period label
 * @param dueDate due date
 * @param tracked managed in BrokerVerse
 * @param returnId live return id
 * @param returnNo live return number
 * @param returnStatus NOT_PREPARED, DRAFT, FILED or PAID
 * @param dueState PAID, OVERDUE, DUE_SOON, UPCOMING or REMINDER
 * @param daysToDue days to the due date (negative when overdue)
 */
public record CalendarEntryResponse(
    String formCode,
    String formName,
    TaxAuthority authority,
    WorksheetKind worksheet,
    LocalDate periodStart,
    LocalDate periodEnd,
    String periodLabel,
    LocalDate dueDate,
    boolean tracked,
    Long returnId,
    String returnNo,
    String returnStatus,
    String dueState,
    long daysToDue) {

  /**
   * Maps an entry.
   *
   * @param e entry
   * @return response
   */
  public static CalendarEntryResponse from(CalendarEntry e) {
    return new CalendarEntryResponse(
        e.formCode(),
        e.formName(),
        e.authority(),
        e.worksheet(),
        e.period().from(),
        e.period().to(),
        e.period().label(),
        e.dueDate(),
        e.tracked(),
        e.returnId(),
        e.returnNo(),
        e.returnStatus(),
        e.dueState(),
        e.daysToDue());
  }
}
