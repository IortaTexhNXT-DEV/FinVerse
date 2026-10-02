package com.iortatechnxt.brokerverse.nbreport.api.dto;

import com.iortatechnxt.brokerverse.nbreport.service.NbDashboard;
import com.iortatechnxt.brokerverse.nbreport.service.NbDashboard.Booked;
import com.iortatechnxt.brokerverse.nbreport.service.NbDashboard.StageAgeing;
import com.iortatechnxt.brokerverse.nbreport.service.NbDashboard.StatusCount;
import java.time.LocalDate;
import java.util.List;

/**
 * The New Business dashboard (BRNB.012).
 *
 * @param asOf date of the figures
 * @param requests requests, quotations and proposal requests by status
 * @param quotationsSent quotations and proposal slips sent this month
 * @param awaitingClient quotations and proposals waiting for the client
 * @param accounts accounts by stage
 * @param overdue work items past their SLA per workflow
 * @param booked bookings of the month
 * @param funnel quotation-to-booking funnel (year to date)
 * @param ageing open accounts by stage and age
 * @param production production against target per team (month)
 */
public record NbDashboardResponse(
    LocalDate asOf,
    List<StatusCount> requests,
    long quotationsSent,
    long awaitingClient,
    List<StatusCount> accounts,
    List<StatusCount> overdue,
    Booked booked,
    List<StatusCount> funnel,
    List<StageAgeing> ageing,
    List<UnitProductionDto> production) {

  /**
   * Maps the dashboard.
   *
   * @param d dashboard
   * @return response
   */
  public static NbDashboardResponse from(NbDashboard d) {
    return new NbDashboardResponse(
        d.asOf(),
        d.requests(),
        d.quotationsSent(),
        d.awaitingClient(),
        d.accounts(),
        d.overdue(),
        d.booked(),
        d.funnel(),
        d.ageing(),
        d.production().stream().map(UnitProductionDto::from).toList());
  }
}
