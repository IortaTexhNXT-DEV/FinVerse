package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.screening.cases.domain.CommitteeVoteRepository;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.common.service.ScreeningPermissions;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * My Approvals source of screening cases (SNSRP-702, 704; the case part of the design's {@code
 * ScreeningApprovalSource}): cases waiting for the unit head (SCR_CASE_APPROVE) assigned to the
 * viewer or in the approvers' queue, never to their own investigator, and cases waiting for the AML
 * Committee (SCR_COMMITTEE) on which the viewer has not voted in the current round. The system view
 * returns all of them.
 */
@Component
public class CaseApprovalSource implements PendingApprovalSource {

  private final ScreeningCaseRepository cases;
  private final CommitteeVoteRepository votes;

  /**
   * Creates the source.
   *
   * @param cases cases
   * @param votes committee votes
   */
  public CaseApprovalSource(ScreeningCaseRepository cases, CommitteeVoteRepository votes) {
    this.cases = cases;
    this.votes = votes;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    List<PendingApproval> items = new ArrayList<>();
    if (viewer.can(CaseCodes.CASE_APPROVE)) {
      waiting(CaseStage.UNIT_HEAD_APPROVAL).stream()
          .filter(c -> viewer.systemView() || mine(c, viewer.username()))
          .filter(c -> viewer.mayApproveItemOf(c.getInvestigator()))
          .map(c -> item(c, "Screening case for approval"))
          .forEach(items::add);
    }
    if (viewer.can(CaseCodes.COMMITTEE)) {
      waiting(CaseStage.AML_COMMITTEE).stream()
          .filter(
              c ->
                  viewer.systemView()
                      || !votes.existsByCaseIdAndRoundNoAndMemberIgnoreCase(
                          c.getId(), c.getCommitteeRound(), viewer.username()))
          .map(c -> item(c, "Screening case for the AML Committee"))
          .forEach(items::add);
    }
    return items;
  }

  private List<ScreeningCase> waiting(CaseStage stage) {
    Specification<ScreeningCase> spec =
        CaseSpecs.equal("status", CaseStatus.OPEN).and(CaseSpecs.equal("stage", stage));
    return cases.findAll(spec);
  }

  private static boolean mine(ScreeningCase c, String user) {
    return c.getAssignee() == null || CurrentUser.sameUser(c.getAssignee(), user);
  }

  private static PendingApproval item(ScreeningCase c, String type) {
    return new PendingApproval(
        ScreeningPermissions.MODULE,
        type,
        c.getCaseNo(),
        c.getClientName() + " - " + c.getCaseType() + " (" + c.getStage() + ")",
        null,
        null,
        c.getInvestigator() == null ? c.getCreatedBy() : c.getInvestigator(),
        c.getStageEnteredAt(),
        c.getCompanyId(),
        CaseCodes.link(c.getId()));
  }
}
