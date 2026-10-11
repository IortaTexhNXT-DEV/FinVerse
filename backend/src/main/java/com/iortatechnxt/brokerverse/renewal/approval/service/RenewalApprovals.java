package com.iortatechnxt.brokerverse.renewal.approval.service;

import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.placement.domain.GateRule;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService;
import com.iortatechnxt.brokerverse.renewal.acceptance.service.RenewalProgression;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateDecision;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RemarkService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalBatch;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Review and submission of the accepted renewal accounts (FRRN.26.01): with the second approval on,
 * an accepted account is Submitted for Approval; the approver checks it and approves it - a CBG
 * Motor account then proceeds to placement once its payment is posted, a Non-CBG account needs the
 * client's payment confirmation - or returns it for correction with remarks (Returned Account), and
 * the user resubmits it. An account For Booking Only must be ready or overridden and a
 * Direct-to-Insurer Payment cleared before it is submitted for placement.
 */
@Service
public class RenewalApprovals {

  /** Message of a missing payment confirmation. */
  public static final String CONFIRMATION_REQUIRED =
      "Please select the client's payment confirmation status.";

  private static final String MOTOR = "MOTOR";

  private final RenewalRecords records;
  private final RenewalBatch batch;
  private final RenewalProgression progression;
  private final SubmissionGate gate;
  private final PaymentGateService paymentGate;
  private final AccountRepository accounts;
  private final RenewalParameters renewal;
  private final RenewalNotices notices;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param batch one transaction per account
   * @param progression fast track to the payment gate and placement
   * @param gate tags checked before the placement
   * @param paymentGate the client's payment confirmation of a Non-CBG account
   * @param accounts renewal accounts
   * @param renewal CBG segments
   * @param notices notifications
   * @param currentUser user
   * @param audit audit trail
   * @param clock clock
   */
  public RenewalApprovals(
      RenewalRecords records,
      RenewalBatch batch,
      RenewalProgression progression,
      SubmissionGate gate,
      PaymentGateService paymentGate,
      AccountRepository accounts,
      RenewalParameters renewal,
      RenewalNotices notices,
      CurrentUser currentUser,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.batch = batch;
    this.progression = progression;
    this.gate = gate;
    this.paymentGate = paymentGate;
    this.accounts = accounts;
    this.renewal = renewal;
    this.notices = notices;
    this.currentUser = currentUser;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Approves accounts Submitted for Approval and submits them for placement.
   *
   * @param companyId company
   * @param refs accounts
   * @param paymentConfirmation the client's payment confirmation (required for Non-CBG accounts)
   * @return approved and refused accounts
   */
  public BatchOutcome approve(Long companyId, List<String> refs, String paymentConfirmation) {
    return batch.run(
        refs,
        ref -> {
          RenewalCandidate c = submitted(companyId, ref);
          boolean cbgMotor = cbgMotor(c);
          if (!cbgMotor && (paymentConfirmation == null || paymentConfirmation.isBlank())) {
            throw new BusinessRuleException("RNW_PAYMENT_CONFIRMATION", CONFIRMATION_REQUIRED);
          }
          gate.requireSubmittable(c);
          c.getPlacement()
              .getDecision()
              .paymentConfirmation(cbgMotor ? null : paymentConfirmation.strip());
          c.getPlacement()
              .getDecision()
              .approval(CandidateDecision.APPROVED, null, currentUser.username(), clock.instant());
          audit.record(
              RenewalCodes.ENTITY,
              ref,
              AuditAction.AUTHORIZE,
              "Account successfully approved and submitted for Placement");
          progression.fastTrack(c.getId());
          if (!cbgMotor) {
            confirmPayment(c, paymentConfirmation.strip());
          }
        });
  }

  private void confirmPayment(RenewalCandidate c, String channel) {
    String arn = c.getRenewalArn();
    boolean awaiting =
        arn != null
            && accounts
                .findByArn(arn)
                .map(a -> a.getStatus() == AccountStatus.AWAITING_PAYMENT)
                .orElse(false);
    if (awaiting
        && paymentGate.ruleFor(accounts.findByArn(arn).orElseThrow()).rule()
            != GateRule.PAYMENT_MATCHED) {
      paymentGate.confirmClient(
          arn,
          new PaymentGateService.ClientConfirmation(
              channel, "Client payment confirmation at the approval of the renewal", null));
    }
  }

  /**
   * Submits accepted accounts for placement once their tags are cleared (an account For Booking
   * Only ready or overridden, a Direct-to-Insurer Payment approved).
   *
   * @param companyId company
   * @param refs accepted accounts
   * @return submitted and refused accounts
   */
  public BatchOutcome submitForPlacement(Long companyId, List<String> refs) {
    return batch.run(
        refs,
        ref -> {
          RenewalCandidate c = records.get(companyId, ref);
          RenewalRecords.requireStage(c, RenewalStage.ACCEPTED);
          if (gate.awaitsApproval(c)) {
            throw new BusinessRuleException(
                "RNW_APPROVAL", "The account is Submitted for Approval: approve it first");
          }
          gate.requireSubmittable(c);
          progression.fastTrack(c.getId());
        });
  }

  /**
   * Returns accounts Submitted for Approval for correction.
   *
   * @param companyId company
   * @param refs accounts
   * @param remarks reason
   * @return returned and refused accounts
   */
  public BatchOutcome reject(Long companyId, List<String> refs, String remarks) {
    String why = RemarkService.requireText(remarks, "Enter the reason of the return");
    return batch.run(
        refs,
        ref -> {
          RenewalCandidate c = submitted(companyId, ref);
          c.getPlacement()
              .getDecision()
              .approval(CandidateDecision.RETURNED, why, currentUser.username(), clock.instant());
          c.getFlags().setReturned(true);
          audit.record(
              RenewalCodes.ENTITY, ref, AuditAction.REJECT, "Returned for correction: " + why);
          notices.users(
              Collections.singletonList(c.getAssignedAo()),
              RenewalCodes.EVENT_RETURNED,
              c,
              new RenewalNotices.Text(ref + " returned for correction", why));
        });
  }

  /**
   * Resubmits a returned account for review and approval.
   *
   * @param companyId company
   * @param ref account
   */
  public void resubmit(Long companyId, String ref) {
    batch.run(
        List.of(ref),
        r -> {
          RenewalCandidate c = records.get(companyId, r);
          if (!CandidateDecision.RETURNED.equals(
              c.getPlacement().getDecision().getApprovalStatus())) {
            throw new BusinessRuleException("RNW_APPROVAL", "The account was not returned");
          }
          gate.submit(c);
          c.getFlags().setReturned(false);
        });
  }

  private RenewalCandidate submitted(Long companyId, String ref) {
    RenewalCandidate c = records.get(companyId, ref);
    RenewalRecords.requireStage(c, RenewalStage.ACCEPTED);
    if (!CandidateDecision.SUBMITTED.equals(c.getPlacement().getDecision().getApprovalStatus())) {
      throw new BusinessRuleException("RNW_APPROVAL", "The account is not Submitted for Approval");
    }
    return c;
  }

  private boolean cbgMotor(RenewalCandidate c) {
    var p = c.getSnapshot().product();
    return p != null && renewal.cbgSegment(p.segment()) && MOTOR.equals(p.lineCode());
  }
}
