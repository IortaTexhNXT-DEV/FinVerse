package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.api.dto.SetupDtos.CommissionLineResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.SetupDtos.LayoutRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.SetupDtos.LayoutResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.SetupDtos.RuleResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.SetupDtos.SeriesRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.SetupDtos.SeriesResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.SetupDtos.SeriesUpdate;
import com.iortatechnxt.brokerverse.cashiering.service.CommissionOrService;
import com.iortatechnxt.brokerverse.cashiering.service.MinimalBalanceService;
import com.iortatechnxt.brokerverse.cashiering.service.MinimalBalanceService.Sweep;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentFileLayouts;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSeriesService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cashiering set-up and runs (CSHID.006/007/008/015/016): the receipt series master with
 * maker-checker, the payment file layouts, the minimal balance rules and sweep, and the commission
 * payment details with their OR issuance.
 */
@RestController
@RequestMapping("/api/v1/cashiering")
public class CashieringSetupController {

  private final ReceiptSeriesService series;
  private final PaymentFileLayouts layouts;
  private final MinimalBalanceService minimal;
  private final CommissionOrService commissions;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param series receipt series
   * @param layouts file layouts
   * @param minimal minimal balances
   * @param commissions commission ORs
   * @param clock clock
   */
  public CashieringSetupController(
      ReceiptSeriesService series,
      PaymentFileLayouts layouts,
      MinimalBalanceService minimal,
      CommissionOrService commissions,
      Clock clock) {
    this.series = series;
    this.layouts = layouts;
    this.minimal = minimal;
    this.commissions = commissions;
    this.clock = clock;
  }

  /**
   * Receipt series of a company.
   *
   * @param companyId company
   * @return series
   */
  @GetMapping("/series")
  @PreAuthorize("hasAnyAuthority('CASH_SERIES_MANAGE', 'MASTER_AUTHORIZE', 'CASH_RECEIPT')")
  public List<SeriesResponse> series(@RequestParam Long companyId) {
    return series.list(companyId).stream().map(SeriesResponse::from).toList();
  }

  /**
   * Creates a series (pending authorization).
   *
   * @param r series
   * @return series
   */
  @PostMapping("/series")
  @PreAuthorize(CashAccess.SERIES)
  public SeriesResponse create(@Valid @RequestBody SeriesRequest r) {
    return SeriesResponse.from(
        series.create(
            new ReceiptSeriesService.SeriesRequest(
                r.companyId(),
                r.branchId(),
                r.kind(),
                r.prefix(),
                r.fromNo(),
                r.toNo(),
                r.atpNo(),
                r.warnAt())));
  }

  /**
   * Changes a series.
   *
   * @param id series
   * @param r change
   * @return series
   */
  @PutMapping("/series/{id}")
  @PreAuthorize(CashAccess.SERIES)
  public SeriesResponse update(@PathVariable Long id, @Valid @RequestBody SeriesUpdate r) {
    return SeriesResponse.from(series.update(id, r.atpNo(), r.toNo(), r.warnAt()));
  }

  /**
   * Authorizes a series (checker).
   *
   * @param id series
   * @return series
   */
  @PostMapping("/series/{id}/authorize")
  @PreAuthorize(CashAccess.SERIES_AUTHORIZE)
  public SeriesResponse authorize(@PathVariable Long id) {
    return SeriesResponse.from(series.authorize(id));
  }

  /**
   * Deactivates a series.
   *
   * @param id series
   * @return series
   */
  @PostMapping("/series/{id}/deactivate")
  @PreAuthorize(CashAccess.SERIES)
  public SeriesResponse deactivate(@PathVariable Long id) {
    return SeriesResponse.from(series.deactivate(id));
  }

  /**
   * Payment file layouts.
   *
   * @return layouts
   */
  @GetMapping("/layouts")
  @PreAuthorize(CashAccess.VIEW)
  public List<LayoutResponse> layouts() {
    return layouts.list().stream().map(LayoutResponse::from).toList();
  }

  /**
   * Changes a layout.
   *
   * @param code handler
   * @param r layout
   * @return layout
   */
  @PutMapping("/layouts/{code}")
  @PreAuthorize(CashAccess.APPROVE)
  public LayoutResponse changeLayout(
      @PathVariable String code, @Valid @RequestBody LayoutRequest r) {
    return LayoutResponse.from(layouts.change(code, r.kind(), r.delimiter(), r.fields()));
  }

  /**
   * Minimal balance rules.
   *
   * @return rules
   */
  @GetMapping("/minimal-balance/rules")
  @PreAuthorize(CashAccess.VIEW)
  public List<RuleResponse> rules() {
    return minimal.rules().stream().map(RuleResponse::from).toList();
  }

  /**
   * Runs the minimal balance sweep now.
   *
   * @return balances reversed and excess moved
   */
  @PostMapping("/minimal-balance/sweep")
  @PreAuthorize(CashAccess.APPROVE)
  public Sweep sweep() {
    return minimal.sweep(LocalDate.now(clock));
  }

  /**
   * Staged commission payment lines.
   *
   * @param companyId company
   * @return lines
   */
  @GetMapping("/commission-payments")
  @PreAuthorize(CashAccess.VIEW)
  public List<CommissionLineResponse> commissionLines(@RequestParam Long companyId) {
    return commissions.staged(companyId).stream().map(CommissionLineResponse::from).toList();
  }

  /**
   * Issues the commission ORs of the staged lines (CSHID.007).
   *
   * @param companyId company
   * @return OR numbers
   */
  @PostMapping("/commission-payments/issue")
  @PreAuthorize(CashAccess.RECEIPT)
  public List<String> issueCommissionOrs(@RequestParam Long companyId) {
    return commissions.issue(companyId);
  }
}
