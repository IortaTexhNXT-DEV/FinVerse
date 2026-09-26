package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimProgress;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.StatusHistory;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The status engine of a claim (BRCLM.010-013/019/027/035, FR-CL-041/042/045/050;
 * CLAIMS_BROKING_DESIGN 8.1). A status must be effective, have a phase and be allowed by the status
 * access matrix for the user's roles and claims unit. Each change writes the status history,
 * restarts "age this stage", recomputes the next follow-up date unless an override is still ahead,
 * moves the workflow stage with the phase and publishes {@code ClaimStatusChanged}. A status of
 * phase TEMP_CLOSED closes the claim temporarily; any in-progress status resumes it.
 *
 * <p>Decision on the workflow (CL0 left it to CL1-B): {@code BCL_CLAIM} has no way back to NEW, so
 * a newly filed status is refused once the claim has left phase NEW ({@code
 * BCL_STATUS_BACK_TO_NEW}); between the two newly filed statuses the claim stays in NEW.
 */
@Service
@Transactional
public class ClaimStatusService {

  /** What a closed claim refuses for a status change. */
  static final String THE_STATUS = "the status";

  private final ClaimLookup lookup;
  private final StatusRules rules;
  private final StatusMatrix matrix;
  private final StatusTransitions transitions;
  private final LovService lovs;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lookup claims of the company
   * @param rules status attributes
   * @param matrix status access matrix
   * @param transitions history, workflow, audit, notice and event of a change
   * @param lovs lists of values
   * @param currentUser current user
   * @param clock clock
   */
  public ClaimStatusService(
      ClaimLookup lookup,
      StatusRules rules,
      StatusMatrix matrix,
      StatusTransitions transitions,
      LovService lovs,
      CurrentUser currentUser,
      Clock clock) {
    this.lookup = lookup;
    this.rules = rules;
    this.matrix = matrix;
    this.transitions = transitions;
    this.lovs = lovs;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Sets the first status of a claim being recorded (FR-CL-042 R1): called by the recording service
   * of CL1-A inside its transaction, after the claim is saved and its {@code BCL_RECORD} permission
   * checked (through {@code ClaimRecordedListener}). The recording rules choose the first status
   * (a newly filed one), so the status access matrix is not applied to it. Opens or aligns the
   * workflow case in the stage of the status's phase and publishes {@code ClaimStatusChanged} with
   * {@code from = null}.
   *
   * @param claim saved claim without a status
   * @param statusCode first status
   * @param remark remark, may be null
   * @return the claim
   */
  public Claim recordInitialStatus(Claim claim, String statusCode, String remark) {
    if (claim.getProgress().getStatusCode() != null) {
      throw new BusinessRuleException(
          "BCL_STATUS_ALREADY_SET", "Claim " + claim.getClaimNo() + " already has a status");
    }
    apply(claim, statusCode, remark, false);
    return claim;
  }

  /**
   * Changes the status of a claim (FR-CL-042).
   *
   * @param companyId company
   * @param claimId claim
   * @param statusCode new status
   * @param remark remark, may be null
   * @return the claim
   */
  public Claim change(Long companyId, Long claimId, String statusCode, String remark) {
    Claim claim = lookup.require(companyId, claimId);
    ClaimLookup.requireOpen(claim, THE_STATUS);
    if (statusCode != null && statusCode.equals(claim.getProgress().getStatusCode())) {
      throw new BusinessRuleException(
          "BCL_STATUS_UNCHANGED", "The claim already has the status " + label(statusCode));
    }
    apply(claim, statusCode, remark, true);
    return claim;
  }

  /**
   * The statuses the current user may set on a claim (FR-CL-041: the Change Status drop-down).
   *
   * @param companyId company
   * @param claimId claim
   * @return effective statuses of the matrix with a usable phase, in list order
   */
  @Transactional(readOnly = true)
  public List<StatusOption> allowedStatuses(Long companyId, Long claimId) {
    Claim claim = lookup.require(companyId, claimId);
    ClaimLookup.requireOpen(claim, THE_STATUS);
    Set<String> allowed = matrix.allowedStatuses(currentUser.username());
    boolean leftNew = claim.getProgress().getPhase() != ClaimPhase.NEW;
    return lovs.activeValues(ClaimCodes.LOV_STATUS, today()).stream()
        .filter(v -> allowed.contains(v.getCode()) && rules.hasPhase(v.getCode()))
        .map(v -> new StatusOption(v.getCode(), v.getLabel(), rules.phaseOf(v.getCode())))
        .filter(o -> !(leftNew && o.phase() == ClaimPhase.NEW))
        .filter(o -> !o.code().equals(claim.getProgress().getStatusCode()))
        .toList();
  }

  private void apply(Claim claim, String statusCode, String remark, boolean checkMatrix) {
    if (statusCode == null || statusCode.isBlank()) {
      throw new BusinessRuleException("BCL_STATUS_REQUIRED", "Select the new status");
    }
    LovValue status = lovs.requireValid(ClaimCodes.LOV_STATUS, statusCode, today());
    ClaimPhase phase = rules.phaseOf(statusCode);
    ClaimProgress progress = claim.getProgress();
    ClaimPhase current = progress.getPhase();
    if (phase == ClaimPhase.NEW && current != ClaimPhase.NEW) {
      throw new BusinessRuleException(
          "BCL_STATUS_BACK_TO_NEW",
          "Claim "
              + claim.getClaimNo()
              + " has left the newly filed phase; set an in-progress or temporary closure status");
    }
    if (checkMatrix && !CurrentUser.SYSTEM.equals(currentUser.username())) {
      matrix.requireAllowed(currentUser.username(), statusCode, status.getLabel());
    }
    StatusHistory.Step from = new StatusHistory.Step(progress.getStatusCode(), current);
    StatusHistory.Step before =
        progress.getStatusCode() == null ? new StatusHistory.Step(null, null) : from;
    Instant since = progress.getStatusSince();
    progress.changeStatus(statusCode, phase, clock.instant());
    LocalDate today = today();
    progress.scheduleFollowUp(today.plusDays(rules.followUpDays(statusCode)), today);
    transitions.record(claim, before, since, TransitionNote.comment(remark));
  }

  private String label(String statusCode) {
    return rules.statusLabel(statusCode);
  }

  private LocalDate today() {
    return ClaimAgeing.today(clock);
  }

  /**
   * A status the user may select.
   *
   * @param code status code
   * @param label label
   * @param phase phase the claim moves to
   */
  public record StatusOption(String code, String label, ClaimPhase phase) {}
}
