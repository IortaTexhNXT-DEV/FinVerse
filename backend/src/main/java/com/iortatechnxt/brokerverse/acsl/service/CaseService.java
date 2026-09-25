package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCase;
import com.iortatechnxt.brokerverse.acsl.domain.AcslCaseRepository;
import com.iortatechnxt.brokerverse.acsl.domain.CaseOutcome;
import com.iortatechnxt.brokerverse.acsl.domain.CaseStage;
import com.iortatechnxt.brokerverse.acsl.domain.CaseSubject;
import com.iortatechnxt.brokerverse.acsl.domain.CaseType;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.RefundValidationCompleted;
import com.iortatechnxt.brokerverse.opsledger.service.port.RefundValidationSource;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ACSL cases (ACSL 2.5.0-2.5.5; ACCOUNTING_DISBURSEMENT_DESIGN 7.3): opened by a processor from an
 * invoice or by another module (the refund validations of payrequest), assigned by the team leader,
 * investigated with findings, and closed by a result given to the requester, which answers a refund
 * validation with {@code RefundValidationCompleted} (MKT 1.11.0).
 */
@Service
@Transactional
public class CaseService {

  private final AcslCaseRepository cases;
  private final InvoiceLedgerQueryService ledger;
  private final WorkflowService workflow;
  private final WorkflowViewService views;
  private final WorkAssignmentService assignments;
  private final UserDirectory users;
  private final DocumentNumberService numbers;
  private final ApplicationEventPublisher events;
  private final AcslNotifier notifier;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param ledger Operations invoice ledger
   * @param workflow workflow engine
   * @param views work cases
   * @param assignments work assignment
   * @param users user directory
   * @param numbers case numbers
   * @param events event publisher (validation results)
   * @param notifier notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CaseService(
      AcslCaseRepository cases,
      InvoiceLedgerQueryService ledger,
      WorkflowService workflow,
      WorkflowViewService views,
      WorkAssignmentService assignments,
      UserDirectory users,
      DocumentNumberService numbers,
      ApplicationEventPublisher events,
      AcslNotifier notifier,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.cases = cases;
    this.ledger = ledger;
    this.workflow = workflow;
    this.views = views;
    this.assignments = assignments;
    this.users = users;
    this.numbers = numbers;
    this.events = events;
    this.notifier = notifier;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Opens a case in ACSL (ACSL 2.5.0 investigation, 2.5.5 analysis request, 2.6.x application or
   * reversal), usually from the invoice being investigated.
   *
   * @param companyId company
   * @param draft type, invoice, AR, amount, subject and details
   * @return the case
   */
  public AcslCase open(Long companyId, CaseDraft draft) {
    if (draft.type() == null || Acsl.blankToNull(draft.subject()) == null) {
      throw new BusinessRuleException("ACSL_CASE_INCOMPLETE", "Give the case type and subject");
    }
    return open(
        companyId,
        draft,
        new AcslCase.Requester(
            null, null, currentUser.username(), Acsl.blankToNull(draft.details())));
  }

  /**
   * Opens the account analysis case of a refund validation (ACSL 2.5.5, MKT 1.11.0), idempotent on
   * the requester's reference.
   *
   * @param request what payrequest asks to validate
   * @return the case
   */
  public AcslCase openAnalysis(RefundValidationSource.ValidationRequest request) {
    RefundValidationSource.Source source = request.source();
    return cases
        .findByRequesterModuleAndRequesterRef(source.module(), source.reference())
        .orElseGet(
            () ->
                open(
                    request.companyId(),
                    new CaseDraft(
                        CaseType.ANALYSIS_REQUEST,
                        request.invoiceNo(),
                        request.arNo(),
                        request.amount(),
                        "Refund validation of AR "
                            + request.arNo()
                            + " ("
                            + source.module()
                            + " "
                            + source.reference()
                            + ")",
                        source.remarks()),
                    new AcslCase.Requester(
                        source.module(),
                        source.reference(),
                        source.requestedBy(),
                        source.remarks())));
  }

  private AcslCase open(Long companyId, CaseDraft draft, AcslCase.Requester requester) {
    String number = numbers.next("ACS-" + LocalDate.now(clock).getYear());
    AcslCase saved =
        cases.save(
            new AcslCase(
                companyId,
                number,
                draft.type(),
                subjectOf(draft),
                draft.subject().strip(),
                requester));
    workflow.start(
        new StartCase(
            companyId,
            Acsl.CASE_WORKFLOW,
            new CaseRecord(
                Acsl.CASE_ENTITY,
                String.valueOf(saved.getId()),
                saved.getCaseNo(),
                saved.getSubject(),
                Acsl.caseLink(saved.getId()),
                requester.module() == null ? Acsl.MODULE : requester.module()),
            null));
    audit.record(
        Acsl.CASE_ENTITY, number, AuditAction.CREATE, draft.type() + ": " + draft.subject());
    notifier.team(
        "ACSL_ASSIGN",
        number + " received",
        saved.getSubject(),
        Acsl.caseLink(saved.getId()),
        Acsl.CASE_ENTITY,
        saved.getId());
    return saved;
  }

  /**
   * Assigns or re-assigns a case to a processor (team leader).
   *
   * @param id case
   * @param username processor holding ACSL_PROCESS
   * @param comment comment
   * @return the case
   */
  public AcslCase assign(Long id, String username, String comment) {
    AcslCase c = get(id);
    if (c.getStage() == CaseStage.RECEIVED) {
      workflow.transition(Acsl.CASE_ENTITY, key(c), "assign", Acsl.note(comment));
    } else if (c.getStage() != CaseStage.ASSIGNED && c.getStage() != CaseStage.INVESTIGATING) {
      throw new BusinessRuleException(Acsl.WRONG_STAGE, c.getCaseNo() + " is " + c.getStage());
    }
    Long caseId =
        views
            .view(Acsl.CASE_ENTITY, key(c))
            .map(v -> v.workCase().getId())
            .orElseThrow(() -> new ResourceNotFoundException(Acsl.CASE_ENTITY, id));
    assignments.assign(caseId, username, users.usersWithPermission("ACSL_PROCESS"));
    audit.record(Acsl.CASE_ENTITY, c.getCaseNo(), AuditAction.UPDATE, "Assigned to " + username);
    return c;
  }

  /**
   * Records the findings of the investigation (ACSL 2.5.0).
   *
   * @param id case
   * @param findings findings
   * @return the case
   */
  public AcslCase recordFindings(Long id, String findings) {
    AcslCase c = get(id);
    Acsl.requireStage(c.getCaseNo(), c.getStage(), CaseStage.INVESTIGATING);
    c.recordFindings(Acsl.blankToNull(findings));
    audit.record(Acsl.CASE_ENTITY, c.getCaseNo(), AuditAction.UPDATE, "Findings recorded");
    return c;
  }

  /**
   * Gives the result to the requester (ACSL 2.5.4): a refund validation is answered with {@code
   * RefundValidationCompleted} (confirmed unless rejected).
   *
   * @param id case
   * @param outcome CONFIRMED, REJECTED or NO_ACTION
   * @param remarks remarks
   * @return the case
   */
  public AcslCase provideResult(Long id, CaseOutcome outcome, String remarks) {
    AcslCase c = get(id);
    Acsl.requireStage(c.getCaseNo(), c.getStage(), CaseStage.INVESTIGATING);
    if (outcome == null || outcome == CaseOutcome.CORRECTION) {
      throw new BusinessRuleException(
          "ACSL_OUTCOME_INVALID", "Give the result: confirmed, rejected or no action");
    }
    workflow.transition(Acsl.CASE_ENTITY, key(c), "provide_result", Acsl.note(remarks));
    close(c, outcome, Acsl.blankToNull(remarks));
    return c;
  }

  /**
   * Closes a case whose investigation raised a correction entry (ACSL 2.9.0).
   *
   * @param c case
   * @param correctionId correction raised
   * @param correctionNo its number
   */
  void correctionRaised(AcslCase c, Long correctionId, String correctionNo) {
    Acsl.requireStage(c.getCaseNo(), c.getStage(), CaseStage.INVESTIGATING);
    workflow.transition(
        Acsl.CASE_ENTITY, key(c), "raise_correction", Acsl.note("Correction " + correctionNo));
    c.linkCorrection(correctionId);
    close(c, CaseOutcome.CORRECTION, "Correction entry " + correctionNo + " raised");
  }

  private void close(AcslCase c, CaseOutcome outcome, String remarks) {
    c.recordResult(outcome, remarks, currentUser.username(), clock.instant());
    if (c.getRequesterModule() != null) {
      events.publishEvent(
          new RefundValidationCompleted(
              c.getCompanyId(),
              RefundValidationSource.ACSL,
              c.getRequesterModule(),
              c.getRequesterRef(),
              outcome == CaseOutcome.CONFIRMED || outcome == CaseOutcome.NO_ACTION,
              null,
              c.getCaseNo() + ": " + (remarks == null ? outcome.name() : remarks)));
    }
    notifier.user(
        c.getRequestedBy(),
        c.getCaseNo() + " result: " + outcome,
        c.getSubject() + (remarks == null ? "" : " - " + remarks),
        Acsl.caseLink(c.getId()),
        Acsl.CASE_ENTITY,
        c.getId());
    audit.record(Acsl.CASE_ENTITY, c.getCaseNo(), AuditAction.UPDATE, "Result " + outcome);
  }

  /**
   * A case.
   *
   * @param id id
   * @return case
   */
  @Transactional(readOnly = true)
  public AcslCase get(Long id) {
    return cases
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(Acsl.CASE_ENTITY, id));
  }

  private CaseSubject subjectOf(CaseDraft draft) {
    String invoiceNo = Acsl.blankToNull(draft.invoiceNo());
    if (invoiceNo == null) {
      return new CaseSubject(
          null, null, null, null, Acsl.blankToNull(draft.arNo()), null, draft.amount());
    }
    OpsInvoice invoice =
        ledger
            .find(invoiceNo)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "ACSL_INVOICE_UNKNOWN", "Invoice " + invoiceNo + " is not in the ledger"));
    return new CaseSubject(
        invoice.getInvoiceNo(),
        invoice.getRootInvoiceNo(),
        invoice.getInsurerCode(),
        invoice.getClientCode(),
        Acsl.blankToNull(draft.arNo()),
        invoice.getCurrency(),
        draft.amount());
  }

  private static String key(AcslCase c) {
    return String.valueOf(c.getId());
  }

  /**
   * A case as entered.
   *
   * @param type type
   * @param invoiceNo invoice, may be blank
   * @param arNo AR of the payment, may be blank
   * @param amount amount concerned, may be null
   * @param subject one-line subject
   * @param details what is asked
   */
  public record CaseDraft(
      CaseType type,
      String invoiceNo,
      String arNo,
      BigDecimal amount,
      String subject,
      String details) {}
}
