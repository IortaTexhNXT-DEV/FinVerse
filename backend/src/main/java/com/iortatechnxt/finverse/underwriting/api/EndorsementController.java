package com.iortatechnxt.finverse.underwriting.api;

import com.iortatechnxt.finverse.common.api.ReasonRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.ApprovalRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.EndorsementResponse;
import com.iortatechnxt.finverse.underwriting.service.EndorsementService;
import com.iortatechnxt.finverse.underwriting.service.PolicyApprovalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API for endorsements (created from their policy, see {@link PolicyController}). */
@RestController
@RequestMapping("/api/v1/underwriting/endorsements")
public class EndorsementController {

  private static final String MAINTAIN = "hasAuthority('POLICY_MAINTAIN')";
  private static final String AUTHORIZE = "hasAuthority('POLICY_AUTHORIZE')";

  private final EndorsementService endorsements;
  private final PolicyApprovalService approvals;

  /**
   * Creates the controller.
   *
   * @param endorsements maker services
   * @param approvals checker services
   */
  public EndorsementController(EndorsementService endorsements, PolicyApprovalService approvals) {
    this.endorsements = endorsements;
    this.approvals = approvals;
  }

  /**
   * Gets an endorsement.
   *
   * @param id id
   * @return endorsement
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('POLICY_VIEW')")
  public EndorsementResponse get(@PathVariable Long id) {
    return EndorsementResponse.from(endorsements.get(id));
  }

  /**
   * Submits an endorsement.
   *
   * @param id id
   * @return endorsement
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(MAINTAIN)
  public EndorsementResponse submit(@PathVariable Long id) {
    return EndorsementResponse.from(endorsements.submit(id));
  }

  /**
   * Discards a draft endorsement.
   *
   * @param id id
   * @return endorsement
   */
  @PostMapping("/{id}/discard")
  @PreAuthorize(MAINTAIN)
  public EndorsementResponse discard(@PathVariable Long id) {
    return EndorsementResponse.from(endorsements.discard(id));
  }

  /**
   * Approves an endorsement and posts its premium.
   *
   * @param id id
   * @param request options (accounting date)
   * @return endorsement
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(AUTHORIZE)
  public EndorsementResponse approve(
      @PathVariable Long id, @RequestBody(required = false) ApprovalRequest request) {
    return EndorsementResponse.from(
        approvals.approveEndorsement(id, request == null ? null : request.accountingDate()));
  }

  /**
   * Returns an endorsement to its maker.
   *
   * @param id id
   * @param request reason
   * @return endorsement
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(AUTHORIZE)
  public EndorsementResponse reject(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return EndorsementResponse.from(approvals.rejectEndorsement(id, request.reason()));
  }
}
