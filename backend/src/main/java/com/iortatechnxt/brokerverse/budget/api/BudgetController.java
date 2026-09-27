package com.iortatechnxt.brokerverse.budget.api;

import com.iortatechnxt.brokerverse.budget.api.dto.BudgetComparisonResponse;
import com.iortatechnxt.brokerverse.budget.api.dto.BudgetLineRequest;
import com.iortatechnxt.brokerverse.budget.api.dto.BudgetResponse;
import com.iortatechnxt.brokerverse.budget.api.dto.CopyActualsRequest;
import com.iortatechnxt.brokerverse.budget.api.dto.CreateBudgetRequest;
import com.iortatechnxt.brokerverse.budget.api.dto.VarianceLineResponse;
import com.iortatechnxt.brokerverse.budget.service.BudgetLineService;
import com.iortatechnxt.brokerverse.budget.service.BudgetMonitoringService;
import com.iortatechnxt.brokerverse.budget.service.BudgetService;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Budget versions (grid, import, copy, approval) and budget monitoring. */
@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

  private static final String MANAGE = "hasAuthority('BUDGET_MANAGE')";
  private static final String VIEW = "hasAnyAuthority('BUDGET_MANAGE','REPORT_FINANCIAL')";
  private static final int MAX_CSV = 2_000_000;

  private final BudgetService budgets;
  private final BudgetLineService lines;
  private final BudgetMonitoringService monitoring;

  /**
   * Creates the controller.
   *
   * @param budgets budget service
   * @param lines line maintenance service
   * @param monitoring monitoring service
   */
  public BudgetController(
      BudgetService budgets, BudgetLineService lines, BudgetMonitoringService monitoring) {
    this.budgets = budgets;
    this.lines = lines;
    this.monitoring = monitoring;
  }

  /**
   * Lists budget versions.
   *
   * @param companyId company
   * @param fiscalYear optional year
   * @return versions
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<BudgetResponse> list(
      @RequestParam Long companyId, @RequestParam(required = false) Integer fiscalYear) {
    return budgets.list(companyId, fiscalYear).stream().map(BudgetResponse::summary).toList();
  }

  /**
   * Gets a version with its lines.
   *
   * @param id id
   * @return budget
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public BudgetResponse get(@PathVariable Long id) {
    return BudgetResponse.detail(budgets.get(id));
  }

  /**
   * Creates a draft version.
   *
   * @param request request
   * @return budget
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MANAGE)
  public BudgetResponse create(@Valid @RequestBody CreateBudgetRequest request) {
    return BudgetResponse.detail(budgets.create(request));
  }

  /**
   * Saves the budget grid.
   *
   * @param id id
   * @param request lines
   * @return budget
   */
  @PutMapping("/{id}/lines")
  @PreAuthorize(MANAGE)
  public BudgetResponse saveLines(
      @PathVariable Long id,
      @Valid @RequestBody @Size(max = 5000) List<@Valid BudgetLineRequest> request) {
    return BudgetResponse.detail(lines.saveLines(id, request));
  }

  /**
   * Imports lines from CSV (see docs/samples/budget_import_sample.csv).
   *
   * @param id id
   * @param csv file content
   * @return budget
   */
  @PostMapping(
      value = "/{id}/import",
      consumes = {MediaType.TEXT_PLAIN_VALUE, "text/csv"})
  @PreAuthorize(MANAGE)
  public BudgetResponse importCsv(
      @PathVariable Long id, @RequestBody @NotBlank @Size(max = MAX_CSV) String csv) {
    return BudgetResponse.detail(lines.importCsv(id, csv));
  }

  /**
   * Fills a draft from prior-year actuals.
   *
   * @param id id
   * @param request source year and adjustment
   * @return budget
   */
  @PostMapping("/{id}/copy-actuals")
  @PreAuthorize(MANAGE)
  public BudgetResponse copyActuals(
      @PathVariable Long id, @Valid @RequestBody CopyActualsRequest request) {
    return BudgetResponse.detail(
        lines.copyFromActuals(id, request.sourceYear(), request.adjustmentPercent()));
  }

  /**
   * Submits a version for approval.
   *
   * @param id id
   * @return budget
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(MANAGE)
  public BudgetResponse submit(@PathVariable Long id) {
    return BudgetResponse.summary(budgets.submit(id));
  }

  /**
   * Approves a version (a different user than the submitter).
   *
   * @param id id
   * @return budget
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(MANAGE)
  public BudgetResponse approve(@PathVariable Long id) {
    return BudgetResponse.summary(budgets.approve(id));
  }

  /**
   * Rejects a version.
   *
   * @param id id
   * @param request reason
   * @return budget
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(MANAGE)
  public BudgetResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return BudgetResponse.summary(budgets.reject(id, request.reason()));
  }

  /**
   * Budget vs actual against the approved version.
   *
   * @param companyId company
   * @param asOf as-of date
   * @param byCostCenter split per cost centre
   * @return comparison
   */
  @GetMapping("/variance")
  @PreAuthorize(VIEW)
  public BudgetComparisonResponse variance(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
      @RequestParam(defaultValue = "false") boolean byCostCenter) {
    return BudgetComparisonResponse.from(monitoring.compare(companyId, asOf, byCostCenter));
  }

  /**
   * "Budget threshold exceeded" alerts: expense accounts at or above a utilization percentage.
   *
   * @param companyId company
   * @param asOf as-of date
   * @param threshold utilization threshold in percent (default 90)
   * @return accounts over the threshold
   */
  @GetMapping("/alerts")
  @PreAuthorize("hasAnyAuthority('BUDGET_MANAGE','REPORT_FINANCIAL','DASHBOARD_VIEW')")
  public List<VarianceLineResponse> alerts(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
      @RequestParam(defaultValue = "90") BigDecimal threshold) {
    return monitoring.alerts(companyId, asOf, threshold).stream()
        .map(VarianceLineResponse::from)
        .toList();
  }
}
