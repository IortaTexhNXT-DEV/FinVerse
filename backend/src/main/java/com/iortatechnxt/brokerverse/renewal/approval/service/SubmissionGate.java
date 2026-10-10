package com.iortatechnxt.brokerverse.renewal.approval.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateDecision;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.placement.service.InsuranceAdvices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * The gate between the client's acceptance and the placement (FRRN.014.04, FRRN.014.05,
 * FRRN.26.01): with {@value #PARAMETER} on an accepted account waits Submitted for Approval; an
 * account For Booking Only must be ready or overridden and a Direct-to-Insurer Payment cleared
 * before the account is submitted for placement, and a mortgaged account For Booking Only gets its
 * Insurance Advice then.
 */
@Component
public class SubmissionGate {

  /** Parameter: the second approval after the client's acceptance. */
  public static final String PARAMETER = "RNW_SECOND_APPROVAL";

  private final BookingOnlyAccounts bookingOnly;
  private final DirectToInsurerTags directToInsurer;
  private final InsuranceAdvices advices;
  private final SystemParameterService parameters;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the gate.
   *
   * @param bookingOnly For Booking Only checks
   * @param directToInsurer Direct-to-Insurer Payment checks
   * @param advices Insurance Advice
   * @param parameters second approval switch
   * @param currentUser user
   * @param audit audit trail
   * @param clock clock
   */
  public SubmissionGate(
      BookingOnlyAccounts bookingOnly,
      DirectToInsurerTags directToInsurer,
      InsuranceAdvices advices,
      SystemParameterService parameters,
      CurrentUser currentUser,
      AuditTrailService audit,
      Clock clock) {
    this.bookingOnly = bookingOnly;
    this.directToInsurer = directToInsurer;
    this.advices = advices;
    this.parameters = parameters;
    this.currentUser = currentUser;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Whether the second approval is on.
   *
   * @return switch
   */
  public boolean secondApproval() {
    return "true".equals(parameters.text(PARAMETER, "false").strip());
  }

  /**
   * Whether an accepted account still waits for its approval; puts it Submitted for Approval when
   * it has no approval status yet.
   *
   * @param c accepted renewal
   * @return true while it waits
   */
  public boolean awaitsApproval(RenewalCandidate c) {
    if (!secondApproval()) {
      return false;
    }
    String status = c.getPlacement().getDecision().getApprovalStatus();
    if (status == null) {
      submit(c);
    }
    return !CandidateDecision.APPROVED.equals(c.getPlacement().getDecision().getApprovalStatus());
  }

  /**
   * Puts an accepted account Submitted for Approval.
   *
   * @param c accepted renewal
   */
  public void submit(RenewalCandidate c) {
    c.getPlacement()
        .getDecision()
        .approval(
            CandidateDecision.SUBMITTED,
            null,
            currentUser.optionalUsername().orElse("SYSTEM"),
            clock.instant());
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.SUBMIT,
        "Renewal account submitted for review and approval");
  }

  /**
   * Checks the tags of an account before its submission for placement.
   *
   * @param c renewal
   */
  public void requireSubmittable(RenewalCandidate c) {
    directToInsurer.requireCleared(c);
    bookingOnly.requireReady(c);
    if (c.getPlacement().isForBookingOnly()) {
      advices.afterPlacement(c, "FOR_BOOKING_ONLY");
    }
  }
}
