package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Who may see and act on a screening case (SNSRP-401, 402, 404; FR-SS-001 R2, FR-SS-040, 041): edit
 * rights come only from the owner permission of the current stage and the assignment (an unassigned
 * case is open to every holder of the stage permission); Compliance, the UCC, approvers, committee
 * members and auditors see every case, investigators their own and their team's cases.
 */
@Component
public class CaseAccess {

  /** Refusal: action in the wrong stage. */
  public static final String NOT_ALLOWED = "SCR_ACTION_NOT_ALLOWED";

  private static final Map<CaseStage, String> OWNER = new EnumMap<>(CaseStage.class);

  private static final List<String> SEE_ALL =
      List.of(
          CaseCodes.CASE_ASSIGN,
          CaseCodes.CASE_APPROVE,
          CaseCodes.COMPLIANCE_REVIEW,
          CaseCodes.COMMITTEE,
          CaseCodes.STR_EXTRACT,
          CaseCodes.REPORT_VIEW,
          CaseCodes.AUDIT_VIEW);

  static {
    OWNER.put(CaseStage.INVESTIGATION, CaseCodes.INVESTIGATE);
    OWNER.put(CaseStage.RETURNED, CaseCodes.INVESTIGATE);
    OWNER.put(CaseStage.UNIT_HEAD_APPROVAL, CaseCodes.CASE_APPROVE);
    OWNER.put(CaseStage.COMPLIANCE_REVIEW, CaseCodes.COMPLIANCE_REVIEW);
    OWNER.put(CaseStage.AML_COMMITTEE, CaseCodes.COMMITTEE);
    OWNER.put(CaseStage.STR_PREPARATION, CaseCodes.COMPLIANCE_REVIEW);
    OWNER.put(CaseStage.STR_EXTRACTION, CaseCodes.STR_EXTRACT);
  }

  private final CurrentUser currentUser;

  /**
   * Creates the access rules.
   *
   * @param currentUser current user
   */
  public CaseAccess(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  /**
   * The owner permission of a stage.
   *
   * @param stage the stage
   * @return the permission, null for NEW and CLOSED
   */
  public static String ownerOf(CaseStage stage) {
    return OWNER.get(stage);
  }

  /**
   * Whether the current user sees every case (FR-SS-041 R1).
   *
   * @return true for Compliance, UCC, approvers, committee, auditors
   */
  public boolean seesAll() {
    return SEE_ALL.stream().anyMatch(currentUser::hasAuthority);
  }

  /**
   * Whether the current user may act on the case in its current stage: the stage permission and the
   * assignment (or an unassigned case).
   *
   * @param c the case
   * @return true when the user may act
   */
  public boolean isActor(ScreeningCase c) {
    String permission = ownerOf(c.getStage());
    return permission != null
        && currentUser.hasAuthority(permission)
        && (c.getAssignee() == null || CurrentUser.sameUser(c.getAssignee(), user()));
  }

  /**
   * Refuses an action unless the case is in one of the stages and the user may act on it.
   *
   * @param c the case
   * @param action the action label for the message
   * @param stages the stages the action is allowed in
   */
  public void requireActor(ScreeningCase c, String action, Set<CaseStage> stages) {
    if (!stages.contains(c.getStage())) {
      if (CurrentUser.sameUser(c.getInvestigator(), user())
          && !c.getStage().isInvestigation()
          && stages.stream().anyMatch(CaseStage::isInvestigation)) {
        throw noLongerYours(c);
      }
      throw new BusinessRuleException(
          NOT_ALLOWED, "The action " + action + " is not allowed in stage " + c.getStage());
    }
    requirePermission(ownerOf(c.getStage()));
    if (c.getAssignee() != null && !CurrentUser.sameUser(c.getAssignee(), user())) {
      throw new BusinessRuleException(
          "SCR_CASE_ASSIGNED_ELSEWHERE",
          "Case " + c.getCaseNo() + " is assigned to " + c.getAssignee());
    }
  }

  /**
   * Refuses a user without a permission (FR-SS-001: refused and logged by the platform).
   *
   * @param permission the permission
   */
  public void requirePermission(String permission) {
    if (permission == null || !currentUser.hasAuthority(permission)) {
      throw new AccessDeniedException("You are not permitted to perform this action");
    }
  }

  /**
   * Whether the current user holds a permission.
   *
   * @param permission permission
   * @return true when held
   */
  public boolean can(String permission) {
    return currentUser.hasAuthority(permission);
  }

  /**
   * The current user.
   *
   * @return user name
   */
  public String user() {
    return currentUser.username();
  }

  private static BusinessRuleException noLongerYours(ScreeningCase c) {
    return new BusinessRuleException(
        "SCR_CASE_NOT_YOURS", "Case " + c.getCaseNo() + " is no longer assigned to you");
  }
}
