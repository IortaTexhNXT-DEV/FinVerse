package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.service.CashieringDashboardService;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringDashboardService.Group;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The Cashiering dashboard (FRS.CSH.01.03): count and amount of every item. */
@RestController
@RequestMapping("/api/v1/cashiering/dashboard")
public class CashieringDashboardController {

  private final CashieringDashboardService dashboard;

  /**
   * Creates the controller.
   *
   * @param dashboard dashboard
   */
  public CashieringDashboardController(CashieringDashboardService dashboard) {
    this.dashboard = dashboard;
  }

  /**
   * The dashboard of a company, counted now.
   *
   * @param companyId company
   * @return groups with their items
   */
  @GetMapping
  @PreAuthorize(CashAccess.VIEW)
  public List<Group> dashboard(@RequestParam Long companyId) {
    return dashboard.dashboard(companyId);
  }
}
