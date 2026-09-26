package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.PremiumRule.PremiumCheck;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPremiumStatus;
import com.iortatechnxt.brokerverse.brokerclaims.domain.CoverSnapshot;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Premium check and claims authorization code of a claim (BRCLM.001, NFR 15.05;
 * CLAIMS_BROKING_DESIGN 8.3, FR-CL-016). The check reads the invoices of the claim's ARN and policy
 * year from the Operations ledger at recording, on each payment posted to an invoice of the cover
 * and in the daily job {@code BCL_PREMIUM_RECHECK}; an unpaid or partly paid cover raises {@code
 * BCL_UNPAID_PREMIUM_CLAIM}, and the handler is told when it becomes paid. The code {@code
 * CAC-<yyyy>-nnnnnn} is issued only on a PAID check, or on a direct-payment cover under parameter
 * {@code BCL_AUTH_DP_POLICY} (ALLOW; CONFIRM with the insurer's payment evidence attached to the
 * claim; BLOCK). What the code means for BDOI waits for CLQ01.
 */
@Service
@Transactional
public class PremiumCheckService {

  /** Exception code of a claim on unpaid premium. */
  public static final String UNPAID_ALERT = "BCL_UNPAID_PREMIUM_CLAIM";

  /** Error code of an authorization refused for unpaid premium. */
  public static final String PREMIUM_UNPAID = "BCL_PREMIUM_UNPAID";

  private static final String CONFIRM = "CONFIRM";
  private static final String ALLOW = "ALLOW";
  private static final String BLOCK = "BLOCK";

  private final CoverService covers;
  private final BrokerClaimQueryService claims;
  private final AttachmentService attachments;
  private final DocumentNumberService numbers;
  private final SystemParameterService parameters;
  private final AlertService alerts;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param covers cover invoices
   * @param claims claims
   * @param attachments payment evidence
   * @param numbers authorization codes
   * @param parameters direct-payment policy
   * @param alerts unpaid premium alert
   * @param notifications handler notification
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public PremiumCheckService(
      CoverService covers,
      BrokerClaimQueryService claims,
      AttachmentService attachments,
      DocumentNumberService numbers,
      SystemParameterService parameters,
      AlertService alerts,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.covers = covers;
    this.claims = claims;
    this.attachments = attachments;
    this.numbers = numbers;
    this.parameters = parameters;
    this.alerts = alerts;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Checks the premium of a claim's cover and records the result; raises the unpaid premium alert
   * and tells the handler when the premium became paid.
   *
   * @param claim claim
   * @return the result with the invoices not fully paid
   */
  public PremiumCheck check(Claim claim) {
    CoverSnapshot cover = claim.getCover();
    ClaimPremiumStatus before = cover.getPremiumStatus();
    PremiumCheck result = covers.premium(cover.getArn(), cover.getPolicyYear());
    cover.premiumChecked(result.status(), clock.instant());
    if (result.blocking()) {
      alerts.raise(
          UNPAID_ALERT,
          new AlertFacts(
              claim.getCompanyId(),
              claim.getBranchId(),
              ClaimCodes.ENTITY_TYPE,
              String.valueOf(claim.getId()),
              "Claim " + claim.getClaimNo() + " on " + cover.getArn() + " with " + result.status(),
              null,
              UNPAID_ALERT + ":" + claim.getId()));
    } else if (becamePaid(before, result.status())) {
      notifications.notifyUser(
          claim.getHandler(),
          new Notice(
              "Premium of claim " + claim.getClaimNo() + " is paid",
              cover.getArn()
                  + " policy year "
                  + cover.getPolicyYear()
                  + ": the authorization code can be generated",
              "/claims-handling/" + claim.getId(),
              ClaimCodes.ENTITY_TYPE,
              String.valueOf(claim.getId())));
    }
    return result;
  }

  /**
   * Checks the premium of a claim again (screen action and listeners).
   *
   * @param companyId company
   * @param claimId claim
   * @return the result
   */
  public PremiumCheck recheck(Long companyId, Long claimId) {
    return check(claims.require(companyId, claimId));
  }

  /**
   * Issues the claims authorization code (BCL_AUTHORIZE).
   *
   * @param companyId company
   * @param claimId claim
   * @param evidenceAttachmentId insurer payment evidence attached to the claim (direct payment)
   * @return the claim with its code
   */
  public Claim authorize(Long companyId, Long claimId, Long evidenceAttachmentId) {
    Claim claim = claims.requireOpen(companyId, claimId);
    PremiumCheck result = check(claim);
    Long evidence = allowed(claim, result.status(), evidenceAttachmentId);
    LocalDate today = LocalDate.now(clock);
    String code = numbers.next(ClaimCodes.AUTHORIZATION_CODE_PREFIX + "-" + today.getYear());
    claim.getCover().authorize(code, currentUser.username(), clock.instant(), evidence);
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.AUTHORIZE,
        "Authorization code " + code + " (premium " + result.status() + ")");
    return claim;
  }

  private Long allowed(Claim claim, ClaimPremiumStatus status, Long evidenceAttachmentId) {
    if (status == ClaimPremiumStatus.PAID) {
      return null;
    }
    String policy = directPaymentPolicy();
    if (status != ClaimPremiumStatus.DIRECT_PAYMENT || BLOCK.equals(policy)) {
      CoverSnapshot cover = claim.getCover();
      throw new BusinessRuleException(
          PREMIUM_UNPAID,
          "The premium of "
              + cover.getArn()
              + " "
              + cover.getPolicyYear()
              + " is not fully paid. The authorization code cannot be generated");
    }
    if (ALLOW.equals(policy)) {
      return null;
    }
    if (evidenceAttachmentId == null || !onClaim(claim, evidenceAttachmentId)) {
      throw new BusinessRuleException(
          "BCL_DP_EVIDENCE_REQUIRED", "Attach the insurer's payment evidence first");
    }
    return evidenceAttachmentId;
  }

  private String directPaymentPolicy() {
    String policy =
        parameters.text(ClaimCodes.PARAM_AUTH_DP_POLICY, CONFIRM).strip().toUpperCase(Locale.ROOT);
    return ALLOW.equals(policy) || CONFIRM.equals(policy) ? policy : BLOCK;
  }

  private boolean onClaim(Claim claim, Long attachmentId) {
    Attachment file = attachments.get(attachmentId);
    return ClaimCodes.ENTITY_TYPE.equals(file.getEntityType())
        && String.valueOf(claim.getId()).equals(file.getEntityId());
  }

  private static boolean becamePaid(ClaimPremiumStatus before, ClaimPremiumStatus now) {
    boolean wasBlocking =
        before == ClaimPremiumStatus.UNPAID || before == ClaimPremiumStatus.PARTIALLY_PAID;
    return wasBlocking && now == ClaimPremiumStatus.PAID;
  }
}
