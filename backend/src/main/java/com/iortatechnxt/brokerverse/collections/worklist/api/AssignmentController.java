package com.iortatechnxt.brokerverse.collections.worklist.api;

import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AssignmentDtos.ActivateRequest;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AssignmentDtos.ReassignRequest;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AssignmentDtos.ReassignedResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AssignmentDtos.RuleRequest;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AssignmentDtos.RuleResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AssignmentDtos.SelectionResponse;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentRuleService;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService;
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
 * Assignment of collection accounts (BRCLXN.052): default assignment rules, and reassignment of
 * selected accounts or of the accounts matching criteria (client, unit, aging, amount) with a
 * preview, permanent or temporary.
 */
@RestController
@RequestMapping("/api/v1/collections")
public class AssignmentController {

  private final AssignmentRuleService rules;
  private final AssignmentService assignments;

  /**
   * Creates the controller.
   *
   * @param rules assignment rules
   * @param assignments reassignment
   */
  public AssignmentController(AssignmentRuleService rules, AssignmentService assignments) {
    this.rules = rules;
    this.assignments = assignments;
  }

  /**
   * The assignment rules in priority order.
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping("/assignment-rules")
  @PreAuthorize(ClxAccess.ASSIGN)
  public List<RuleResponse> rules(@RequestParam Long companyId) {
    return rules.list(companyId).stream().map(RuleResponse::from).toList();
  }

  /**
   * Adds a rule.
   *
   * @param companyId company
   * @param request rule
   * @return the rule
   */
  @PostMapping("/assignment-rules")
  @PreAuthorize(ClxAccess.ASSIGN)
  public RuleResponse createRule(
      @RequestParam Long companyId, @Valid @RequestBody RuleRequest request) {
    return RuleResponse.from(rules.create(companyId, request.toDetails()));
  }

  /**
   * Changes a rule.
   *
   * @param id rule
   * @param request rule
   * @return the rule
   */
  @PutMapping("/assignment-rules/{id}")
  @PreAuthorize(ClxAccess.ASSIGN)
  public RuleResponse updateRule(@PathVariable Long id, @Valid @RequestBody RuleRequest request) {
    return RuleResponse.from(rules.update(id, request.toDetails()));
  }

  /**
   * Activates or deactivates a rule.
   *
   * @param id rule
   * @param request new state
   * @return the rule
   */
  @PostMapping("/assignment-rules/{id}/active")
  @PreAuthorize(ClxAccess.ASSIGN)
  public RuleResponse activate(@PathVariable Long id, @Valid @RequestBody ActivateRequest request) {
    return RuleResponse.from(rules.activate(id, request.active()));
  }

  /**
   * The accounts a reassignment would move.
   *
   * @param companyId company
   * @param request selection
   * @return total and first accounts
   */
  @PostMapping("/reassignments/preview")
  @PreAuthorize(ClxAccess.ASSIGN)
  public SelectionResponse preview(
      @RequestParam Long companyId, @Valid @RequestBody ReassignRequest request) {
    return SelectionResponse.from(assignments.preview(companyId, request.selection()));
  }

  /**
   * Reassigns the selected accounts.
   *
   * @param companyId company
   * @param request selection, handler, kind, end date and reason
   * @return bulk reference and accounts moved
   */
  @PostMapping("/reassignments")
  @PreAuthorize(ClxAccess.ASSIGN)
  public ReassignedResponse reassign(
      @RequestParam Long companyId, @Valid @RequestBody ReassignRequest request) {
    return ReassignedResponse.from(
        assignments.reassign(companyId, request.selection(), request.command()));
  }
}
