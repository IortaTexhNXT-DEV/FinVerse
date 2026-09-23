package com.iortatechnxt.finverse.dashboard.api;

import com.iortatechnxt.finverse.dashboard.service.BudgetWidget;
import com.iortatechnxt.finverse.dashboard.service.CashWidget;
import com.iortatechnxt.finverse.dashboard.service.ClaimsWidget;
import com.iortatechnxt.finverse.dashboard.service.CollectionsWidget;
import com.iortatechnxt.finverse.dashboard.service.DashboardService;
import com.iortatechnxt.finverse.dashboard.service.DashboardSummary;
import com.iortatechnxt.finverse.dashboard.service.LedgerDashboardService;
import com.iortatechnxt.finverse.dashboard.service.OperationsDashboardService;
import com.iortatechnxt.finverse.dashboard.service.PayablesWidget;
import com.iortatechnxt.finverse.dashboard.service.PremiumWidget;
import com.iortatechnxt.finverse.dashboard.service.WorkloadWidget;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Executive dashboard API: the headline summary and one endpoint per widget, so every widget loads
 * and fails independently. Widgets take an optional reference date ({@code asOf}, default today)
 * and an optional branch.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

  private static final String VIEW = "hasAuthority('DASHBOARD_VIEW')";

  private final DashboardService service;
  private final LedgerDashboardService ledger;
  private final OperationsDashboardService operations;

  /**
   * Creates the controller.
   *
   * @param service dashboard summary
   * @param ledger ledger-based widgets
   * @param operations widgets fed by business modules
   */
  public DashboardController(
      DashboardService service,
      LedgerDashboardService ledger,
      OperationsDashboardService operations) {
    this.service = service;
    this.ledger = ledger;
    this.operations = operations;
  }

  /**
   * Returns the dashboard summary.
   *
   * @param companyId company
   * @param branchId optional branch
   * @return summary
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public DashboardSummary summary(
      @RequestParam Long companyId, @RequestParam(required = false) Long branchId) {
    return service.summary(companyId, branchId);
  }

  /**
   * Gross written premium month and year to date against the prior year.
   *
   * @param companyId company
   * @param branchId optional branch
   * @param asOf optional reference date
   * @return premium widget
   */
  @GetMapping("/premium")
  @PreAuthorize(VIEW)
  public PremiumWidget premium(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return ledger.premium(companyId, branchId, asOf);
  }

  /**
   * Claims paid and outstanding.
   *
   * @param companyId company
   * @param branchId optional branch
   * @param asOf optional reference date
   * @return claims widget
   */
  @GetMapping("/claims")
  @PreAuthorize(VIEW)
  public ClaimsWidget claims(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return ledger.claims(companyId, branchId, asOf);
  }

  /**
   * Collections against the receivables ageing.
   *
   * @param companyId company
   * @param branchId optional branch
   * @param asOf optional reference date
   * @return collections widget
   */
  @GetMapping("/collections")
  @PreAuthorize(VIEW)
  public CollectionsWidget collections(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return operations.collections(companyId, branchId, asOf);
  }

  /**
   * Vendor payables overdue and due within 7 and 30 days.
   *
   * @param companyId company
   * @param branchId optional branch
   * @param asOf optional reference date
   * @return payables widget
   */
  @GetMapping("/payables")
  @PreAuthorize(VIEW)
  public PayablesWidget payables(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return ledger.payables(companyId, branchId, asOf);
  }

  /**
   * Cash and bank position.
   *
   * @param companyId company
   * @param branchId optional branch
   * @param asOf optional reference date
   * @return cash widget
   */
  @GetMapping("/cash")
  @PreAuthorize(VIEW)
  public CashWidget cash(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return ledger.cash(companyId, branchId, asOf);
  }

  /**
   * Expense budget against actual (company level: budgets have no branch).
   *
   * @param companyId company
   * @param asOf optional reference date
   * @return budget widget
   */
  @GetMapping("/budget")
  @PreAuthorize(VIEW)
  public BudgetWidget budget(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return operations.budget(companyId, asOf);
  }

  /**
   * Open alerts and the signed-in user's pending approvals.
   *
   * @param companyId company
   * @return workload widget
   */
  @GetMapping("/workload")
  @PreAuthorize(VIEW)
  public WorkloadWidget workload(@RequestParam Long companyId) {
    return operations.workload(companyId);
  }
}
