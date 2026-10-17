package com.iortatechnxt.brokerverse.csf.cases.service;

import static com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseInputs.blankToNull;
import static com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseInputs.requireText;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.csf.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.AccountRef;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Category;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Escalation;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Handling;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Party;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Sla;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The actions on a case (FR-CSF-050 to 058, 060): log a case or resolve it on the call, change its
 * category or priority, resolve, return or request CSR action by the fulfilment unit, re-endorse or
 * close by the agent, reassign, escalate and close by the supervisor, notes, follow-up cases and
 * the SLA notices. Each action is checked against the status of the case (section 5.3), written to
 * the case history (the audit trail of the case) in the same transaction and notified to the next
 * owner.
 */
@Service
@Transactional
public class CsfCaseService {

  private static final String RESOLUTION_REQUIRED = "Enter the resolution given to the client";

  private final CsfCaseStore cases;
  private final CsfCaseSetupStore setup;
  private final CsfCaseAccess access;
  private final CsfCaseInputs inputs;
  private final CsfCaseCalendar calendar;
  private final CsfCaseNotices notices;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param setup automatic escalation of the case types
   * @param access access and allowed actions
   * @param inputs checked inputs
   * @param calendar working days
   * @param notices notifications
   * @param numbers Case IDs
   * @param audit case history
   * @param currentUser signed-in user
   * @param clock clock
   */
  @SuppressWarnings("java:S107")
  public CsfCaseService(
      CsfCaseStore cases,
      CsfCaseSetupStore setup,
      CsfCaseAccess access,
      CsfCaseInputs inputs,
      CsfCaseCalendar calendar,
      CsfCaseNotices notices,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.cases = cases;
    this.setup = setup;
    this.access = access;
    this.inputs = inputs;
    this.calendar = calendar;
    this.notices = notices;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * A new case.
   *
   * @param clientId client, null for a caller who is not a client
   * @param accountId account of the client, may be null
   * @param callerName caller name (no client)
   * @param callerContact caller mobile or e-mail (no client)
   * @param category channel, type, nature, product, segment, unit and priority
   * @param subject subject
   * @param description description
   * @param resolveNow resolved on the call (Resolve and Close)
   * @param resolution resolution (Resolve and Close)
   * @param closureReason closure reason (Resolve and Close), default Information provided
   */
  public record LogRequest(
      Long clientId,
      Long accountId,
      String callerName,
      String callerContact,
      Category category,
      String subject,
      String description,
      boolean resolveNow,
      String resolution,
      String closureReason) {}

  /**
   * Logs a case (FR-CSF-050): Submit to Fulfilment Unit, or Resolve and Close.
   *
   * @param companyId company
   * @param r request
   * @return the case
   */
  public CsfCase log(Long companyId, LogRequest r) {
    access.requirePermission(Permission.CSF_CASE_CREATE);
    Party party = inputs.party(companyId, r.clientId(), r.callerName(), r.callerContact());
    AccountRef account = inputs.account(companyId, r.clientId(), r.accountId());
    Category category = inputs.category(r.category());
    requireText(
        r.subject(), "CSF_CASE_SUBJECT_REQUIRED", "Complete the mandatory fields - subject");
    requireText(
        r.description(),
        "CSF_CASE_DESCRIPTION_REQUIRED",
        "Complete the mandatory fields - description");
    if (r.resolveNow()) {
      requireText(r.resolution(), "CSF_CASE_RESOLUTION_REQUIRED", RESOLUTION_REQUIRED);
    }
    if (party.clientId() != null && inputs.duplicateCheck()) {
      Optional<CsfCase> open =
          cases.openDuplicate(companyId, party.clientId(), account.accountId(), category.nature());
      if (open.isPresent()) {
        throw new BusinessRuleException(
            "CSF_CASE_DUPLICATE",
            "Case "
                + open.get().caseNo()
                + " is already open for this client, account and nature of transaction. Add a"
                + " note to it instead");
      }
    }
    return create(companyId, party, account, category, r, null, null);
  }

  /**
   * Creates a follow-up case from a closed case (FR-CSF-058).
   *
   * @param id closed case
   * @param reason reason for closure and re-creation
   * @return the new case
   */
  public CsfCase followUp(Long id, String reason) {
    access.requirePermission(Permission.CSF_CASE_CREATE);
    CsfCase original = access.require(id);
    if (!original.status().closed()) {
      throw CsfCaseAccess.notAllowed(original);
    }
    requireText(reason, "CSF_CASE_REASON_REQUIRED", "Enter the reason for the follow-up case");
    LogRequest r =
        new LogRequest(
            original.party().clientId(),
            original.account().accountId(),
            original.party().callerName(),
            original.party().callerContact(),
            original.category(),
            original.subject(),
            original.description(),
            false,
            null,
            null);
    CsfCase created =
        create(
            original.companyId(),
            original.party(),
            original.account(),
            original.category(),
            r,
            original,
            reason.strip());
    audit.recordChange(
        CsfCaseCodes.ENTITY,
        original.caseNo(),
        AuditAction.CREATE,
        "Follow-up case created: " + reason.strip(),
        null,
        created.caseNo());
    return created;
  }

  /**
   * Changes the case type or the nature of transaction (FR-CSF-051); the due date is recomputed
   * from the creation time.
   *
   * @param id case
   * @param caseType new case type
   * @param nature new nature of transaction
   * @param reason reason
   * @return the case
   */
  public CsfCase changeCategory(Long id, String caseType, String nature, String reason) {
    access.requirePermission(Permission.CSF_CASE_SUPERVISE);
    CsfCase c = access.requireOpen(id);
    requireText(reason, "CSF_CASE_REASON_REQUIRED", "Enter the reason for changing the category");
    String type = inputs.active(CsfCaseCodes.LOV_TYPE, caseType, "case type");
    String nat = inputs.active(CsfCaseCodes.LOV_NATURE, nature, "nature of transaction");
    int tat = inputs.tatOf(type);
    LocalDate due =
        CsfCaseCalendar.plus(BusinessClock.dateOf(c.createdAt()), tat, calendar.of(c.companyId()));
    CsfCase changed =
        save(c, x -> x.withCategory(x.recategorised(type, nat, null, null)).withDue(tat, due));
    history(
        c,
        "Category changed: " + reason.strip(),
        inputs.categoryText(c.category().caseType(), c.category().nature()),
        inputs.categoryText(type, nat) + ", due " + due);
    notices.categoryChanged(changed);
    return changed;
  }

  /**
   * Resolves and closes a case by the fulfilment unit (FR-CSF-060).
   *
   * @param id case
   * @param resolution resolution
   * @param closureReason closure reason
   * @return the case
   */
  public CsfCase resolve(Long id, String resolution, String closureReason) {
    CsfCase c = access.requireFulfilment(id);
    CsfCase closed = close(c, CaseStatus.CLOSED_BY_FULFILMENT, resolution, closureReason);
    notices.toOwner(closed, "resolved and closed by the fulfilment unit");
    return closed;
  }

  /**
   * Returns a case to the agent (FR-CSF-060).
   *
   * @param id case
   * @param reason return reason
   * @param comment comment
   * @return the case
   */
  public CsfCase returnToAgent(Long id, String reason, String comment) {
    CsfCase c = access.requireFulfilment(id);
    String why = inputs.active(CsfCaseCodes.LOV_RETURN, reason, "return reason");
    String words = inputs.label(CsfCaseCodes.LOV_RETURN, why);
    CsfCase changed =
        move(
            c,
            CaseStatus.RETURNED_TO_AGENT,
            h ->
                new Handling(
                    why,
                    h.instruction(),
                    h.resolution(),
                    h.closureReason(),
                    h.closedAt(),
                    h.closedBy()));
    note(changed, "Returned to the agent: " + words, comment);
    notices.toOwner(changed, "returned to you: " + words);
    return changed;
  }

  /**
   * Asks the agent to act with the client (FR-CSF-060).
   *
   * @param id case
   * @param instruction instruction to the agent
   * @return the case
   */
  public CsfCase requestCsrAction(Long id, String instruction) {
    CsfCase c = access.requireFulfilment(id);
    requireText(instruction, "CSF_CASE_INSTRUCTION_REQUIRED", "Enter the instruction to the agent");
    String text = instruction.strip();
    CsfCase changed =
        move(
            c,
            CaseStatus.CSR_ACTION_REQUIRED,
            h ->
                new Handling(
                    h.returnReason(),
                    text,
                    h.resolution(),
                    h.closureReason(),
                    h.closedAt(),
                    h.closedBy()));
    note(changed, "CSR action requested", text);
    notices.toOwner(changed, "needs your action with the client");
    return changed;
  }

  /**
   * Re-endorses a returned case to the fulfilment unit (FR-CSF-060).
   *
   * @param id case
   * @param comment comment
   * @return the case
   */
  public CsfCase reendorse(Long id, String comment) {
    access.requirePermission(Permission.CSF_CASE_CREATE);
    CsfCase c = access.require(id);
    if (!CaseStatus.withAgent().contains(c.status())) {
      throw CsfCaseAccess.notAllowed(c);
    }
    access.requireOwnerOrSupervisor(c);
    CsfCase changed = move(c, CaseStatus.WITH_FULFILMENT, UnaryOperator.identity());
    note(changed, "Re-endorsed to the fulfilment unit", comment);
    notices.routed(changed, "re-endorsed to your unit");
    return changed;
  }

  /**
   * Closes a case as Closed by CSR: the agent a returned case or one waiting for his action, the
   * supervisor any open case (FR-CSF-056, 058).
   *
   * @param id case
   * @param resolution resolution
   * @param closureReason closure reason
   * @return the case
   */
  public CsfCase closeByCsr(Long id, String resolution, String closureReason) {
    CsfCase c = access.requireOpen(id);
    if (!access.has(Permission.CSF_CASE_SUPERVISE)) {
      access.requirePermission(Permission.CSF_CASE_CREATE);
      if (!CaseStatus.withAgent().contains(c.status())) {
        throw CsfCaseAccess.notAllowed(c);
      }
      access.requireOwnerOrSupervisor(c);
    }
    CsfCase closed = close(c, CaseStatus.CLOSED_BY_CSR, resolution, closureReason);
    notices.toUnit(closed, "closed by the contact centre");
    return closed;
  }

  /**
   * Reassigns a case to another agent or another fulfilment unit (FR-CSF-056).
   *
   * @param id case
   * @param toUser new owner, may be null
   * @param toUnit new fulfilment unit, may be null
   * @param comment comment
   * @return the case
   */
  public CsfCase reassign(Long id, String toUser, String toUnit, String comment) {
    access.requirePermission(Permission.CSF_CASE_SUPERVISE);
    CsfCase c = access.requireOpen(id);
    String user = blankToNull(toUser);
    String unit = blankToNull(toUnit);
    if (user == null && unit == null) {
      throw new BusinessRuleException(
          "CSF_CASE_REASSIGN_TARGET", "Select the agent or the fulfilment unit");
    }
    CsfCase changed = c;
    if (user != null && !CurrentUser.sameUser(user, c.owner())) {
      changed = save(changed, x -> x.withOwner(user));
      history(changed, "Reassigned" + CsfCaseInputs.suffix(comment), c.owner(), user);
      notices.toOwner(changed, "assigned to you");
    }
    if (unit != null && !unit.equals(c.category().fulfilmentUnit())) {
      String code = inputs.active(CsfCaseCodes.LOV_UNIT, unit, "fulfilment unit");
      changed = save(changed, x -> x.withCategory(x.recategorised(null, null, code, null)));
      history(
          changed,
          "Reassigned to another fulfilment unit" + CsfCaseInputs.suffix(comment),
          inputs.label(CsfCaseCodes.LOV_UNIT, c.category().fulfilmentUnit()),
          inputs.label(CsfCaseCodes.LOV_UNIT, code));
      notices.routed(changed, "reassigned to your unit");
    }
    return changed;
  }

  /**
   * Escalates a case with a reason (FR-CSF-056); the status does not change.
   *
   * @param id case
   * @param reason escalation reason
   * @param comment comment
   * @return the case
   */
  public CsfCase escalate(Long id, String reason, String comment) {
    access.requirePermission(Permission.CSF_CASE_SUPERVISE);
    CsfCase c = access.requireOpen(id);
    return markEscalated(
        c, inputs.active(CsfCaseCodes.LOV_ESCALATION, reason, "escalation reason"), comment);
  }

  /**
   * Changes the priority of a case (supervisor).
   *
   * @param id case
   * @param priority new priority
   * @return the case
   */
  public CsfCase changePriority(Long id, String priority) {
    access.requirePermission(Permission.CSF_CASE_SUPERVISE);
    CsfCase c = access.requireOpen(id);
    String p = inputs.active(CsfCaseCodes.LOV_PRIORITY, priority, "priority");
    CsfCase changed = save(c, x -> x.withCategory(x.recategorised(null, null, null, p)));
    history(
        changed,
        "Priority changed",
        inputs.label(CsfCaseCodes.LOV_PRIORITY, c.category().priority()),
        inputs.label(CsfCaseCodes.LOV_PRIORITY, p));
    return changed;
  }

  /**
   * Adds a note (FR-CSF-053); notes are append-only.
   *
   * @param id case
   * @param text note
   * @return the case
   */
  public CsfCase addNote(Long id, String text) {
    CsfCase c = access.requireOpen(id);
    access.requireMayWork(c);
    requireText(text, "CSF_CASE_NOTE_REQUIRED", "Enter the note");
    String body = text.strip();
    cases.addNote(c.id(), body, currentUser.username(), clock.instant());
    audit.recordChange(
        CsfCaseCodes.ENTITY, c.caseNo(), AuditAction.UPDATE, "Note added", null, body);
    if (access.isUnitMember(c)) {
      notices.toOwner(c, "has a new note from the fulfilment unit");
    } else {
      notices.toUnit(c, "has a new note from the contact centre");
    }
    return c;
  }

  /**
   * Records the SLA status of an open case, sends the approaching-breach and breach notices once
   * and escalates a breached case of a case type set for it (FR-CSF-057); used by the SLA check.
   *
   * @param c open case
   * @param slaStatus SLA status for today
   * @return true when something changed
   */
  public boolean applySla(CsfCase c, String slaStatus) {
    Sla before = c.sla();
    Sla after =
        new Sla(
            slaStatus,
            before.nearNotified() || CsfCaseCodes.APPROACHING.equals(slaStatus),
            before.breachNotified() || CsfCaseCodes.BREACHED.equals(slaStatus));
    if (after.equals(before)) {
      return false;
    }
    CsfCase changed = save(c, x -> x.withSla(after));
    if (!slaStatus.equals(before.status())) {
      history(changed, "SLA status changed", before.status(), slaStatus);
    }
    if (after.nearNotified() != before.nearNotified()) {
      notices.sla(changed, "is approaching its due date " + changed.dueDate());
    }
    if (after.breachNotified() != before.breachNotified()) {
      notices.sla(changed, "is past its due date " + changed.dueDate());
      escalateOnBreach(changed);
    }
    return true;
  }

  private void escalateOnBreach(CsfCase c) {
    boolean auto =
        setup
            .typeSetting(c.category().caseType())
            .map(CsfCaseSetupStore.TypeSetting::autoEscalate)
            .orElse(true);
    if (auto && !c.escalation().escalated()) {
      markEscalated(c, CsfCaseCodes.PAST_TAT, "Escalated automatically on breach");
    }
  }

  private CsfCase create(
      Long companyId,
      Party party,
      AccountRef account,
      Category category,
      LogRequest r,
      CsfCase original,
      String followUpReason) {
    String user = currentUser.username();
    Instant now = clock.instant();
    int tat = inputs.tatOf(category.caseType());
    LocalDate due = CsfCaseCalendar.plus(BusinessClock.today(clock), tat, calendar.of(companyId));
    CsfCase c =
        new CsfCase(
            null,
            0,
            companyId,
            numbers.next(inputs.numberPrefix()),
            party,
            account,
            category,
            r.subject().strip(),
            r.description().strip(),
            r.resolveNow() ? CaseStatus.CLOSED_BY_CSR : CaseStatus.WITH_FULFILMENT,
            user,
            user,
            tat,
            due,
            new Sla(CsfCaseCodes.WITHIN_TAT, false, false),
            new Escalation(false, null, null),
            r.resolveNow()
                ? resolvedOnCall(r, user, now)
                : new Handling(null, null, null, null, null, null),
            original == null ? null : original.id(),
            followUpReason,
            now);
    CsfCase saved = cases.find(cases.insert(c)).orElseThrow();
    audit.recordChange(
        CsfCaseCodes.ENTITY,
        saved.caseNo(),
        AuditAction.CREATE,
        "Case logged: "
            + inputs.categoryText(category.caseType(), category.nature())
            + ", "
            + inputs.label(CsfCaseCodes.LOV_UNIT, category.fulfilmentUnit())
            + ", due "
            + due,
        null,
        saved.status().label());
    if (!r.resolveNow()) {
      notices.routed(saved, "routed to your unit");
    }
    return saved;
  }

  private static Handling resolvedOnCall(LogRequest r, String user, Instant now) {
    String closure =
        Optional.ofNullable(blankToNull(r.closureReason())).orElse("INFORMATION_PROVIDED");
    return new Handling(null, null, r.resolution().strip(), closure, now, user);
  }

  private CsfCase close(CsfCase c, CaseStatus to, String resolution, String closureReason) {
    requireText(resolution, "CSF_CASE_RESOLUTION_REQUIRED", RESOLUTION_REQUIRED);
    if (blankToNull(closureReason) == null) {
      throw new BusinessRuleException("CSF_CASE_CLOSURE_REASON", "Select the closure reason");
    }
    String reason = inputs.active(CsfCaseCodes.LOV_CLOSURE, closureReason, "closure reason");
    String text = resolution.strip();
    String user = currentUser.username();
    Instant now = clock.instant();
    CsfCase closed =
        move(c, to, h -> new Handling(h.returnReason(), h.instruction(), text, reason, now, user));
    note(closed, "Resolution", text);
    return closed;
  }

  private CsfCase move(CsfCase c, CaseStatus to, UnaryOperator<Handling> handling) {
    CsfCase changed = save(c, x -> x.withStatus(to, handling.apply(x.handling())));
    audit.recordChange(
        CsfCaseCodes.ENTITY,
        c.caseNo(),
        to.closed() ? AuditAction.CLOSE : AuditAction.UPDATE,
        "Status changed",
        c.status().label(),
        to.label());
    return changed;
  }

  private CsfCase markEscalated(CsfCase c, String reason, String comment) {
    Instant now = clock.instant();
    CsfCase changed = save(c, x -> x.withEscalation(new Escalation(true, reason, now)));
    String words = inputs.label(CsfCaseCodes.LOV_ESCALATION, reason);
    history(changed, "Escalated" + CsfCaseInputs.suffix(comment), "No", words);
    notices.escalated(changed, words);
    return changed;
  }

  private CsfCase save(CsfCase c, UnaryOperator<CsfCase> change) {
    cases.update(change.apply(c), currentUser.username(), clock.instant());
    return cases.find(c.id()).orElseThrow();
  }

  private void note(CsfCase c, String what, String text) {
    String body = blankToNull(text) == null ? what : what + ": " + text.strip();
    cases.addNote(c.id(), body, currentUser.username(), clock.instant());
  }

  private void history(CsfCase c, String summary, String before, String after) {
    audit.recordChange(CsfCaseCodes.ENTITY, c.caseNo(), AuditAction.UPDATE, summary, before, after);
  }
}
