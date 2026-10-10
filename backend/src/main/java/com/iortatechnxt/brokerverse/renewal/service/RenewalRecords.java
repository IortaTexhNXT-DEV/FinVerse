package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignment;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalAssignmentRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds renewals for the services and controllers: by reference in the user's data scope
 * (FR-RN-002), and the common state guards (open, stage, Marketing lock).
 */
@Component
@Transactional(readOnly = true)
public class RenewalRecords {

  private final RenewalCandidateRepository candidates;
  private final RenewalAssignmentRepository assignments;
  private final RenewalScope scope;
  private final CurrentUser currentUser;

  /**
   * Creates the look-up.
   *
   * @param candidates candidates
   * @param assignments assignments (AO scope)
   * @param scope data scope
   * @param currentUser current user
   */
  public RenewalRecords(
      RenewalCandidateRepository candidates,
      RenewalAssignmentRepository assignments,
      RenewalScope scope,
      CurrentUser currentUser) {
    this.candidates = candidates;
    this.assignments = assignments;
    this.scope = scope;
    this.currentUser = currentUser;
  }

  /**
   * A renewal of the user's scope by reference.
   *
   * @param companyId company
   * @param renewalRef renewal reference
   * @return candidate
   */
  public RenewalCandidate get(Long companyId, String renewalRef) {
    RenewalCandidate candidate =
        candidates
            .findByCompanyIdAndRenewalRef(companyId, renewalRef)
            .orElseThrow(() -> new ResourceNotFoundException("Renewal", renewalRef));
    requireScope(candidate);
    return candidate;
  }

  /**
   * A renewal by id, without the scope (jobs, events).
   *
   * @param id candidate id
   * @return candidate
   */
  public RenewalCandidate byId(Long id) {
    return candidates.findById(id).orElseThrow(() -> new ResourceNotFoundException("Renewal", id));
  }

  /**
   * Refuses a renewal outside the user's scope.
   *
   * @param candidate candidate
   */
  public void requireScope(RenewalCandidate candidate) {
    boolean assigned =
        currentUser.optionalUsername().isPresent()
            && assignments.existsByCandidateIdAndUsernameAndRole(
                candidate.getId(), currentUser.username(), RenewalAssignment.Role.AO);
    scope.require(candidate, assigned);
  }

  /**
   * Refuses the action unless the renewal is in one of the stages.
   *
   * @param candidate candidate
   * @param stages allowed stages
   */
  public static void requireStage(RenewalCandidate candidate, RenewalStage... stages) {
    for (RenewalStage stage : stages) {
      if (candidate.getStage() == stage) {
        return;
      }
    }
    throw new BusinessRuleException(
        "RNW_STAGE_INVALID",
        "Renewal "
            + candidate.getRenewalRef()
            + " cannot be updated in stage "
            + candidate.getStage().label()
            + "; allowed: "
            + Arrays.stream(stages).map(RenewalStage::label).collect(Collectors.joining(", ")));
  }

  /**
   * Refuses a change on the Marketing side once the Renewal Advice is generated (BRD 2.004.9).
   *
   * @param candidate candidate
   */
  public static void requireUnlocked(RenewalCandidate candidate) {
    boolean stageLocked =
        candidate.getStage().isMarketingLocked()
            && !candidate.getPlacement().getTags().isLockAtPlacement();
    if (candidate.getMarketingLockedAt() != null || stageLocked) {
      throw new BusinessRuleException(
          "RENEWAL_LOCKED",
          "Renewal "
              + candidate.getRenewalRef()
              + " is locked since the Renewal Advice was generated");
    }
  }
}
