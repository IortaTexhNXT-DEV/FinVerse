package com.iortatechnxt.brokerverse.nbreport.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The New Business dashboard (BRNB.012) of a company on a date.
 *
 * @param asOf date of the figures ("this month" is its month, the funnel runs from 1 January)
 * @param requests quotation requests, quotations and proposal requests by status
 * @param quotationsSent quotations and proposal slips sent to clients this month
 * @param awaitingClient quotations and proposals waiting for the client's answer
 * @param accounts accounts by stage (voided excluded)
 * @param overdue open work items past their stage SLA, per workflow
 * @param booked accounts booked this month
 * @param funnel quotation-to-booking funnel of the year to date
 * @param ageing open accounts by stage and age
 * @param production production against target per team this month
 */
public record NbDashboard(
    LocalDate asOf,
    List<StatusCount> requests,
    long quotationsSent,
    long awaitingClient,
    List<StatusCount> accounts,
    List<StatusCount> overdue,
    Booked booked,
    List<StatusCount> funnel,
    List<StageAgeing> ageing,
    List<UnitProduction> production) {

  /** Copies the lists. */
  public NbDashboard {
    requests = List.copyOf(requests);
    accounts = List.copyOf(accounts);
    overdue = List.copyOf(overdue);
    funnel = List.copyOf(funnel);
    ageing = List.copyOf(ageing);
    production = List.copyOf(production);
  }

  /**
   * A count by status, stage or step.
   *
   * @param group what is counted (e.g. QUOTATION, PROPOSAL, NB_ACCOUNT)
   * @param code status, stage or step code (the drill-down filter)
   * @param label label
   * @param count count
   */
  public record StatusCount(String group, String code, String label, long count) {}

  /**
   * Accounts booked in the month.
   *
   * @param count booking invoices
   * @param premium basic premium of every invoice booked (PHP, net of endorsements)
   * @param commission commission (PHP)
   */
  public record Booked(long count, BigDecimal premium, BigDecimal commission) {}

  /**
   * Open accounts of one stage by the time spent in it.
   *
   * @param stage stage code
   * @param label stage name
   * @param upToOneDay entered less than a day ago
   * @param upToThreeDays one to three days
   * @param upToSevenDays three to seven days
   * @param overSevenDays more than seven days
   * @param overdue past the stage SLA
   */
  public record StageAgeing(
      String stage,
      String label,
      long upToOneDay,
      long upToThreeDays,
      long upToSevenDays,
      long overSevenDays,
      long overdue) {}
}
