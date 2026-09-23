package com.iortatechnxt.finverse.dashboard.api;

import com.iortatechnxt.finverse.dashboard.service.DashboardService;
import com.iortatechnxt.finverse.dashboard.service.DashboardSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Executive dashboard API. */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

  private final DashboardService service;

  /**
   * Creates the controller.
   *
   * @param service dashboard service
   */
  public DashboardController(DashboardService service) {
    this.service = service;
  }

  /**
   * Returns the dashboard summary.
   *
   * @param companyId company
   * @param branchId optional branch
   * @return summary
   */
  @GetMapping
  @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
  public DashboardSummary summary(
      @RequestParam Long companyId, @RequestParam(required = false) Long branchId) {
    return service.summary(companyId, branchId);
  }
}
