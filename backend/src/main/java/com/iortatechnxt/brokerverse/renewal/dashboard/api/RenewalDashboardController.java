package com.iortatechnxt.brokerverse.renewal.dashboard.api;

import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardDrillService;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardFilter;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.RenewalDashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Renewal dashboard (BDOI Renewal FRS FRRN.002.02) and the drill-down of its figures, within
 * the user's data scope. Export and print of a drill-down run through the Report Centre (report
 * {@code RNW-DASHBOARD}).
 */
@RestController
@RequestMapping("/api/v1/renewal/dashboard")
@PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_REVIEW','RNW_PROCESS','RNW_REPORT_VIEW')")
public class RenewalDashboardController {

  private final RenewalDashboardService dashboard;
  private final DashboardDrillService drill;

  /**
   * Creates the controller.
   *
   * @param dashboard dashboard
   * @param drill drill-down
   */
  public RenewalDashboardController(
      RenewalDashboardService dashboard, DashboardDrillService drill) {
    this.dashboard = dashboard;
    this.drill = drill;
  }

  /**
   * The dashboard.
   *
   * @param companyId company
   * @param from period from (default first day of the month)
   * @param to period to (default last day of the month)
   * @param segment market segment
   * @param officer Account Officer
   * @param top size of the Biggest Open Deals
   * @return figures
   */
  @GetMapping
  public RenewalDashboardService.Dashboard dashboard(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String segment,
      @RequestParam(required = false) String officer,
      @RequestParam(required = false) Integer top) {
    return dashboard.dashboard(
        new DashboardFilter(companyId, from, to, segment, officer, null), top);
  }

  /**
   * The accounts of a figure.
   *
   * @param companyId company
   * @param metric metric key of the figure
   * @param from period from
   * @param to period to
   * @param segment market segment
   * @param officer Account Officer
   * @param top size of the Biggest Open Deals
   * @return columns and rows
   */
  @GetMapping("/drill")
  public DashboardDrillService.Drill drill(
      @RequestParam Long companyId,
      @RequestParam String metric,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String segment,
      @RequestParam(required = false) String officer,
      @RequestParam(required = false) Integer top) {
    return drill.drill(
        new DashboardFilter(companyId, from, to, segment, officer, null), metric, top);
  }
}
