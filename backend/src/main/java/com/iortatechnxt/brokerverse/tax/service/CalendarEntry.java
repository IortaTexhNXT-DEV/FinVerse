package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.tax.domain.TaxAuthority;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import java.time.LocalDate;

/**
 * One filing obligation of the tax calendar: a form for a period, its due date and where it stands.
 *
 * @param formCode form
 * @param formName form name
 * @param authority authority
 * @param worksheet computing worksheet
 * @param period filing period
 * @param dueDate due date
 * @param tracked whether the return is managed in BrokerVerse
 * @param returnId live return id, null when not prepared
 * @param returnNo live return number
 * @param returnStatus NOT_PREPARED, DRAFT, FILED or PAID
 * @param dueState PAID, OVERDUE, DUE_SOON, UPCOMING or REMINDER (untracked forms)
 * @param daysToDue days from the reference date to the due date (negative when overdue)
 */
public record CalendarEntry(
    String formCode,
    String formName,
    TaxAuthority authority,
    WorksheetKind worksheet,
    TaxPeriod period,
    LocalDate dueDate,
    boolean tracked,
    Long returnId,
    String returnNo,
    String returnStatus,
    String dueState,
    long daysToDue) {

  /** Return not yet prepared. */
  public static final String NOT_PREPARED = "NOT_PREPARED";

  /** Due date passed and not paid. */
  public static final String OVERDUE = "OVERDUE";

  /** Due within the alert threshold and not paid. */
  public static final String DUE_SOON = "DUE_SOON";

  /** Due later than the alert threshold. */
  public static final String UPCOMING = "UPCOMING";

  /** Paid. */
  public static final String PAID = "PAID";

  /** Reminder only (form prepared outside BrokerVerse). */
  public static final String REMINDER = "REMINDER";
}
