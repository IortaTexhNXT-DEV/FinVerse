package com.iortatechnxt.brokerverse.screening;

import static com.iortatechnxt.brokerverse.screening.ScreeningCaseFixtures.INVESTIGATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientRiskService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseApprovalService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseApprovalSource;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseDocumentService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseReviewService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSubmissionService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseTimeline;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseValidator.Decision;
import com.iortatechnxt.brokerverse.screening.cases.service.CommitteeService;
import com.iortatechnxt.brokerverse.screening.str.domain.StrExtraction;
import com.iortatechnxt.brokerverse.screening.str.domain.StrStatus;
import com.iortatechnxt.brokerverse.screening.str.domain.StrTransaction.Line;
import com.iortatechnxt.brokerverse.screening.str.domain.SuspiciousTransactionReport;
import com.iortatechnxt.brokerverse.screening.str.service.StrExtractionService;
import com.iortatechnxt.brokerverse.screening.str.service.StrFilingService;
import com.iortatechnxt.brokerverse.screening.str.service.StrService;
import com.iortatechnxt.brokerverse.screening.str.service.StrService.StrEdit;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A screening case end to end (SNSRP-303, 401, 501-502, 601, 701-706; exit criterion of wave S1-C):
 * a listed client's registration opens the case, the investigator completes the review, uploads the
 * KYC form and submits (validated and routed), the unit head concurs, Compliance returns it for
 * rework and escalates it to the AML Committee after the resubmission, the committee approves an
 * STR by majority, the STR is prepared, completed and marked ready, extracted with the STR layout
 * and filed with its AMLC reference, which closes the case.
 */
@IntegrationTest
class ScreeningCasesIT {

  private static final String APPROVER = "scrapprover";
  private static final String COMPLIANCE = "compoff";
  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  @Autowired private ScreeningCaseFixtures fx;
  @Autowired private CaseReviewService reviews;
  @Autowired private CaseDocumentService documents;
  @Autowired private CaseSubmissionService submissions;
  @Autowired private CaseApprovalService approvals;
  @Autowired private CommitteeService committee;
  @Autowired private CaseTimeline timeline;
  @Autowired private CaseApprovalSource inbox;
  @Autowired private StrService strs;
  @Autowired private StrExtractionService extractions;
  @Autowired private StrFilingService filings;
  @Autowired private ClientRiskService clientRisk;

  private List<CaseEventType> events(ScreeningCase c) {
    return timeline.of(c.getId()).stream().map(CaseEvent::getEvent).toList();
  }

  @Test
  void aCaseRunsFromInvestigationToTheFiledStr() {
    Client client = fx.listedClient();
    ScreeningCase opened = fx.caseOf(client);
    assertThat(opened.getCaseNo()).matches("SCR-\\d{4}-\\d{6}");
    assertThat(opened.getStage()).isEqualTo(CaseStage.INVESTIGATION);
    assertThat(opened.getTriggerCode()).isEqualTo("CLIENT_REGISTERED");
    assertThat(opened.getApprovalVersionId()).isNotNull();
    assertThat(opened.getSlaVersionId()).isNotNull();
    assertThat(opened.getDueAt()).isNotNull();
    assertThat(clientRisk.activeTags(client.getId())).contains("WATCHLIST_REVIEW");
    ScreeningCase c = fx.toInvestigator(opened);
    assertThat(events(c)).contains(CaseEventType.CREATED, CaseEventType.MATCH_ADDED);

    // Review: a value of the wrong type is flagged; the complete review is saved.
    assertThatThrownBy(
            () ->
                fx.as(
                    INVESTIGATOR,
                    () -> reviews.save(fx.reload(c), Map.of("PROPOSED_RATING", "EXTREME"))))
        .isInstanceOf(FieldValidationException.class);
    fx.completeReview(c);
    Decision decision = new Decision("TRUE_MATCH_REVIEW", "Same person; escalate", true);

    // Validation: the KYC form is required (logged even though the submission is refused).
    assertThatThrownBy(() -> fx.as(INVESTIGATOR, () -> submissions.submit(c.getId(), decision)))
        .hasMessageContaining("before submitting");
    assertThat(events(c)).contains(CaseEventType.VALIDATION_FAILED);
    fx.uploadKycForm(c);
    assertThat(documents.of(c.getId()).get(0).getNominatedName())
        .startsWith("KYC-REVIEW_")
        .contains("_KYC-FORM_1");

    CaseSubmissionService.Submitted submitted =
        fx.as(INVESTIGATOR, () -> submissions.submit(c.getId(), decision));
    assertThat(submitted.screeningCase().getStage()).isEqualTo(CaseStage.UNIT_HEAD_APPROVAL);
    assertThat(events(c)).contains(CaseEventType.VALIDATED, CaseEventType.SUBMITTED);
    assertThatThrownBy(() -> fx.as(INVESTIGATOR, () -> reviews.save(fx.reload(c), Map.of())))
        .hasMessageContaining("is no longer assigned to you");
    assertThat(
            inbox.pendingFor(ApprovalViewer.user(APPROVER, Set.of("SCR_CASE_APPROVE"))).stream()
                .anyMatch(p -> p.reference().equals(c.getCaseNo())))
        .isTrue();

    // Unit head concurs: routed to Compliance.
    ScreeningCase approved =
        fx.as(
            APPROVER,
            () ->
                approvals.unitHead(c.getId(), new CaseApprovalService.Step("CONCUR", null, "OK")));
    assertThat(approved.getStage()).isEqualTo(CaseStage.COMPLIANCE_REVIEW);

    // Compliance returns it for rework; the investigator resubmits to Compliance.
    assertThatThrownBy(
            () ->
                fx.as(
                    COMPLIANCE,
                    () ->
                        approvals.compliance(
                            c.getId(),
                            new CaseApprovalService.Step("RETURN", "INCOMPLETE_DETAILS", " "))))
        .hasMessage("Enter the items to correct");
    ScreeningCase returned =
        fx.as(
                COMPLIANCE,
                () ->
                    approvals.compliance(
                        c.getId(),
                        new CaseApprovalService.Step(
                            "RETURN", "INCOMPLETE_DETAILS", "Add the source of funds")))
            .screeningCase();
    assertThat(returned.getStage()).isEqualTo(CaseStage.RETURNED);
    assertThat(returned.getAssignee()).isEqualTo(INVESTIGATOR);
    assertThat(returned.getRoundNo()).isEqualTo(2);
    ScreeningCase resubmitted =
        fx.as(
                INVESTIGATOR,
                () ->
                    submissions.resubmit(
                        c.getId(), "Source of funds added", new Decision(null, null, true)))
            .screeningCase();
    assertThat(resubmitted.getStage()).isEqualTo(CaseStage.COMPLIANCE_REVIEW);
    assertThat(resubmitted.getAssignee()).isEqualTo(COMPLIANCE);

    // Escalation to the committee; MAJORITY of 5: the third APPROVE_STR vote finalises.
    ScreeningCase escalated =
        fx.as(
                COMPLIANCE,
                () ->
                    approvals.compliance(
                        c.getId(),
                        new CaseApprovalService.Step("ESCALATE_COMMITTEE", null, "For decision")))
            .screeningCase();
    assertThat(escalated.getStage()).isEqualTo(CaseStage.AML_COMMITTEE);
    vote("amlcom1", c);
    vote("amlcom2", c);
    assertThat(fx.reload(c).getStage()).isEqualTo(CaseStage.AML_COMMITTEE);
    assertThatThrownBy(() -> vote("amlcom1", c))
        .hasMessage("You have already recorded your decision on this case");
    vote("amlcom3", c);
    ScreeningCase decided = fx.reload(c);
    assertThat(decided.getStage()).isEqualTo(CaseStage.STR_PREPARATION);
    assertThat(decided.getCommitteeDecision()).isEqualTo("APPROVE_STR");

    // STR: prefilled, gaps listed, completed and marked ready (APPROVED by the committee).
    SuspiciousTransactionReport str = fx.as(COMPLIANCE, () -> strs.prepare(c.getId()));
    assertThat(str.getStatus()).isEqualTo(StrStatus.DRAFT);
    assertThat(str.getSubjectSnapshot()).contains(client.getDisplayName());
    assertThat(strs.values(str.getId())).containsEntry("SUBJECT_NAME", client.getDisplayName());
    assertThat(strs.gaps(str.getId())).containsKeys(StrService.TRANSACTIONS, StrService.REASONS);
    Line zero =
        new Line("OR-1", LocalDate.now(), BigDecimal.ZERO.setScale(2), "PHP", "RECEIPT", null);
    assertThatThrownBy(
            () ->
                fx.as(
                    COMPLIANCE,
                    () ->
                        strs.save(
                            str.getId(), new StrEdit(Map.of(), Set.of("DEMO01"), List.of(zero)))))
        .hasMessage("The amount must be greater than 0");
    Line receipt =
        new Line(
            "OR-" + c.getId(),
            LocalDate.now(),
            new BigDecimal("50000.00"),
            "PHP",
            "RECEIPT",
            "Cash");
    fx.as(
        COMPLIANCE,
        () ->
            strs.save(
                str.getId(),
                new StrEdit(
                    Map.of("NARRATIVE", "Cash premium from a listed person"),
                    Set.of("DEMO01"),
                    List.of(receipt))));
    assertThat(strs.gaps(str.getId())).isEmpty();
    SuspiciousTransactionReport ready = fx.as(COMPLIANCE, () -> strs.markReady(str.getId()));
    assertThat(ready.getStatus()).isEqualTo(StrStatus.APPROVED);
    assertThat(fx.reload(c).getStage()).isEqualTo(CaseStage.STR_EXTRACTION);

    // Extraction of the committee-approved STRs of today, then the AMLC filing closes the case.
    LocalDate today = LocalDate.now(MANILA);
    StrExtraction extraction =
        fx.as(
            COMPLIANCE, () -> extractions.extract(fx.reload(c).getCompanyId(), today, today, null));
    assertThat(extraction.getStrIds()).contains(str.getId());
    assertThat(strs.get(str.getId()).getStatus()).isEqualTo(StrStatus.EXTRACTED);
    RunFile file = fx.as(COMPLIANCE, () -> extractions.file(extraction.getId()));
    assertThat(new String(file.content(), StandardCharsets.UTF_8)).contains(str.getStrNo());
    assertThatThrownBy(
            () ->
                fx.as(
                    COMPLIANCE,
                    () ->
                        extractions.extract(
                            fx.reload(c).getCompanyId(), today, today.minusDays(1), null)))
        .hasMessage("The end date must be on or after the start date");
    String reference = "AMLC-" + c.getCaseNo();
    assertThatThrownBy(
            () -> fx.as(COMPLIANCE, () -> filings.file(str.getId(), reference, today.minusDays(2))))
        .hasMessage("The filing date cannot be before the extraction date");
    SuspiciousTransactionReport filed =
        fx.as(COMPLIANCE, () -> filings.file(str.getId(), reference, today));
    assertThat(filed.getStatus()).isEqualTo(StrStatus.FILED);
    ScreeningCase closed = fx.reload(c);
    assertThat(closed.getStage()).isEqualTo(CaseStage.CLOSED);
    assertThat(closed.getStatus()).isEqualTo(CaseStatus.CLOSED);
    assertThat(events(c))
        .contains(
            CaseEventType.RETURNED,
            CaseEventType.RESUBMITTED,
            CaseEventType.ESCALATED,
            CaseEventType.COMMITTEE_DECISION,
            CaseEventType.STR_READY,
            CaseEventType.STR_EXTRACTED,
            CaseEventType.FILED);
  }

  private void vote(String member, ScreeningCase c) {
    fx.as(member, () -> committee.vote(c.getId(), "APPROVE_STR", "Supports the STR"));
  }
}
