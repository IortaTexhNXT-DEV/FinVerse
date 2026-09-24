package com.iortatechnxt.brokerverse.consolidation.api;

import com.iortatechnxt.brokerverse.consolidation.api.dto.ConsolidationRunResponse;
import com.iortatechnxt.brokerverse.consolidation.api.dto.GroupRequest;
import com.iortatechnxt.brokerverse.consolidation.api.dto.GroupResponse;
import com.iortatechnxt.brokerverse.consolidation.service.ConsolidationGroupService;
import com.iortatechnxt.brokerverse.consolidation.service.ConsolidationRunService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

/** Consolidation groups and runs (translation, elimination, consolidated trial balance). */
@RestController
@RequestMapping("/api/v1/consolidation")
public class ConsolidationController {

  private static final String RUN = "hasAuthority('CONSOLIDATION_RUN')";
  private static final String VIEW = "hasAnyAuthority('CONSOLIDATION_RUN','REPORT_FINANCIAL')";

  private final ConsolidationGroupService groups;
  private final ConsolidationRunService runs;

  /**
   * Creates the controller.
   *
   * @param groups group service
   * @param runs run service
   */
  public ConsolidationController(ConsolidationGroupService groups, ConsolidationRunService runs) {
    this.groups = groups;
    this.runs = runs;
  }

  /**
   * Lists groups.
   *
   * @return groups
   */
  @GetMapping("/groups")
  @PreAuthorize(VIEW)
  public List<GroupResponse> groups() {
    return groups.list().stream().map(GroupResponse::from).toList();
  }

  /**
   * Creates a group.
   *
   * @param request request
   * @return group
   */
  @PostMapping("/groups")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(RUN)
  public GroupResponse create(@Valid @RequestBody GroupRequest request) {
    return GroupResponse.from(groups.create(request));
  }

  /**
   * Updates a group.
   *
   * @param id id
   * @param request request
   * @return group
   */
  @PutMapping("/groups/{id}")
  @PreAuthorize(RUN)
  public GroupResponse update(@PathVariable Long id, @Valid @RequestBody GroupRequest request) {
    return GroupResponse.from(groups.update(id, request));
  }

  /**
   * Lists the runs of a group.
   *
   * @param groupId group
   * @return runs
   */
  @GetMapping("/groups/{groupId}/runs")
  @PreAuthorize(VIEW)
  public List<ConsolidationRunResponse> runs(@PathVariable Long groupId) {
    return runs.list(groupId).stream().map(ConsolidationRunResponse::summary).toList();
  }

  /**
   * Runs the consolidation.
   *
   * @param groupId group
   * @param asOf as-of date
   * @return run with consolidated trial balance
   */
  @PostMapping("/groups/{groupId}/runs")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(RUN)
  public ConsolidationRunResponse run(
      @PathVariable Long groupId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
    return ConsolidationRunResponse.detail(runs.run(groupId, asOf));
  }

  /**
   * Gets a run with its consolidated trial balance and ledger.
   *
   * @param runId run
   * @return run
   */
  @GetMapping("/runs/{runId}")
  @PreAuthorize(VIEW)
  public ConsolidationRunResponse run(@PathVariable Long runId) {
    return ConsolidationRunResponse.detail(runs.get(runId));
  }

  /**
   * Finalizes (locks) a run.
   *
   * @param runId run
   * @return run
   */
  @PostMapping("/runs/{runId}/finalize")
  @PreAuthorize(RUN)
  public ConsolidationRunResponse finalizeRun(@PathVariable Long runId) {
    return ConsolidationRunResponse.summary(runs.finalizeRun(runId));
  }
}
