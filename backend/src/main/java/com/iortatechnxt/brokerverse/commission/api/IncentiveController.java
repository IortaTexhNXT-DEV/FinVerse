package com.iortatechnxt.brokerverse.commission.api;

import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.RunLineResponse;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.RunRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.RunResponse;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.SchemeRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.SchemeResponse;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.SchemeTerms;
import com.iortatechnxt.brokerverse.commission.service.IncentivePosting;
import com.iortatechnxt.brokerverse.commission.service.IncentiveRunService;
import com.iortatechnxt.brokerverse.commission.service.IncentiveSchemeService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
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
 * Incentive schemes and runs (CMRID.003/005/006): the tier editor of No Touch, Top Up and Motor
 * Mania ({@code INCENTIVE_MANAGE}), computed runs with their lines, posting by the team leader
 * ({@code COMMREC_APPROVE}) with the accrual and the pass-on to branches, and cancellation.
 */
@RestController
@RequestMapping("/api/v1/commission/incentives")
public class IncentiveController {

  private final IncentiveSchemeService schemes;
  private final IncentiveRunService runs;
  private final IncentivePosting posting;

  /**
   * Creates the controller.
   *
   * @param schemes schemes
   * @param runs runs
   * @param posting posting
   */
  public IncentiveController(
      IncentiveSchemeService schemes, IncentiveRunService runs, IncentivePosting posting) {
    this.schemes = schemes;
    this.runs = runs;
    this.posting = posting;
  }

  /**
   * Schemes of a company.
   *
   * @param companyId company
   * @return schemes with their tiers
   */
  @GetMapping("/schemes")
  @PreAuthorize(CommissionAccess.INCENTIVE_READ)
  public List<SchemeResponse> schemes(@RequestParam Long companyId) {
    return schemes.list(companyId).stream().map(SchemeResponse::from).toList();
  }

  /**
   * A scheme.
   *
   * @param id scheme
   * @return scheme
   */
  @GetMapping("/schemes/{id}")
  @PreAuthorize(CommissionAccess.INCENTIVE_READ)
  public SchemeResponse scheme(@PathVariable Long id) {
    return SchemeResponse.from(schemes.require(id));
  }

  /**
   * Adds a scheme.
   *
   * @param request scheme
   * @return scheme
   */
  @PostMapping("/schemes")
  @PreAuthorize(CommissionAccess.INCENTIVE)
  public SchemeResponse create(@Valid @RequestBody SchemeRequest request) {
    return SchemeResponse.from(
        schemes.create(request.companyId(), request.code(), request.terms().toTerms()));
  }

  /**
   * Changes a scheme and its tiers.
   *
   * @param id scheme
   * @param terms terms
   * @return scheme
   */
  @PutMapping("/schemes/{id}")
  @PreAuthorize(CommissionAccess.INCENTIVE)
  public SchemeResponse update(@PathVariable Long id, @Valid @RequestBody SchemeTerms terms) {
    return SchemeResponse.from(schemes.update(id, terms.toTerms()));
  }

  /**
   * Runs of a company.
   *
   * @param companyId company
   * @param schemeId scheme
   * @param page page
   * @param size size
   * @return runs
   */
  @GetMapping("/runs")
  @PreAuthorize(CommissionAccess.INCENTIVE_READ)
  public PageResponse<RunResponse> runs(
      @RequestParam Long companyId,
      @RequestParam(required = false) Long schemeId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        runs.runs(companyId, schemeId, CommissionAccess.page(page, size)), RunResponse::from);
  }

  /**
   * A run.
   *
   * @param id run
   * @return run
   */
  @GetMapping("/runs/{id}")
  @PreAuthorize(CommissionAccess.INCENTIVE_READ)
  public RunResponse run(@PathVariable Long id) {
    return RunResponse.from(runs.require(id));
  }

  /**
   * Lines of a run.
   *
   * @param id run
   * @param page page
   * @param size size
   * @return lines
   */
  @GetMapping("/runs/{id}/lines")
  @PreAuthorize(CommissionAccess.INCENTIVE_READ)
  public PageResponse<RunLineResponse> lines(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        runs.lines(id, CommissionAccess.page(page, size)), RunLineResponse::from);
  }

  /**
   * Computes a run.
   *
   * @param request scheme and period
   * @return run
   */
  @PostMapping("/runs")
  @PreAuthorize(CommissionAccess.INCENTIVE)
  public RunResponse compute(@Valid @RequestBody RunRequest request) {
    return RunResponse.from(runs.compute(request.schemeId(), request.from(), request.to()));
  }

  /**
   * Posts a run (accrual and pass-on).
   *
   * @param id run
   * @return run
   */
  @PostMapping("/runs/{id}/post")
  @PreAuthorize(CommissionAccess.APPROVE)
  public RunResponse post(@PathVariable Long id) {
    return RunResponse.from(posting.post(id));
  }

  /**
   * Cancels a computed run.
   *
   * @param id run
   * @return run
   */
  @PostMapping("/runs/{id}/cancel")
  @PreAuthorize(CommissionAccess.INCENTIVE)
  public RunResponse cancel(@PathVariable Long id) {
    return RunResponse.from(runs.cancel(id));
  }
}
