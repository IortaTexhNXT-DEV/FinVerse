package com.iortatechnxt.brokerverse.renewal.approval.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateTags;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RemarkService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direct-to-Insurer Payment (FRRN.014.05): the tag with its option, With Blanket Approval (the
 * account proceeds) or Route for UH Approval (the Unit Head of the account approves, or rejects
 * with a reason, after which the account proceeds only once the tag is removed).
 */
@Service
public class DirectToInsurerTags {

  private final RenewalRecords records;
  private final RenewalNotices notices;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param notices notification of the Unit Head
   * @param currentUser user
   * @param audit audit trail
   */
  public DirectToInsurerTags(
      RenewalRecords records,
      RenewalNotices notices,
      CurrentUser currentUser,
      AuditTrailService audit) {
    this.records = records;
    this.notices = notices;
    this.currentUser = currentUser;
    this.audit = audit;
  }

  /**
   * Tags an account, or removes the tag.
   *
   * @param companyId company
   * @param ref renewal
   * @param option BLANKET or UNIT_HEAD, null to remove the tag
   */
  @Transactional
  public void tag(Long companyId, String ref, String option) {
    RenewalCandidate c = records.get(companyId, ref);
    RenewalRecords.requireUnlocked(c);
    if (option == null || option.isBlank()) {
      c.getPlacement().getTags().directToInsurer(null, null);
      audit.record(
          RenewalCodes.ENTITY, ref, AuditAction.UPDATE, "Direct-to-Insurer Payment removed");
      return;
    }
    if (!CandidateTags.BLANKET.equals(option) && !CandidateTags.UNIT_HEAD.equals(option)) {
      throw new BusinessRuleException(
          "RNW_DTI_OPTION", "Select With Blanket Approval or Route for UH Approval");
    }
    boolean blanket = CandidateTags.BLANKET.equals(option);
    c.getPlacement()
        .getTags()
        .directToInsurer(option, blanket ? CandidateTags.APPROVED : CandidateTags.PENDING);
    audit.record(
        RenewalCodes.ENTITY,
        ref,
        AuditAction.UPDATE,
        "Direct-to-Insurer Payment "
            + (blanket ? "with blanket approval" : "routed to the Unit Head"));
    if (!blanket) {
      notices.users(
          Collections.singletonList(unitHead(c)),
          RenewalCodes.EVENT_ASSIGNED,
          c,
          new RenewalNotices.Text(
              ref + ": Direct-to-Insurer Payment to approve", c.getSnapshot().clientName()));
    }
  }

  /**
   * The Unit Head's decision.
   *
   * @param companyId company
   * @param ref renewal
   * @param approve approve or reject
   * @param reason rejection reason
   */
  @Transactional
  public void decide(Long companyId, String ref, boolean approve, String reason) {
    RenewalCandidate c = records.get(companyId, ref);
    if (!CandidateTags.PENDING.equals(c.getPlacement().getTags().getDtiStatus())) {
      throw new BusinessRuleException(
          "RNW_DTI_STATUS", "No Direct-to-Insurer Payment awaits the Unit Head approval");
    }
    String why = approve ? reason : RemarkService.requireText(reason, "Enter the rejection reason");
    c.getPlacement()
        .getTags()
        .dtiDecision(
            approve ? CandidateTags.APPROVED : CandidateTags.REJECTED, currentUser.username(), why);
    audit.record(
        RenewalCodes.ENTITY,
        ref,
        approve ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        "Direct-to-Insurer Payment " + (approve ? "approved" : "rejected: " + why));
    notices.users(
        Collections.singletonList(c.getAssignedAo()),
        RenewalCodes.EVENT_RETURNED,
        c,
        new RenewalNotices.Text(
            ref + ": Direct-to-Insurer Payment " + (approve ? "approved" : "rejected"),
            why == null ? "" : why));
  }

  /**
   * Refuses to proceed while the tag awaits the Unit Head or was rejected.
   *
   * @param c renewal
   */
  public void requireCleared(RenewalCandidate c) {
    String status = c.getPlacement().getTags().getDtiStatus();
    if (CandidateTags.PENDING.equals(status)) {
      throw new BusinessRuleException(
          "RNW_DTI_PENDING", "The Direct-to-Insurer Payment awaits the Unit Head approval");
    }
    if (CandidateTags.REJECTED.equals(status)) {
      throw new BusinessRuleException(
          "RNW_DTI_REJECTED",
          "The Unit Head rejected the Direct-to-Insurer Payment: remove the tag to proceed");
    }
  }

  private static String unitHead(RenewalCandidate c) {
    return c.getSnapshot().sales() == null ? null : c.getSnapshot().sales().unitHead();
  }
}
