package com.iortatechnxt.brokerverse.brokerclaims.status.api;

import com.iortatechnxt.brokerverse.brokerclaims.status.api.dto.StatusDtos.ActionPlanRequest;
import com.iortatechnxt.brokerverse.brokerclaims.status.api.dto.StatusDtos.AdjusterRequest;
import com.iortatechnxt.brokerverse.brokerclaims.status.api.dto.StatusDtos.ClaimProgressResponse;
import com.iortatechnxt.brokerverse.brokerclaims.status.api.dto.StatusDtos.FollowUpRequest;
import com.iortatechnxt.brokerverse.brokerclaims.status.api.dto.StatusDtos.ReopenRequest;
import com.iortatechnxt.brokerverse.brokerclaims.status.api.dto.StatusDtos.SettlementRequest;
import com.iortatechnxt.brokerverse.brokerclaims.status.api.dto.StatusDtos.StatusRequest;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimClosureService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimFollowUpService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimProgressQuery;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimStatusService.StatusOption;
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
 * Status actions and History of a claim (BRCLM.005/010-021/027/035; FR-CM-041-045/050/051/053):
 * every call names the company of the claim, so a claim of another company is "not found".
 */
@RestController
@RequestMapping("/api/v1/broker-claims/{id}")
public class ClaimStatusController {

  private final ClaimStatusService statuses;
  private final ClaimClosureService closures;
  private final ClaimFollowUpService followUps;
  private final ClaimProgressQuery query;

  /**
   * Creates the controller.
   *
   * @param statuses status engine
   * @param closures settlement, closure and reopen
   * @param followUps follow-up, action plan and adjuster
   * @param query progress and history
   */
  public ClaimStatusController(
      ClaimStatusService statuses,
      ClaimClosureService closures,
      ClaimFollowUpService followUps,
      ClaimProgressQuery query) {
    this.statuses = statuses;
    this.closures = closures;
    this.followUps = followUps;
    this.query = query;
  }

  /**
   * Where the claim stands, with its ages.
   *
   * @param id claim
   * @param companyId company
   * @return progress
   */
  @GetMapping("/progress")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public ClaimProgressQuery.Progress progress(@PathVariable Long id, @RequestParam Long companyId) {
    return query.progress(companyId, id);
  }

  /**
   * The History tab: status changes and field changes.
   *
   * @param id claim
   * @param companyId company
   * @return history
   */
  @GetMapping("/history")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public ClaimProgressQuery.History history(@PathVariable Long id, @RequestParam Long companyId) {
    return query.history(companyId, id);
  }

  /**
   * The statuses the user may set (status access matrix).
   *
   * @param id claim
   * @param companyId company
   * @return statuses
   */
  @GetMapping("/allowed-statuses")
  @PreAuthorize("hasAnyAuthority('BCL_STATUS_UPDATE', 'BCL_RECORD')")
  public List<StatusOption> allowedStatuses(@PathVariable Long id, @RequestParam Long companyId) {
    return statuses.allowedStatuses(companyId, id);
  }

  /**
   * Changes the status.
   *
   * @param id claim
   * @param companyId company
   * @param request status and remark
   * @return progress
   */
  @PostMapping("/status")
  @PreAuthorize("hasAuthority('BCL_STATUS_UPDATE')")
  public ClaimProgressResponse changeStatus(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody StatusRequest request) {
    return ClaimProgressResponse.from(
        statuses.change(companyId, id, request.statusCode(), request.remark()));
  }

  /**
   * Sets the requested type of settlement; a closing type also needs BCL_CLOSE.
   *
   * @param id claim
   * @param companyId company
   * @param request type, amount, date and remark
   * @return progress
   */
  @PostMapping("/settlement")
  @PreAuthorize("hasAuthority('BCL_SETTLEMENT_UPDATE')")
  public ClaimProgressResponse settle(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody SettlementRequest request) {
    return ClaimProgressResponse.from(closures.settle(companyId, id, request.toSettlement()));
  }

  /**
   * Reopens a permanently closed claim.
   *
   * @param id claim
   * @param companyId company
   * @param request reason and remark
   * @return progress
   */
  @PostMapping("/reopen")
  @PreAuthorize("hasAuthority('BCL_REOPEN')")
  public ClaimProgressResponse reopen(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody ReopenRequest request) {
    return ClaimProgressResponse.from(
        closures.reopen(companyId, id, request.reasonCode(), request.remark()));
  }

  /**
   * Overrides the next follow-up date.
   *
   * @param id claim
   * @param companyId company
   * @param request date and reason
   * @return progress
   */
  @PostMapping("/follow-up")
  @PreAuthorize("hasAuthority('BCL_FOLLOW_UP_OVERRIDE')")
  public ClaimProgressResponse overrideFollowUp(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody FollowUpRequest request) {
    return ClaimProgressResponse.from(
        followUps.overrideFollowUp(companyId, id, request.date(), request.reasonCode()));
  }

  /**
   * Encodes the next action plan summary.
   *
   * @param id claim
   * @param companyId company
   * @param request text
   * @return progress
   */
  @PutMapping("/action-plan")
  @PreAuthorize("hasAuthority('BCL_ACTION_PLAN')")
  public ClaimProgressResponse planNextAction(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody ActionPlanRequest request) {
    return ClaimProgressResponse.from(followUps.planNextAction(companyId, id, request.text()));
  }

  /**
   * Sets or clears the adjuster of the claim.
   *
   * @param id claim
   * @param companyId company
   * @param request adjuster and remark
   * @return progress
   */
  @PutMapping("/adjuster")
  @PreAuthorize("hasAuthority('BCL_ADJUSTER_ASSIGN')")
  public ClaimProgressResponse assignAdjuster(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody AdjusterRequest request) {
    return ClaimProgressResponse.from(
        followUps.assignAdjuster(companyId, id, request.adjusterCode(), request.remark()));
  }
}
