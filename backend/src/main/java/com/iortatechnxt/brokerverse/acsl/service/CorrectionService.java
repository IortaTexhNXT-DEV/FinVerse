package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCase;
import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLine;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLineValues;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionRepository;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionStage;
import com.iortatechnxt.brokerverse.acsl.domain.LineOrigin;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionJournals.OriginalLine;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preparation of correction entries (ACSL 2.7.0-2.10.0, 2.9.1): raised from a case or by the team
 * leader, assigned to a preparer, prepared (a wrong-account proposal reverses the original line and
 * re-posts it to the right account; other lines are entered), checked (balanced, postable accounts,
 * known invoices and components), submitted and endorsed by a reviewer who is not the preparer.
 * Approval and posting are in {@link CorrectionPosting}.
 */
@Service
@Transactional
public class CorrectionService {

  private static final String KIND_LOV = "ACSL_CORRECTION_KIND";
  private static final int MIN_LINES = 2;
  private static final int MAX_LINES = 200;
  private static final String DEFAULT_CURRENCY = "PHP";

  private final CorrectionRepository corrections;
  private final CaseService cases;
  private final CorrectionJournals journals;
  private final CorrectionLineRules lineRules;
  private final InvoiceLedgerQueryService ledger;
  private final LovService lovs;
  private final WorkflowService workflow;
  private final WorkflowViewService views;
  private final WorkAssignmentService assignments;
  private final UserDirectory users;
  private final DocumentNumberService numbers;
  private final AcslNotifier notifier;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param corrections corrections
   * @param cases cases
   * @param journals journals and accounts
   * @param lineRules line checks
   * @param ledger Operations invoice ledger
   * @param lovs lists of values
   * @param workflow workflow engine
   * @param views work cases
   * @param assignments work assignment
   * @param users user directory
   * @param numbers correction numbers
   * @param notifier notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CorrectionService(
      CorrectionRepository corrections,
      CaseService cases,
      CorrectionJournals journals,
      CorrectionLineRules lineRules,
      InvoiceLedgerQueryService ledger,
      LovService lovs,
      WorkflowService workflow,
      WorkflowViewService views,
      WorkAssignmentService assignments,
      UserDirectory users,
      DocumentNumberService numbers,
      AcslNotifier notifier,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.corrections = corrections;
    this.cases = cases;
    this.journals = journals;
    this.lineRules = lineRules;
    this.ledger = ledger;
    this.lovs = lovs;
    this.workflow = workflow;
    this.views = views;
    this.assignments = assignments;
    this.users = users;
    this.numbers = numbers;
    this.notifier = notifier;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Raises a correction entry (team leader), waiting for its preparer's assignment.
   *
   * @param companyId company
   * @param draft kind, invoice, original journal and description
   * @return the correction
   */
  public Correction create(Long companyId, Draft draft) {
    return open(companyId, draft, null);
  }

  /**
   * Raises a correction entry from an investigation (ACSL 2.9.0); the case closes with it.
   *
   * @param caseId case being investigated
   * @param draft kind, original journal and description (the invoice is the case's)
   * @return the correction
   */
  public Correction raiseFromCase(Long caseId, Draft draft) {
    AcslCase c = cases.get(caseId);
    String invoiceNo =
        draft.invoiceNo() == null ? c.accountOrNone().invoiceNo() : draft.invoiceNo();
    Correction correction =
        open(
            c.getCompanyId(),
            new Draft(draft.kind(), invoiceNo, draft.originalBatchNo(), draft.description()),
            caseId);
    cases.correctionRaised(c, correction.getId(), correction.getCorrectionNo());
    return correction;
  }

  private Correction open(Long companyId, Draft draft, Long caseId) {
    lovs.requireValid(KIND_LOV, draft.kind(), LocalDate.now(clock));
    if (Acsl.blankToNull(draft.description()) == null) {
      throw new BusinessRuleException("ACSL_DESCRIPTION_REQUIRED", "Describe the correction");
    }
    Optional<OpsInvoice> invoice =
        Optional.ofNullable(Acsl.blankToNull(draft.invoiceNo()))
            .map(
                no ->
                    ledger
                        .find(no)
                        .orElseThrow(
                            () ->
                                new BusinessRuleException(
                                    "ACSL_INVOICE_UNKNOWN",
                                    "Invoice " + no + " is not in the ledger")));
    String batchNo = Acsl.blankToNull(draft.originalBatchNo());
    Optional<JournalBatch> original =
        batchNo == null ? Optional.empty() : Optional.of(journals.batch(companyId, batchNo));
    Long branchId =
        original
            .map(JournalBatch::getBranchId)
            .or(() -> invoice.map(OpsInvoice::getBranchId))
            .orElseGet(() -> lineRules.headOffice(companyId));
    String currency =
        original
            .map(JournalBatch::getCurrency)
            .or(() -> invoice.map(OpsInvoice::getCurrency))
            .orElse(DEFAULT_CURRENCY);
    String number = numbers.next("COR-" + LocalDate.now(clock).getYear());
    Correction saved =
        corrections.save(
            new Correction(
                companyId,
                branchId,
                number,
                new Correction.Header(
                    draft.kind(),
                    invoice.map(OpsInvoice::getInvoiceNo).orElse(null),
                    invoice.map(OpsInvoice::getRootInvoiceNo).orElse(null),
                    batchNo,
                    currency,
                    draft.description().strip()),
                caseId));
    workflow.start(
        new StartCase(
            companyId,
            Acsl.CORRECTION_WORKFLOW,
            new CaseRecord(
                Acsl.CORRECTION_ENTITY,
                String.valueOf(saved.getId()),
                number,
                saved.getDescription(),
                Acsl.correctionLink(saved.getId()),
                Acsl.MODULE),
            null));
    audit.record(Acsl.CORRECTION_ENTITY, number, AuditAction.CREATE, saved.getDescription());
    return saved;
  }

  /**
   * Assigns or re-assigns the preparation of a correction (ACSL 2.7.0, 2.8.0).
   *
   * @param id correction
   * @param username preparer holding ACSL_PROCESS
   * @param comment comment
   * @return the correction
   */
  public Correction assign(Long id, String username, String comment) {
    Correction c = get(id);
    if (c.getStage() == CorrectionStage.ASSIGNED) {
      workflow.transition(Acsl.CORRECTION_ENTITY, key(c), "assign", Acsl.note(comment));
    } else {
      Acsl.requireStage(c.getCorrectionNo(), c.getStage(), CorrectionStage.DRAFT);
    }
    Long caseId =
        views
            .view(Acsl.CORRECTION_ENTITY, key(c))
            .map(v -> v.workCase().getId())
            .orElseThrow(() -> new ResourceNotFoundException(Acsl.CORRECTION_ENTITY, id));
    assignments.assign(caseId, username, users.usersWithPermission("ACSL_PROCESS"));
    audit.record(
        Acsl.CORRECTION_ENTITY, c.getCorrectionNo(), AuditAction.UPDATE, "Assigned to " + username);
    return c;
  }

  /**
   * Proposes the correction of a posting to a wrong account (ACSL 2.9.1): the original line is
   * reversed and re-posted to the right account (and party), both linked to the original journal.
   *
   * @param id correction
   * @param proposal original journal line and the right account
   * @return the correction
   */
  public Correction proposeWrongAccount(Long id, Proposal proposal) {
    Correction c = editable(id);
    OriginalLine original = journals.line(c.getCompanyId(), proposal.batchNo(), proposal.lineNo());
    journals.account(c.getCompanyId(), proposal.targetAccountCode());
    String party =
        proposal.targetPartyCode() == null ? original.partyCode() : proposal.targetPartyCode();
    List<CorrectionLineValues> lines = new ArrayList<>();
    c.getLines().forEach(l -> lines.add(l.values()));
    lines.add(
        new CorrectionLineValues(
            original.accountCode(),
            original.side().opposite(),
            original.amount(),
            original.partyCode(),
            c.getInvoiceNo(),
            proposal.component(),
            original.costCenter(),
            original.businessLine(),
            "Reversal of " + original.batchNo() + " line " + original.lineNo(),
            LineOrigin.REVERSAL,
            original.batchNo(),
            original.lineNo()));
    lines.add(
        new CorrectionLineValues(
            proposal.targetAccountCode(),
            original.side(),
            original.amount(),
            party,
            c.getInvoiceNo(),
            proposal.targetComponentOrSame(),
            original.costCenter(),
            original.businessLine(),
            "Re-post of " + original.batchNo() + " line " + original.lineNo(),
            LineOrigin.REPOST,
            original.batchNo(),
            original.lineNo()));
    return replace(c, lines, "Wrong-account proposal on " + original.batchNo());
  }

  /**
   * Replaces the lines of a draft correction (ACSL 2.9.0).
   *
   * @param id correction
   * @param lines lines
   * @return the correction
   */
  public Correction saveLines(Long id, List<CorrectionLineValues> lines) {
    return replace(editable(id), lines, lines.size() + " line(s) saved");
  }

  private Correction replace(Correction c, List<CorrectionLineValues> lines, String summary) {
    if (lines.size() > MAX_LINES) {
      throw new BusinessRuleException(
          "ACSL_TOO_MANY_LINES", "A correction has at most " + MAX_LINES + " lines");
    }
    List<CorrectionLineValues> checked = new ArrayList<>();
    for (CorrectionLineValues v : lines) {
      checked.add(lineRules.check(c, v));
    }
    c.replaceLines(checked);
    audit.record(Acsl.CORRECTION_ENTITY, c.getCorrectionNo(), AuditAction.UPDATE, summary);
    return c;
  }

  /**
   * Submits a balanced correction for review (ACSL 2.9.0).
   *
   * @param id correction
   * @param comment comment
   * @return the correction
   */
  public Correction submit(Long id, String comment) {
    Correction c = editable(id);
    List<CorrectionLine> lines = c.getLines();
    if (lines.size() < MIN_LINES || !c.isBalanced()) {
      throw new BusinessRuleException(
          "ACSL_CORRECTION_UNBALANCED",
          "A correction needs at least two lines and equal debits ("
              + c.totalDebit().toPlainString()
              + ") and credits ("
              + c.totalCredit().toPlainString()
              + ")");
    }
    workflow.transition(Acsl.CORRECTION_ENTITY, key(c), "submit", Acsl.note(comment));
    c.submitted(currentUser.username(), clock.instant());
    notifier.team(
        "ACSL_REVIEW",
        c.getCorrectionNo() + " for review",
        c.getDescription(),
        Acsl.correctionLink(c.getId()),
        Acsl.CORRECTION_ENTITY,
        c.getId());
    audit.record(Acsl.CORRECTION_ENTITY, c.getCorrectionNo(), AuditAction.SUBMIT, "Submitted");
    return c;
  }

  /**
   * Endorses a reviewed correction for approval (ACSL 2.10.0); not by its preparer.
   *
   * @param id correction
   * @param comment comment
   * @return the correction
   */
  public Correction endorse(Long id, String comment) {
    Correction c = get(id);
    Acsl.requireStage(c.getCorrectionNo(), c.getStage(), CorrectionStage.FOR_REVIEW);
    if (CurrentUser.sameUser(currentUser.username(), c.getSubmittedBy())) {
      throw new BusinessRuleException(
          Acsl.FOUR_EYES, "A correction is reviewed by someone other than its preparer");
    }
    workflow.transition(Acsl.CORRECTION_ENTITY, key(c), "endorse", Acsl.note(comment));
    c.reviewed(currentUser.username(), clock.instant());
    notifier.team(
        "ACSL_APPROVE",
        c.getCorrectionNo() + " for approval",
        c.getDescription(),
        Acsl.correctionLink(c.getId()),
        Acsl.CORRECTION_ENTITY,
        c.getId());
    audit.record(Acsl.CORRECTION_ENTITY, c.getCorrectionNo(), AuditAction.UPDATE, "Endorsed");
    return c;
  }

  /**
   * A correction with its lines.
   *
   * @param id id
   * @return correction
   */
  @Transactional(readOnly = true)
  public Correction get(Long id) {
    return corrections
        .findLoaded(id)
        .orElseThrow(() -> new ResourceNotFoundException(Acsl.CORRECTION_ENTITY, id));
  }

  private Correction editable(Long id) {
    Correction c = get(id);
    Acsl.requireStage(c.getCorrectionNo(), c.getStage(), CorrectionStage.DRAFT);
    return c;
  }

  private static String key(Correction c) {
    return String.valueOf(c.getId());
  }

  /**
   * A correction as raised.
   *
   * @param kind kind (ACSL_CORRECTION_KIND)
   * @param invoiceNo invoice corrected, may be blank
   * @param originalBatchNo journal corrected, may be blank
   * @param description description
   */
  public record Draft(String kind, String invoiceNo, String originalBatchNo, String description) {}

  /**
   * A wrong-account proposal (ACSL 2.9.1).
   *
   * @param batchNo original journal
   * @param lineNo original line
   * @param targetAccountCode the right account
   * @param targetPartyCode the right party, null keeps the original's
   * @param component Operations ledger component of the reversed line, may be null
   * @param targetComponent component of the re-posted line, null for the same component
   */
  public record Proposal(
      String batchNo,
      int lineNo,
      String targetAccountCode,
      String targetPartyCode,
      String component,
      String targetComponent) {

    /**
     * The component of the re-posted line.
     *
     * @return target component, or the reversed line's
     */
    public String targetComponentOrSame() {
      return targetComponent == null || targetComponent.isBlank() ? component : targetComponent;
    }
  }
}
