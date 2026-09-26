package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The actions the current user may take on a case now (SNSRP-401; FR-SS-001 "buttons for actions
 * the user may not perform are hidden"): from the stage, its owner permission, the assignment and
 * the user's other permissions. The services check the same rules again.
 */
@Component
public class CaseActions {

  /** Edit the review. */
  public static final String REVIEW_EDIT = "REVIEW_EDIT";

  /** Upload a document. */
  public static final String UPLOAD = "UPLOAD";

  /** Confirm or clear a match of the case. */
  public static final String MATCH_DECIDE = "MATCH_DECIDE";

  /** Submit with a disposition. */
  public static final String SUBMIT = "SUBMIT";

  /** Resubmit a returned case. */
  public static final String RESUBMIT = "RESUBMIT";

  /** Update the client's risk tag. */
  public static final String RISK_TAG = "RISK_TAG";

  /** Unit head decision. */
  public static final String DECIDE = "DECIDE";

  /** Compliance outcome. */
  public static final String OUTCOME = "OUTCOME";

  /** Committee vote. */
  public static final String VOTE = "VOTE";

  /** Prepare and edit the STR, mark it ready. */
  public static final String STR_EDIT = "STR_EDIT";

  /** Record the AMLC filing. */
  public static final String FILING = "FILING";

  /** Re-open a closed case. */
  public static final String REOPEN = "REOPEN";

  /** Re-assign. */
  public static final String REASSIGN = "REASSIGN";

  private final CaseAccess access;
  private final CaseQueries queries;

  /**
   * Creates the rules.
   *
   * @param access case access
   * @param queries votes
   */
  public CaseActions(CaseAccess access, CaseQueries queries) {
    this.access = access;
    this.queries = queries;
  }

  /**
   * The actions of the current user.
   *
   * @param c the case
   * @return action codes
   */
  public List<String> of(ScreeningCase c) {
    List<String> actions = new ArrayList<>();
    boolean actor = c.isOpen() && access.isActor(c);
    CaseStage stage = c.getStage();
    if (actor) {
      actions.add(UPLOAD);
      actions.addAll(stageActions(c));
    }
    if (stage == CaseStage.AML_COMMITTEE && access.can(CaseCodes.COMMITTEE) && !queries.voted(c)) {
      actions.add(VOTE);
    }
    if (stage == CaseStage.CLOSED && access.can(CaseCodes.COMPLIANCE_REVIEW)) {
      actions.add(REOPEN);
    }
    if (stage.isReassignable() && access.can(CaseCodes.CASE_ASSIGN)) {
      actions.add(REASSIGN);
    }
    return actions;
  }

  private List<String> stageActions(ScreeningCase c) {
    return switch (c.getStage()) {
      case INVESTIGATION, RETURNED -> investigation(c);
      case UNIT_HEAD_APPROVAL ->
          CurrentUser.sameUser(c.getInvestigator(), access.user()) ? List.of() : List.of(DECIDE);
      case COMPLIANCE_REVIEW -> List.of(OUTCOME);
      case STR_PREPARATION -> List.of(STR_EDIT);
      case STR_EXTRACTION -> List.of(FILING);
      default -> List.of();
    };
  }

  private List<String> investigation(ScreeningCase c) {
    List<String> actions = new ArrayList<>(List.of(REVIEW_EDIT, MATCH_DECIDE));
    actions.add(c.getStage() == CaseStage.RETURNED ? RESUBMIT : SUBMIT);
    if (access.can(CaseCodes.RISK_TAG)) {
      actions.add(RISK_TAG);
    }
    return actions;
  }
}
