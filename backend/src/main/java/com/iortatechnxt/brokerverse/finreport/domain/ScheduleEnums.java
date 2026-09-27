package com.iortatechnxt.brokerverse.finreport.domain;

/**
 * Enumerations of the account schedule engine (FRBS 3.2.0, Appendix A II-IV;
 * ACCOUNTING_DISBURSEMENT_DESIGN sections 10 and 12.1).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ScheduleEnums {

  private ScheduleEnums() {}

  /** Report family of Appendix A a schedule belongs to. */
  public enum ScheduleFamily {
    /** II. GARD - BDO Unibank bank format. */
    GARD,
    /** III. Subsidiaries accounting. */
    SUBSIDIARIES,
    /** IV. Schedules. */
    SCHEDULE,
    /** IV. Schedules with ageing. */
    AGING,
    /** Any other schedule. */
    OTHER
  }

  /** How the accounts of a schedule are chosen. */
  public enum SelectorKind {
    /** Account code prefixes: {@code 1210} takes 1210, 1210.01 and so on. */
    ACCOUNT_PREFIX,
    /** Report groups of the chart ({@code coa_account.report_group}). */
    REPORT_GROUP
  }

  /** The rows of a schedule. */
  public enum Grouping {
    /** One row per account. */
    ACCOUNT,
    /** One row per sub-ledger party. */
    PARTY,
    /** One row per party and document reference (open item). */
    DOCUMENT,
    /** One row per cost centre. */
    COST_CENTER,
    /** One row per branch. */
    BRANCH,
    /** One row per business line. */
    BUSINESS_LINE
  }

  /** The side shown as positive. */
  public enum Side {
    /** Debit balances positive (assets, expenses). */
    DEBIT,
    /** Credit balances positive (liabilities, equity, income). */
    CREDIT
  }

  /** What the main figure of a schedule is. */
  public enum Basis {
    /** The balance at the as-of date. */
    BALANCE,
    /** The movement of the period (from - as of). */
    MOVEMENT
  }

  /** The period a schedule is compared with. */
  public enum Comparative {
    /** No comparison. */
    NONE,
    /** The previous month (month-on-month analysis). */
    PREVIOUS_MONTH,
    /** The same date or period of the previous year (year-on-year analysis). */
    PREVIOUS_YEAR
  }

  /** Whether BDOI has confirmed the layout (AQ05). */
  public enum LayoutStatus {
    /** Draft definition, layout to confirm with FRBS. */
    TO_CONFIRM,
    /** Layout confirmed by FRBS. */
    CONFIRMED
  }

  /** A figure a schedule can show. */
  public enum Measure {
    /** Balance before the period. */
    OPENING,
    /** Debits of the period. */
    DEBITS,
    /** Credits of the period. */
    CREDITS,
    /** Net movement of the period on the schedule's side. */
    MOVEMENT,
    /** Balance at the as-of date; year to date for a movement schedule. */
    CLOSING,
    /** The main figure of the comparative period. */
    COMPARATIVE,
    /** Main figure less comparative. */
    VARIANCE,
    /** Variance as a percentage of the comparative. */
    VARIANCE_PCT
  }
}
