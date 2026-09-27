package com.iortatechnxt.brokerverse.submitted.processing.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLimitCheckRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunResult;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunResultRepository;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.MasterlistController;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.processing.api.dto.RunDtos.LimitCheckView;
import com.iortatechnxt.brokerverse.submitted.processing.api.dto.RunDtos.PolicyResults;
import com.iortatechnxt.brokerverse.submitted.processing.api.dto.RunDtos.ResultView;
import com.iortatechnxt.brokerverse.submitted.processing.api.dto.RunDtos.RunRequest;
import com.iortatechnxt.brokerverse.submitted.processing.api.dto.RunDtos.RunView;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService;
import com.iortatechnxt.brokerverse.submitted.service.SbmScopeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Processing Runs (FRS FR-SP-021, 022, 050): start a run on demand (Run Processing), the list of
 * runs, a run with its results and fallout, and the rule results and limit checks of a record.
 */
@RestController
@RequestMapping("/api/v1/submitted/runs")
public class RunController {

  private final SbmProcessingService processing;
  private final SbmRunRepository runs;
  private final SbmRunResultRepository results;
  private final SbmLimitCheckRepository checks;
  private final SbmPolicyRepository policies;
  private final MasterlistService masterlist;
  private final SbmScopeService scope;

  /**
   * Creates the controller.
   *
   * @param processing processing runs
   * @param runs runs
   * @param results run results
   * @param checks limit checks
   * @param policies masterlist
   * @param masterlist masterlist (scope of a record)
   * @param scope data scope
   */
  public RunController(
      SbmProcessingService processing,
      SbmRunRepository runs,
      SbmRunResultRepository results,
      SbmLimitCheckRepository checks,
      SbmPolicyRepository policies,
      MasterlistService masterlist,
      SbmScopeService scope) {
    this.processing = processing;
    this.runs = runs;
    this.results = results;
    this.checks = checks;
    this.policies = policies;
    this.masterlist = masterlist;
    this.scope = scope;
  }

  /**
   * Starts a run for records (or every record not yet in the renewal).
   *
   * @param request company and records
   * @return the run
   */
  @PostMapping
  @PreAuthorize("hasAuthority('SBM_PROCESS')")
  @Transactional
  public RunView run(@Valid @RequestBody RunRequest request) {
    List<Long> ids =
        request.policyIds() == null || request.policyIds().isEmpty()
            ? processing.scheduledScope(request.companyId())
            : request.policyIds();
    String scopeText =
        request.policyIds() == null || request.policyIds().isEmpty()
            ? "Every record not yet in the renewal"
            : ids.size() + " selected record(s)";
    return RunView.from(
        processing.run(
            new SbmProcessingService.RunRequest(
                request.companyId(), SbmRun.Trigger.MANUAL, scopeText, ids)));
  }

  /**
   * Runs, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return runs
   */
  @GetMapping
  @PreAuthorize(MasterlistController.VIEW)
  public PageResponse<RunView> list(@RequestParam Long companyId, Pageable pageable) {
    return PageResponse.of(runs.findByCompanyIdOrderByIdDesc(companyId, pageable), RunView::from);
  }

  /**
   * A run.
   *
   * @param id run
   * @return run
   */
  @GetMapping("/{id}")
  @PreAuthorize(MasterlistController.VIEW)
  public RunView get(@PathVariable Long id) {
    return RunView.from(processing.require(id));
  }

  /**
   * Results of a run, optionally of one outcome (FALLOUT for the fallout tab), within the user's
   * scope.
   *
   * @param id run
   * @param outcome outcome, may be empty
   * @param pageable page
   * @return results
   */
  @GetMapping("/{id}/results")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public PageResponse<ResultView> results(
      @PathVariable Long id,
      @RequestParam(required = false) SbmRunResult.Outcome outcome,
      Pageable pageable) {
    SbmRun run = processing.require(id);
    Page<SbmRunResult> page =
        outcome == null
            ? results.findByRunIdOrderByIdAsc(id, pageable)
            : results.findByRunIdAndOutcomeOrderByIdAsc(id, outcome, pageable);
    Map<Long, SbmPolicy> byId =
        policies
            .findAllById(page.getContent().stream().map(SbmRunResult::getPolicyId).distinct().toList())
            .stream()
            .filter(scope.current(run.getCompanyId())::allows)
            .collect(Collectors.toMap(SbmPolicy::getId, Function.identity()));
    return PageResponse.of(
        page,
        r -> {
          SbmPolicy p = byId.get(r.getPolicyId());
          return ResultView.from(
              r, p == null ? null : p.getSbmNo(), p == null ? null : p.getAssured().assuredName());
        });
  }

  /**
   * The rule results and limit checks of a record.
   *
   * @param policyId record
   * @return results and checks
   */
  @GetMapping("/of-policy/{policyId}")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public PolicyResults ofPolicy(@PathVariable Long policyId) {
    SbmPolicy p = masterlist.get(policyId);
    return new PolicyResults(
        results.findByPolicyIdOrderByIdDesc(policyId).stream()
            .map(r -> ResultView.from(r, p.getSbmNo(), p.getAssured().assuredName()))
            .toList(),
        checks.findByPolicyIdOrderByIdDesc(policyId).stream().map(LimitCheckView::from).toList());
  }
}
