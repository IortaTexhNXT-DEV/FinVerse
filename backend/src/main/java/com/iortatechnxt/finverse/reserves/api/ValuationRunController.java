package com.iortatechnxt.finverse.reserves.api;

import com.iortatechnxt.finverse.common.api.PageResponse;
import com.iortatechnxt.finverse.common.api.ReasonRequest;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.reserves.api.dto.TakafulItemResponse;
import com.iortatechnxt.finverse.reserves.api.dto.UprDetailResponse;
import com.iortatechnxt.finverse.reserves.api.dto.ValuationRunDetailResponse;
import com.iortatechnxt.finverse.reserves.api.dto.ValuationRunRequest;
import com.iortatechnxt.finverse.reserves.api.dto.ValuationRunResponse;
import com.iortatechnxt.finverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.finverse.reserves.domain.ValuationRun;
import com.iortatechnxt.finverse.reserves.service.ReserveAnalysisService;
import com.iortatechnxt.finverse.reserves.service.ReservePosting;
import com.iortatechnxt.finverse.reserves.service.ValuationRunService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Actuarial valuation runs: list, preview (calculate), recalculate, submit, approve / reject, post,
 * cancel, and the policy-level UPR and takaful drill-downs. Preparing needs RESERVE_PREPARE;
 * approving, posting and cancelling need PERIOD_END_RUN.
 */
@RestController
@RequestMapping("/api/v1/reserves/runs")
@PreAuthorize("hasAnyAuthority('RESERVE_PREPARE', 'PERIOD_END_RUN')")
public class ValuationRunController {

  private static final int MAX_PAGE_SIZE = 200;

  private final ValuationRunService service;
  private final OrganizationService organization;

  /**
   * Creates the controller.
   *
   * @param service valuation run service
   * @param organization branches (codes)
   */
  public ValuationRunController(ValuationRunService service, OrganizationService organization) {
    this.service = service;
    this.organization = organization;
  }

  /**
   * Lists the runs of a company.
   *
   * @param companyId company
   * @return runs, latest first
   */
  @GetMapping
  public List<ValuationRunResponse> list(@RequestParam Long companyId) {
    return service.list(companyId).stream().map(ValuationRunResponse::from).toList();
  }

  /**
   * Gets a run with its lines, totals and journal movements.
   *
   * @param id run
   * @return detail
   */
  @GetMapping("/{id}")
  public ValuationRunDetailResponse get(@PathVariable Long id) {
    return detail(service.get(id));
  }

  /**
   * Policy-level UPR of a run.
   *
   * @param id run
   * @param businessLine optional line of business
   * @param page page number
   * @param size page size (max 200)
   * @return page
   */
  @GetMapping("/{id}/upr")
  public PageResponse<UprDetailResponse> upr(
      @PathVariable Long id,
      @RequestParam(required = false) String businessLine,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    PageRequest request =
        PageRequest.of(
            Math.max(page, 0),
            Math.clamp(size, 1, MAX_PAGE_SIZE),
            Sort.by("businessLine", "documentNo"));
    return PageResponse.of(service.uprDetail(id, businessLine, request), UprDetailResponse::from);
  }

  /**
   * Takaful surplus lines of a run.
   *
   * @param id run
   * @return lines
   */
  @GetMapping("/{id}/takaful")
  public List<TakafulItemResponse> takaful(@PathVariable Long id) {
    return service.takaful(id).stream().map(TakafulItemResponse::from).toList();
  }

  /**
   * Calculates a new run (preview).
   *
   * @param request company and valuation month
   * @return detail
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('RESERVE_PREPARE')")
  public ValuationRunDetailResponse create(@Valid @RequestBody ValuationRunRequest request) {
    return detail(service.create(request.companyId(), request.valuationDate()));
  }

  /**
   * Recalculates a run in preview.
   *
   * @param id run
   * @return detail
   */
  @PostMapping("/{id}/recalculate")
  @PreAuthorize("hasAuthority('RESERVE_PREPARE')")
  public ValuationRunDetailResponse recalculate(@PathVariable Long id) {
    return detail(service.recalculate(id));
  }

  /**
   * Submits a run for approval.
   *
   * @param id run
   * @return header
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize("hasAuthority('RESERVE_PREPARE')")
  public ValuationRunResponse submit(@PathVariable Long id) {
    return ValuationRunResponse.from(service.submit(id));
  }

  /**
   * Approves a run (checker).
   *
   * @param id run
   * @return header
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('PERIOD_END_RUN')")
  public ValuationRunResponse approve(@PathVariable Long id) {
    return ValuationRunResponse.from(service.approve(id));
  }

  /**
   * Rejects a submitted run back to preview.
   *
   * @param id run
   * @param request reason
   * @return header
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('PERIOD_END_RUN')")
  public ValuationRunResponse reject(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return ValuationRunResponse.from(service.reject(id, request.reason()));
  }

  /**
   * Posts the journals of an approved run (idempotent).
   *
   * @param id run
   * @return header
   */
  @PostMapping("/{id}/post")
  @PreAuthorize("hasAuthority('PERIOD_END_RUN')")
  public ValuationRunResponse post(@PathVariable Long id) {
    return ValuationRunResponse.from(service.post(id));
  }

  /**
   * Cancels a run (reversing its journals when posted).
   *
   * @param id run
   * @param request reason
   * @return header
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('PERIOD_END_RUN')")
  public ValuationRunResponse cancel(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return ValuationRunResponse.from(service.cancel(id, request.reason()));
  }

  private ValuationRunDetailResponse detail(ValuationRun run) {
    ValuationRun loaded = service.get(run.getId());
    List<ReserveLineValues> lines = ReserveAnalysisService.values(loaded);
    List<ReserveLineValues> opening =
        service.baseline(loaded).map(ReserveAnalysisService::values).orElse(List.of());
    Map<Long, String> codes =
        organization.listBranches(loaded.getCompanyId()).stream()
            .collect(Collectors.toMap(Branch::getId, Branch::getCode));
    return ValuationRunDetailResponse.of(
        loaded,
        lines,
        ReservePosting.movements(lines, opening),
        id -> codes.getOrDefault(id, String.valueOf(id)));
  }
}
