package com.iortatechnxt.brokerverse.workflow.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.workflow.api.dto.ActionRequest;
import com.iortatechnxt.brokerverse.workflow.api.dto.AssignRequest;
import com.iortatechnxt.brokerverse.workflow.api.dto.CaseResponse;
import com.iortatechnxt.brokerverse.workflow.api.dto.QueueCountResponse;
import com.iortatechnxt.brokerverse.workflow.api.dto.QueueParams;
import com.iortatechnxt.brokerverse.workflow.api.dto.WorkItemResponse;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkQueueService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowDefinitions;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * My Work queues, a record's workflow panel (stage, actions, history), generic actions (return,
 * void...), claim and assignment.
 */
@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

  private static final String VIEW = "hasAuthority('WORK_VIEW')";
  private static final String RECORD_VIEW =
      "hasAnyAuthority('WORK_VIEW', 'CLIENT_VIEW', 'QUOTE_VIEW', 'ACCOUNT_VIEW', 'TSU_PROCESS')";
  private static final int MAX_PAGE_SIZE = 200;

  private final WorkflowService workflow;
  private final WorkflowViewService views;
  private final WorkAssignmentService assignments;
  private final WorkflowDefinitions definitions;
  private final WorkQueueService queues;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param workflow transitions
   * @param views case views
   * @param assignments claim and assignment
   * @param definitions stage definitions
   * @param queues work queues
   * @param clock clock
   */
  public WorkflowController(
      WorkflowService workflow,
      WorkflowViewService views,
      WorkAssignmentService assignments,
      WorkflowDefinitions definitions,
      WorkQueueService queues,
      Clock clock) {
    this.workflow = workflow;
    this.views = views;
    this.assignments = assignments;
    this.definitions = definitions;
    this.queues = queues;
    this.clock = clock;
  }

  /**
   * Items of my queues.
   *
   * @param params filters
   * @param page page
   * @param size size
   * @return items, oldest due first
   */
  @GetMapping("/queue")
  @PreAuthorize(VIEW)
  public PageResponse<WorkItemResponse> queue(
      @ModelAttribute QueueParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    Map<String, String> names = stageNames(params.workflow());
    var now = clock.instant();
    return PageResponse.of(
        queues.queue(params.toQuery(), PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        c -> WorkItemResponse.from(c, stageName(names, c), now));
  }

  private Map<String, String> stageNames(String workflowCode) {
    return workflowCode == null ? Map.of() : definitions.stageNames(workflowCode);
  }

  private String stageName(Map<String, String> names, WorkCase c) {
    String name = names.get(c.getStageCode());
    return name != null ? name : definitions.stageOf(c).getName();
  }

  /**
   * My Work tiles.
   *
   * @param companyId company
   * @return counts per queue stage I work
   */
  @GetMapping("/counts")
  @PreAuthorize(VIEW)
  public List<QueueCountResponse> counts(@RequestParam Long companyId) {
    return queues.counts(companyId).stream().map(QueueCountResponse::from).toList();
  }

  /**
   * Workflow panel of a record.
   *
   * @param entityType record type
   * @param entityId record id
   * @return stage, actions and history
   */
  @GetMapping("/cases/by-record")
  @PreAuthorize(RECORD_VIEW)
  public CaseResponse byRecord(@RequestParam String entityType, @RequestParam String entityId) {
    return views
        .view(entityType, entityId)
        .map(v -> CaseResponse.from(v, clock.instant()))
        .orElseThrow(() -> new ResourceNotFoundException(entityType + " work item", entityId));
  }

  /**
   * Runs a generic action (return, void, decline...).
   *
   * @param id case
   * @param action action code
   * @param request reason and comment
   * @return updated panel
   */
  @PostMapping("/cases/{id}/actions/{action}")
  @PreAuthorize(RECORD_VIEW)
  public CaseResponse act(
      @PathVariable Long id,
      @PathVariable String action,
      @Valid @RequestBody(required = false) ActionRequest request) {
    WorkCase c = workflow.genericTransition(id, action, request == null ? null : request.note());
    return CaseResponse.from(views.view(c), clock.instant());
  }

  /**
   * Takes an unassigned item from my queue.
   *
   * @param id case
   * @return updated item
   */
  @PostMapping("/cases/{id}/claim")
  @PreAuthorize(VIEW)
  public WorkItemResponse claim(@PathVariable Long id) {
    WorkCase c = assignments.claim(id);
    return WorkItemResponse.from(c, definitions.stageOf(c).getName(), clock.instant());
  }

  /**
   * Users an item may be assigned to.
   *
   * @param id case
   * @return user names
   */
  @GetMapping("/cases/{id}/assignees")
  @PreAuthorize("hasAuthority('WORK_ASSIGN')")
  public List<String> assignees(@PathVariable Long id) {
    return queues.eligibleUsers(definitions.stageOf(views.get(id)));
  }

  /**
   * Assigns or re-assigns an item (team leader).
   *
   * @param id case
   * @param request assignee
   * @return updated item
   */
  @PostMapping("/cases/{id}/assign")
  @PreAuthorize("hasAuthority('WORK_ASSIGN')")
  public WorkItemResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
    WorkCase current = views.get(id);
    String assignee =
        request.assignee() == null || request.assignee().isBlank()
            ? null
            : request.assignee().trim();
    WorkCase c =
        assignments.assign(id, assignee, queues.eligibleUsers(definitions.stageOf(current)));
    return WorkItemResponse.from(c, definitions.stageOf(c).getName(), clock.instant());
  }

  /**
   * Stage names of a workflow (timeline display).
   *
   * @param workflowCode workflow
   * @return names by code, in order
   */
  @GetMapping("/definitions/{workflowCode}/stages")
  @PreAuthorize("isAuthenticated()")
  public Map<String, String> stages(@PathVariable String workflowCode) {
    return definitions.stageNames(workflowCode);
  }
}
