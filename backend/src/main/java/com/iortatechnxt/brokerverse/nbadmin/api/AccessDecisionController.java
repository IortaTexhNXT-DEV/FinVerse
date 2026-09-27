package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessDecisionResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessRequestResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.DecisionRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessDecisionService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessImplementationService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestDescriber;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestReturnService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService.Decision;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Decisions on access requests (BRD 1.006-1.007, 2.002, 3.002; UAM-NFR-40): approve, second
 * approval, reject, return, resubmit, cancel, and the implementation of approved group-profile
 * requests by the System Administrator (BRD-11 p.6).
 */
@RestController
@RequestMapping("/api/v1/nbadmin/access-requests")
public class AccessDecisionController {

  private static final String APPROVE = "hasAuthority('ACCESS_APPROVE')";
  private static final String DECIDE = "hasAnyAuthority('ACCESS_APPROVE', 'UAM_SECOND_APPROVE')";

  private final AccessDecisionService decisions;
  private final AccessRequestReturnService returns;
  private final AccessImplementationService implementations;
  private final AccessRequestDescriber describer;

  /**
   * Creates the controller.
   *
   * @param decisions approvals and rejections
   * @param returns return, resubmission and cancellation
   * @param implementations implementation of group-profile requests
   * @param describer descriptions of the requests
   */
  public AccessDecisionController(
      AccessDecisionService decisions,
      AccessRequestReturnService returns,
      AccessImplementationService implementations,
      AccessRequestDescriber describer) {
    this.describer = describer;
    this.decisions = decisions;
    this.returns = returns;
    this.implementations = implementations;
  }

  /**
   * Approves a request as its current approver (FR-UA-031).
   *
   * @param id request
   * @param body optional comment
   * @return decision with the temporary password of a created user
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(APPROVE)
  public AccessDecisionResponse approve(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return decision(decisions.approve(id, body.comment()));
  }

  /**
   * Second approval of a flagged request (UAM-NFR-40; FR-UA-034).
   *
   * @param id request
   * @param body optional comment
   * @return decision
   */
  @PostMapping("/{id}/second-approve")
  @PreAuthorize("hasAuthority('UAM_SECOND_APPROVE')")
  public AccessDecisionResponse secondApprove(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return decision(decisions.secondApprove(id, body.comment()));
  }

  /**
   * Rejects a request (FR-UA-032).
   *
   * @param id request
   * @param body reason
   * @return decision
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(DECIDE)
  public AccessDecisionResponse reject(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return decision(decisions.reject(id, body.comment()));
  }

  /**
   * Returns a request to its requester with remarks (BRD 2.002.7; FR-UA-033).
   *
   * @param id request
   * @param body remarks
   * @return the returned request
   */
  @PostMapping("/{id}/return")
  @PreAuthorize(DECIDE)
  public AccessRequestResponse returnRequest(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return view(returns.returnRequest(id, body.comment()));
  }

  /**
   * Resubmits a returned request with a corrected justification to the same approvers (requester;
   * the compatible correction of BASAU 2.6.x).
   *
   * @param id request
   * @param body new justification
   * @return the pending request
   */
  @PostMapping("/{id}/resubmit")
  @PreAuthorize("hasAnyAuthority('ACCESS_REQUEST', 'UAM_CORRECT')")
  public AccessRequestResponse resubmit(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return view(returns.resubmit(id, body.comment()));
  }

  /**
   * Cancels a request with a reason (BRD 1.007; FR-UA-017).
   *
   * @param id request
   * @param body reason
   * @return the cancelled request
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyAuthority('ACCESS_REQUEST', 'UAM_CANCEL', 'ACCESS_APPROVE')")
  public AccessRequestResponse cancel(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
    return view(returns.cancel(id, body.comment()));
  }

  /**
   * Implements an approved group-profile request (FR-UA-045; System Administrator).
   *
   * @param id request
   * @return the implemented request
   */
  @PostMapping("/{id}/implement")
  @PreAuthorize("hasAuthority('ROLE_MANAGE')")
  public AccessRequestResponse implement(@PathVariable Long id) {
    return view(implementations.implement(id));
  }

  private AccessRequestResponse view(AccessRequest r) {
    return AccessRequestResponse.from(r, describer.describe(r));
  }

  private AccessDecisionResponse decision(Decision d) {
    return AccessDecisionResponse.from(d, describer.describe(d.request()));
  }
}
