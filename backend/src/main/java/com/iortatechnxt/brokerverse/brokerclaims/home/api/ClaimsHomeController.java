package com.iortatechnxt.brokerverse.brokerclaims.home.api;

import com.iortatechnxt.brokerverse.brokerclaims.home.service.ClaimAssignmentService;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.ClaimAssignmentService.Assignee;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.ClaimsHomeService;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery;
import com.iortatechnxt.brokerverse.brokerclaims.home.service.WorklistQuery.WorklistRow;
import com.iortatechnxt.brokerverse.brokerclaims.service.ClaimExperienceQueryService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Claims home, worklist and reassignment (NFR 15.03, BRCLM.034/043, FR-CM-055), and the loss
 * experience of a cover for the account page and Marketing (BRCLM.040, FR-CM-065).
 */
@RestController
@RequestMapping("/api/v1/broker-claims")
public class ClaimsHomeController {

  private static final int MAX_PAGE = 200;

  private final ClaimsHomeService home;
  private final WorklistQuery worklist;
  private final ClaimAssignmentService assignments;
  private final ClaimExperienceQueryService experience;

  /**
   * Creates the controller.
   *
   * @param home Claims home
   * @param worklist worklist
   * @param assignments reassignment
   * @param experience loss experience
   */
  public ClaimsHomeController(
      ClaimsHomeService home,
      WorklistQuery worklist,
      ClaimAssignmentService assignments,
      ClaimExperienceQueryService experience) {
    this.home = home;
    this.worklist = worklist;
    this.assignments = assignments;
    this.experience = experience;
  }

  /**
   * The Claims home of the signed-in user.
   *
   * @param companyId company
   * @return tiles, open claims by status and ageing buckets
   */
  @GetMapping("/home")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public ClaimsHomeService.Home home(@RequestParam Long companyId) {
    return home.home(companyId);
  }

  /**
   * A page of the worklist.
   *
   * @param companyId company
   * @param tab tab
   * @param flag home tile filter
   * @param status status
   * @param q search text
   * @param page page
   * @param size page size (at most 200)
   * @return claims
   */
  @GetMapping("/worklist")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public PageResponse<WorklistRow> worklist(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "OPEN") WorklistQuery.Tab tab,
      @RequestParam(required = false) WorklistQuery.Flag flag,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return worklist.search(
        companyId,
        new WorklistQuery.WorklistCriteria(tab, flag, status, q),
        Math.max(0, page),
        Math.clamp(size, 1, MAX_PAGE));
  }

  /**
   * The users claims may be assigned to.
   *
   * @return handlers with unit and team
   */
  @GetMapping("/assignees")
  @PreAuthorize("hasAnyAuthority('WORK_ASSIGN', 'BCL_RECORD')")
  public List<Assignee> assignees() {
    return assignments.assignees();
  }

  /**
   * Reassigns claims to another handler.
   *
   * @param companyId company
   * @param request claims, handler and comment
   * @return number of claims moved
   */
  @PostMapping("/reassign")
  @PreAuthorize("hasAuthority('WORK_ASSIGN') and hasAuthority('BCL_VIEW')")
  public Map<String, Integer> reassign(
      @RequestParam Long companyId, @RequestBody ReassignRequest request) {
    return Map.of(
        "moved",
        assignments.reassign(companyId, request.claimIds(), request.handler(), request.comment()));
  }

  /**
   * The loss experience of a cover (account page Claims tab, Marketing, Renewal).
   *
   * @param arn account
   * @param policyYear policy year, all years when absent
   * @return counts and amounts
   */
  @GetMapping("/experience")
  @PreAuthorize("hasAnyAuthority('BCL_VIEW', 'BCL_REPORT_VIEW')")
  public ClaimExperienceQueryService.ClaimExperience experience(
      @RequestParam String arn, @RequestParam(required = false) Integer policyYear) {
    return experience.summary(arn, policyYear);
  }

  /**
   * Reassign request.
   *
   * @param claimIds claims
   * @param handler new handler
   * @param comment comment
   */
  public record ReassignRequest(List<Long> claimIds, String handler, String comment) {}
}
