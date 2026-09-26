package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-assignment of a case or approval (SNSRP-404; FR-SS-043): in a review or approval stage, by a
 * holder of SCR_CASE_ASSIGN, to an eligible user (holder of the stage permission, not the client's
 * account officer, not the investigator for an approval) with a reason of {@code
 * SCR_REASSIGN_REASON} (a comment for OTHERS). The data, stage and history of the case do not
 * change; only the new assignee can act afterwards. Both users are informed.
 */
@Service
@Transactional
public class CaseAssignmentService {

  private static final String OTHERS = "OTHERS";

  private final ScreeningCaseRepository cases;
  private final CaseAccess access;
  private final CaseRouter router;
  private final RoleMembers members;
  private final CaseMover mover;
  private final CaseClientFacts clientFacts;
  private final CaseNotifier notifier;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param access case access
   * @param router eligibility
   * @param members permission holders
   * @param mover assignment
   * @param clientFacts sales teams
   * @param notifier notices
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the re-assignment
  public CaseAssignmentService(
      ScreeningCaseRepository cases,
      CaseAccess access,
      CaseRouter router,
      RoleMembers members,
      CaseMover mover,
      CaseClientFacts clientFacts,
      CaseNotifier notifier,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.cases = cases;
    this.access = access;
    this.router = router;
    this.members = members;
    this.mover = mover;
    this.clientFacts = clientFacts;
    this.notifier = notifier;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The users a case can be re-assigned to (the current assignee excluded).
   *
   * @param caseId the case
   * @return user names
   */
  @Transactional(readOnly = true)
  public List<String> eligible(Long caseId) {
    ScreeningCase c = CaseChecks.get(cases, caseId);
    String permission = CaseAccess.ownerOf(c.getStage());
    if (!c.getStage().isReassignable() || permission == null) {
      return List.of();
    }
    return members.withPermission(permission).stream()
        .filter(u -> router.eligible(c, u, permission))
        .filter(u -> !CurrentUser.sameUser(u, c.getAssignee()))
        .toList();
  }

  /**
   * Re-assigns a case.
   *
   * @param caseId the case
   * @param assignee the new assignee
   * @param reasonCode the reason
   * @param comment the comment (required for OTHERS)
   * @return the case
   */
  public ScreeningCase reassign(Long caseId, String assignee, String reasonCode, String comment) {
    ScreeningCase c = CaseChecks.get(cases, caseId);
    access.requirePermission(CaseCodes.CASE_ASSIGN);
    CaseStage stage = c.getStage();
    if (!stage.isReassignable()) {
      throw new BusinessRuleException(
          CaseAccess.NOT_ALLOWED, "The action re-assign is not allowed in stage " + stage);
    }
    requireReason(reasonCode, comment);
    requireEligible(c, assignee);
    String previous = c.getAssignee();
    String text = comment == null ? null : comment.strip();
    mover.assign(
        c, assignee, CaseEventType.REASSIGNED, new CaseMover.AssignFacts(reasonCode, text, text));
    if (stage.isInvestigation()) {
      c.team(clientFacts.teamOf(c.getCompanyId(), assignee).orElse(null));
    }
    notifier.user(previous, c, "SCR_CASE_ASSIGNED", "re-assigned to " + assignee);
    audit.record(
        CaseCodes.ENTITY,
        c.getCaseNo(),
        AuditAction.UPDATE,
        "Re-assigned "
            + (previous == null ? "" : "from " + previous + " ")
            + "to "
            + assignee
            + " ("
            + reasonCode
            + ")");
    return c;
  }

  private void requireReason(String reasonCode, String comment) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException(
          "SCR_REASSIGN_REASON_REQUIRED", "Select the reason for the re-assignment");
    }
    lovs.requireValid(CaseCodes.REASSIGN_LOV, reasonCode, LocalDate.now(clock));
    if (OTHERS.equals(reasonCode) && (comment == null || comment.isBlank())) {
      throw new BusinessRuleException(
          "SCR_REASSIGN_COMMENT_REQUIRED", "Enter a comment for reason Others");
    }
  }

  private void requireEligible(ScreeningCase c, String assignee) {
    if (assignee == null || assignee.isBlank()) {
      throw new BusinessRuleException("SCR_ASSIGNEE_REQUIRED", "Select the new assignee");
    }
    String permission = CaseAccess.ownerOf(c.getStage());
    if (!router.eligible(c, assignee, permission)
        || CurrentUser.sameUser(assignee, c.getAssignee())) {
      throw new BusinessRuleException(
          "SCR_ASSIGNEE_NOT_ELIGIBLE", assignee + " cannot take cases in stage " + c.getStage());
    }
  }
}
