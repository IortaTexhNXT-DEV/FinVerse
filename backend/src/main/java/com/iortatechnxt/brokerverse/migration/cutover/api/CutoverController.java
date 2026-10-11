package com.iortatechnxt.brokerverse.migration.cutover.api;

import com.iortatechnxt.brokerverse.migration.common.service.Workbooks;
import com.iortatechnxt.brokerverse.migration.cutover.api.dto.CutoverDtos.CohortResponse;
import com.iortatechnxt.brokerverse.migration.cutover.api.dto.CutoverDtos.CriterionResponse;
import com.iortatechnxt.brokerverse.migration.cutover.api.dto.CutoverDtos.DecisionResponse;
import com.iortatechnxt.brokerverse.migration.cutover.api.dto.CutoverDtos.DecommissionResponse;
import com.iortatechnxt.brokerverse.migration.cutover.api.dto.CutoverDtos.PlanDetail;
import com.iortatechnxt.brokerverse.migration.cutover.api.dto.CutoverDtos.PlanResponse;
import com.iortatechnxt.brokerverse.migration.cutover.api.dto.CutoverDtos.TaskResponse;
import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverPlan;
import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverTask;
import com.iortatechnxt.brokerverse.migration.cutover.domain.DecommissionItem;
import com.iortatechnxt.brokerverse.migration.cutover.service.CutoverService;
import com.iortatechnxt.brokerverse.migration.cutover.service.DecommissionService;
import com.iortatechnxt.brokerverse.migration.cutover.service.RunoffService;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
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
 * The cutover console (DATA_MIGRATION_DESIGN 17 and 22): cutover plans with their runbook and go /
 * no-go criteria, the board's decision, the runbook export, the run-off of the migrated policies
 * and the decommissioning checklists.
 */
@RestController
@RequestMapping("/api/v1/migration")
@Transactional
public class CutoverController {

  private static final String VIEW = "hasAuthority('MIG_VIEW')";
  private static final String MANAGE = "hasAuthority('MIG_CUTOVER_MANAGE')";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final CutoverService cutover;
  private final RunoffService runoff;
  private final DecommissionService decommission;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param cutover cutover plans
   * @param runoff run-off
   * @param decommission decommissioning
   * @param downloads file answers
   */
  public CutoverController(
      CutoverService cutover,
      RunoffService runoff,
      DecommissionService decommission,
      FileDownloads downloads) {
    this.cutover = cutover;
    this.runoff = runoff;
    this.decommission = decommission;
    this.downloads = downloads;
  }

  /**
   * The cutover plans of a company.
   *
   * @param companyId company
   * @return plans
   */
  @GetMapping("/cutover/plans")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public List<PlanResponse> plans(@RequestParam Long companyId) {
    return cutover.plans(companyId).stream().map(PlanResponse::from).toList();
  }

  /**
   * Creates a plan with the standard runbook.
   *
   * @param companyId company
   * @param body plan data
   * @return the plan
   */
  @PostMapping("/cutover/plans")
  @PreAuthorize(MANAGE)
  public PlanResponse create(@RequestParam Long companyId, @Valid @RequestBody PlanBody body) {
    return PlanResponse.from(
        cutover.create(
            companyId,
            new CutoverPlan.Data(
                body.name(),
                body.kind(),
                body.mockNo(),
                body.environment(),
                body.goLiveDate(),
                body.freezeStart(),
                body.freezeEnd())));
  }

  /**
   * A plan with its runbook, criteria and decisions.
   *
   * @param planNo plan
   * @return the plan
   */
  @GetMapping("/cutover/plans/{planNo}")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public PlanDetail plan(@PathVariable String planNo) {
    return PlanDetail.from(cutover.view(planNo));
  }

  /**
   * Records the progress of a task.
   *
   * @param planNo plan
   * @param seq task
   * @param body status and remarks
   * @return the task
   */
  @PostMapping("/cutover/plans/{planNo}/tasks/{seq}")
  @PreAuthorize(MANAGE)
  public TaskResponse progress(
      @PathVariable String planNo, @PathVariable int seq, @Valid @RequestBody TaskBody body) {
    return TaskResponse.from(cutover.progress(planNo, seq, body.status(), body.note()));
  }

  /**
   * Measures the automatic go / no-go criteria.
   *
   * @param planNo plan
   * @return the criteria
   */
  @PostMapping("/cutover/plans/{planNo}/measure")
  @PreAuthorize("hasAnyAuthority('MIG_CUTOVER_MANAGE', 'MIG_GONOGO_DECIDE')")
  public List<CriterionResponse> measure(@PathVariable String planNo) {
    return cutover.measure(planNo).stream().map(CriterionResponse::from).toList();
  }

  /**
   * Records a manual criterion.
   *
   * @param planNo plan
   * @param no criterion
   * @param body met and evidence
   * @return the criterion
   */
  @PostMapping("/cutover/plans/{planNo}/criteria/{no}")
  @PreAuthorize(MANAGE)
  public CriterionResponse record(
      @PathVariable String planNo, @PathVariable int no, @Valid @RequestBody CriterionBody body) {
    return CriterionResponse.from(cutover.record(planNo, no, body.met(), body.note()));
  }

  /**
   * Records the go / no-go decision.
   *
   * @param planNo plan
   * @param body GO or NO-GO and comment
   * @return the decision
   */
  @PostMapping("/cutover/plans/{planNo}/decision")
  @PreAuthorize("hasAuthority('MIG_GONOGO_DECIDE')")
  public DecisionResponse decide(
      @PathVariable String planNo, @Valid @RequestBody DecisionBody body) {
    return DecisionResponse.from(cutover.decide(planNo, body.go(), body.comment()));
  }

  /**
   * The runbook of a plan as a workbook (tasks and criteria).
   *
   * @param planNo plan
   * @param request request
   * @return the workbook
   */
  @GetMapping("/cutover/plans/{planNo}/runbook")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> runbook(@PathVariable String planNo, HttpServletRequest request) {
    PlanDetail d = PlanDetail.from(cutover.view(planNo));
    List<List<String>> tasks = new ArrayList<>();
    for (TaskResponse t : d.tasks()) {
      tasks.add(
          List.of(
              String.valueOf(t.seq()),
              t.phase(),
              t.task(),
              t.ownerRole(),
              Objects.toString(t.objectCode(), ""),
              Objects.toString(t.dependsOn(), ""),
              Objects.toString(t.plannedStart(), ""),
              Objects.toString(t.plannedEnd(), ""),
              t.status(),
              Objects.toString(t.remarks(), "")));
    }
    List<List<String>> criteria = new ArrayList<>();
    for (CriterionResponse c : d.criteria()) {
      criteria.add(
          List.of(
              String.valueOf(c.criterionNo()),
              c.name(),
              c.threshold(),
              c.manual() ? "Recorded" : "Measured",
              Objects.toString(c.measuredValue(), ""),
              c.met() == null ? "" : (c.met() ? "Met" : "Not met")));
    }
    try (Workbooks book = Workbooks.create()) {
      book.sheet(
          "Runbook",
          List.of(
              "No.",
              "Phase",
              "Task",
              "Owner",
              "Object",
              "After",
              "Planned start",
              "Planned end",
              "Status",
              "Remarks"),
          tasks);
      book.sheet(
          "Go-no-go",
          List.of("No.", "Criterion", "Threshold", "Kind", "Measured", "Result"),
          criteria);
      return downloads.respond(
          FileDownload.inline(planNo + "_runbook.xlsx", XLSX, book.bytes()), request);
    }
  }

  /**
   * The latest run-off snapshot.
   *
   * @param companyId company
   * @return cohorts
   */
  @GetMapping("/runoff")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public List<CohortResponse> runoff(@RequestParam Long companyId) {
    return runoff.latest(companyId).stream().map(CohortResponse::from).toList();
  }

  /**
   * Takes a run-off snapshot now.
   *
   * @param companyId company
   * @param date snapshot date
   * @return cohorts
   */
  @PostMapping("/runoff/snapshot")
  @PreAuthorize(MANAGE)
  public List<CohortResponse> snapshot(@RequestParam Long companyId, @RequestParam LocalDate date) {
    return runoff.snapshot(companyId, date).stream().map(CohortResponse::from).toList();
  }

  /**
   * The decommissioning checklists.
   *
   * @param companyId company
   * @return items
   */
  @GetMapping("/decommission")
  @PreAuthorize(VIEW)
  public List<DecommissionResponse> checklists(@RequestParam Long companyId) {
    return decommission.checklists(companyId).stream().map(DecommissionResponse::from).toList();
  }

  /**
   * Opens the checklist of a legacy system.
   *
   * @param companyId company
   * @param system legacy system
   * @return its items
   */
  @PostMapping("/decommission/systems/{system}")
  @PreAuthorize(MANAGE)
  public List<DecommissionResponse> open(
      @RequestParam Long companyId, @PathVariable String system) {
    return decommission.open(companyId, system).stream().map(DecommissionResponse::from).toList();
  }

  /**
   * Records the status and evidence of a criterion.
   *
   * @param id item
   * @param body status and evidence
   * @return the item
   */
  @PostMapping("/decommission/items/{id}")
  @PreAuthorize(MANAGE)
  public DecommissionResponse update(@PathVariable Long id, @Valid @RequestBody ItemBody body) {
    return DecommissionResponse.from(decommission.update(id, body.status(), body.evidence()));
  }

  /**
   * A plan to create.
   *
   * @param name name
   * @param kind kind
   * @param mockNo mock number
   * @param environment SIT, UAT, PERF or PROD
   * @param goLiveDate go-live date
   * @param freezeStart freeze start
   * @param freezeEnd freeze end
   */
  public record PlanBody(
      @NotBlank @Size(max = 120) String name,
      @NotNull CutoverPlan.Kind kind,
      Integer mockNo,
      @NotBlank @Size(max = 20) String environment,
      @NotNull LocalDate goLiveDate,
      LocalDateTime freezeStart,
      LocalDateTime freezeEnd) {}

  /**
   * Progress of a task.
   *
   * @param status status
   * @param note remarks
   */
  public record TaskBody(@NotNull CutoverTask.Status status, @Size(max = 1000) String note) {}

  /**
   * A manual criterion.
   *
   * @param met met
   * @param note evidence
   */
  public record CriterionBody(boolean met, @Size(max = 1000) String note) {}

  /**
   * A go / no-go decision.
   *
   * @param go GO or NO-GO
   * @param comment comment
   */
  public record DecisionBody(boolean go, @Size(max = 2000) String comment) {}

  /**
   * A decommissioning criterion update.
   *
   * @param status status
   * @param evidence evidence
   */
  public record ItemBody(
      @NotNull DecommissionItem.Status status, @Size(max = 1000) String evidence) {}
}
