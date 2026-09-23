package com.iortatechnxt.finverse.underwriting.api;

import com.iortatechnxt.finverse.common.api.PageResponse;
import com.iortatechnxt.finverse.common.api.ReasonRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.ApprovalRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.EndorsementRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.EndorsementResponse;
import com.iortatechnxt.finverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.PolicyResponse;
import com.iortatechnxt.finverse.underwriting.api.dto.PolicySearchParams;
import com.iortatechnxt.finverse.underwriting.api.dto.PremiumResponse;
import com.iortatechnxt.finverse.underwriting.service.EndorsementService;
import com.iortatechnxt.finverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.finverse.underwriting.service.PolicyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST API for policies: inquiry, premium preview, maker and checker actions, endorsements. */
@RestController
@RequestMapping("/api/v1/underwriting/policies")
public class PolicyController {

  private static final int MAX_PAGE_SIZE = 200;
  private static final String VIEW = "hasAuthority('POLICY_VIEW')";
  private static final String MAINTAIN = "hasAuthority('POLICY_MAINTAIN')";
  private static final String AUTHORIZE = "hasAuthority('POLICY_AUTHORIZE')";

  private final PolicyService policies;
  private final PolicyApprovalService approvals;
  private final EndorsementService endorsements;

  /**
   * Creates the controller.
   *
   * @param policies maker services
   * @param approvals checker services
   * @param endorsements endorsement services
   */
  public PolicyController(
      PolicyService policies, PolicyApprovalService approvals, EndorsementService endorsements) {
    this.policies = policies;
    this.approvals = approvals;
    this.endorsements = endorsements;
  }

  /**
   * Searches policies.
   *
   * @param params filters
   * @param page page index
   * @param size page size
   * @return page of policies
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<PolicyResponse> search(
      @Valid @ModelAttribute PolicySearchParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    var pageable =
        PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "id"));
    return PageResponse.of(policies.search(params.toCriteria(), pageable), PolicyResponse::summary);
  }

  /**
   * Gets a policy with risks.
   *
   * @param id id
   * @return policy
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public PolicyResponse get(@PathVariable Long id) {
    return PolicyResponse.withRisks(policies.get(id));
  }

  /**
   * Endorsement history of a policy.
   *
   * @param id policy
   * @return endorsements
   */
  @GetMapping("/{id}/endorsements")
  @PreAuthorize(VIEW)
  public List<EndorsementResponse> endorsements(@PathVariable Long id) {
    return endorsements.forPolicy(id).stream().map(EndorsementResponse::from).toList();
  }

  /**
   * Computes the premium of a policy request without saving it.
   *
   * @param request request
   * @return premium figures
   */
  @PostMapping("/preview")
  @PreAuthorize(MAINTAIN)
  public PremiumResponse preview(@Valid @RequestBody PolicyRequest request) {
    return PremiumResponse.from(policies.preview(request));
  }

  /**
   * Creates a draft policy.
   *
   * @param request request
   * @return policy
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public PolicyResponse create(@Valid @RequestBody PolicyRequest request) {
    return PolicyResponse.withRisks(policies.create(request));
  }

  /**
   * Updates a draft policy.
   *
   * @param id id
   * @param request request
   * @return policy
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  public PolicyResponse update(@PathVariable Long id, @Valid @RequestBody PolicyRequest request) {
    return PolicyResponse.withRisks(policies.update(id, request));
  }

  /**
   * Submits a policy for approval.
   *
   * @param id id
   * @return policy
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(MAINTAIN)
  public PolicyResponse submit(@PathVariable Long id) {
    return PolicyResponse.withRisks(policies.submit(id));
  }

  /**
   * Discards a draft policy.
   *
   * @param id id
   * @return policy
   */
  @PostMapping("/{id}/discard")
  @PreAuthorize(MAINTAIN)
  public PolicyResponse discard(@PathVariable Long id) {
    return PolicyResponse.withRisks(policies.discard(id));
  }

  /**
   * Approves a policy and posts its premium.
   *
   * @param id id
   * @param request options (accounting date)
   * @return policy
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(AUTHORIZE)
  public PolicyResponse approve(
      @PathVariable Long id, @RequestBody(required = false) ApprovalRequest request) {
    return PolicyResponse.withRisks(
        approvals.approvePolicy(id, request == null ? null : request.accountingDate()));
  }

  /**
   * Returns a policy to its maker.
   *
   * @param id id
   * @param request reason
   * @return policy
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(AUTHORIZE)
  public PolicyResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return PolicyResponse.withRisks(approvals.rejectPolicy(id, request.reason()));
  }

  /**
   * Creates a draft endorsement on an approved policy.
   *
   * @param id policy
   * @param request request
   * @return endorsement
   */
  @PostMapping("/{id}/endorsements")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public EndorsementResponse endorse(
      @PathVariable Long id, @Valid @RequestBody EndorsementRequest request) {
    return EndorsementResponse.from(endorsements.create(id, request));
  }
}
