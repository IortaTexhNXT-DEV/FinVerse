package com.iortatechnxt.brokerverse.eb.member.service;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService;
import com.iortatechnxt.brokerverse.eb.document.service.EbDocumentService.Registration;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChangeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbResponsibleParty;
import com.iortatechnxt.brokerverse.eb.domain.EbRosterVersion;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Member changes of a programme line (BRID-013, 025; FR-EB-055, 056), numbered {@code
 * EBM-<yyyy>-nnnnnn} with their {@code EB_MEMBER_CHANGE} work case: captured by the AO against the
 * accepted roster (the client's request entered with its files), relayed to the insurer by e-mail
 * (a billing item is opened), billed (the insurer's billing, or the direct billing uploaded as
 * {@code EB_DIRECT_BILLING} for Processing and Collection), validated by Processing (the
 * endorsement request of a change with a premium effect is raised in Operations) and closed, when
 * its lines are applied to the roster. Return and cancel are generic actions of the workflow panel.
 */
@Service
@Transactional
public class MemberChangeService {

  private static final Set<String> SOURCES = Set.of("AO", "CLIENT");

  private final EbMemberChangeRepository changes;
  private final EbRecords records;
  private final RosterService roster;
  private final MemberChangeRules rules;
  private final MemberChangeEffects effects;
  private final MemberChangeMail mail;
  private final EbDocumentService documents;
  private final TrackedItemService trackedItems;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final EbWorkingDays workingDays;
  private final EbParameters parameters;
  private final EbActivityLog activity;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param changes member changes
   * @param records programme look-up
   * @param roster accepted roster
   * @param rules line checks
   * @param effects endorsement request and roster update
   * @param mail relay e-mail
   * @param documents EB document register
   * @param trackedItems billing items
   * @param workflow workflow engine
   * @param numbers document numbers
   * @param workingDays working-day calendar
   * @param parameters EB parameters
   * @param activity TAT stamps
   * @param notifications in-app notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public MemberChangeService(
      EbMemberChangeRepository changes,
      EbRecords records,
      RosterService roster,
      MemberChangeRules rules,
      MemberChangeEffects effects,
      MemberChangeMail mail,
      EbDocumentService documents,
      TrackedItemService trackedItems,
      WorkflowService workflow,
      DocumentNumberService numbers,
      EbWorkingDays workingDays,
      EbParameters parameters,
      EbActivityLog activity,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.changes = changes;
    this.records = records;
    this.roster = roster;
    this.rules = rules;
    this.effects = effects;
    this.mail = mail;
    this.documents = documents;
    this.trackedItems = trackedItems;
    this.workflow = workflow;
    this.numbers = numbers;
    this.workingDays = workingDays;
    this.parameters = parameters;
    this.activity = activity;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Captures a member change.
   *
   * @param companyId company
   * @param programmeId programme
   * @param input line, source, flags, lines and files
   * @return the change, CAPTURED
   */
  public EbMemberChange capture(Long companyId, Long programmeId, MemberChangeInput input) {
    EbProgramme programme = records.programme(companyId, programmeId);
    EbProgrammeLine line = programme.line(input.lineNo());
    String source = input.source() == null ? "AO" : input.source();
    if (!SOURCES.contains(source)) {
      throw new BusinessRuleException("EB_MEMBER_SOURCE", "Select who asked for the change");
    }
    if (input.lines().isEmpty()) {
      throw new BusinessRuleException("EB_MEMBER_LINES_REQUIRED", "Add at least one member line");
    }
    EbRosterVersion accepted = acceptedRoster(programme, input.policyYear());
    Set<String> seen = MemberChangeRules.seen();
    List<EbMemberChange.LineData> lines = new ArrayList<>();
    for (MemberChangeInput.Line l : input.lines()) {
      lines.add(rules.check(accepted, line, l, seen));
    }
    String number =
        numbers.next(
            EbCodes.series(
                EbCodes.PREFIX_MEMBER_CHANGE, BusinessClock.currentYear(clock).getValue()));
    EbMemberChange change =
        new EbMemberChange(
            programme,
            number,
            line,
            new EbMemberChange.Header(
                accepted.getPolicyYear(), source, input.financial(), input.description()));
    lines.forEach(change::addLine);
    EbMemberChange saved = changes.save(change);
    workflow.start(
        new StartCase(
            companyId,
            EbCodes.WORKFLOW_MEMBER_CHANGE,
            new CaseRecord(
                EbCodes.ENTITY_MEMBER_CHANGE,
                saved.getId().toString(),
                number,
                "Member change " + programme.getName() + " - " + MemberChangeEffects.summary(saved),
                EbCodes.MEMBER_CHANGE_LINK + saved.getId(),
                programme.getTeamCode()),
            null));
    if (!input.files().isEmpty()) {
      store(
          programme, saved, EbDocumentTypes.MEMBER_CHANGE, EbDocumentSource.CLIENT, input.files());
    }
    activity.received(
        companyId, programme.getId(), TatActivity.MEMBER_CHANGE, number, currentUser.username());
    audit.record(
        EbCodes.ENTITY_MEMBER_CHANGE,
        number,
        AuditAction.CREATE,
        "Captured on line " + line.getLineNo() + ": " + MemberChangeEffects.summary(saved));
    return saved;
  }

  private EbRosterVersion acceptedRoster(EbProgramme programme, Integer year) {
    if (year != null) {
      return roster.accepted(programme.getId(), year).orElseThrow(() -> noRoster(programme));
    }
    return roster.versions(programme.getCompanyId(), programme.getId()).stream()
        .filter(v -> v.getStatus() == EbRosterVersion.Status.ACCEPTED)
        .findFirst()
        .orElseThrow(() -> noRoster(programme));
  }

  private static BusinessRuleException noRoster(EbProgramme programme) {
    return new BusinessRuleException(
        "EB_ROSTER_REQUIRED",
        "Programme "
            + programme.getProgrammeNo()
            + " has no accepted roster: upload the master list");
  }

  /**
   * Relays a captured change to the insurer of the line and opens its billing item.
   *
   * @param companyId company
   * @param changeId change
   * @return the change, RELAYED
   */
  public EbMemberChange relay(Long companyId, Long changeId) {
    EbMemberChange change = require(companyId, changeId);
    requireStatus(change, EbMemberChange.Status.CAPTURED);
    EbProgramme programme = records.programme(companyId, change.getProgrammeId());
    EbProgrammeLine line = programme.line(change.getLineNo());
    Long message = mail.relay(programme, line, change);
    change.relayed(clock.instant(), message);
    transition(change, "relay", TransitionNote.NONE);
    LocalDate due =
        workingDays.plus(
            companyId, BusinessClock.today(clock), parameters.tatDays(TatActivity.MEMBER_CHANGE));
    trackedItems.openLinked(
        programme,
        MemberChangeEffects.BILLING,
        new EbTrackedItem.Details(
            "Billing of member change " + change.getChangeNo(),
            null,
            change.getChangeNo(),
            line.getCurrentArn(),
            EbResponsibleParty.INSURER,
            line.getIncumbentInsurer(),
            null,
            due,
            null),
        null,
        change.getId());
    audit.record(
        EbCodes.ENTITY_MEMBER_CHANGE,
        change.getChangeNo(),
        AuditAction.SUBMIT,
        "Relayed to the insurer");
    return change;
  }

  /**
   * Records the insurer's billing; a direct billing is uploaded with its files.
   *
   * @param companyId company
   * @param changeId relayed change
   * @param billing date, reference, amount, direct flag
   * @param files direct billing files (required when direct)
   * @return the change, BILLED
   */
  public EbMemberChange bill(
      Long companyId, Long changeId, EbMemberChange.Billing billing, List<UploadedFile> files) {
    EbMemberChange change = require(companyId, changeId);
    LocalDate today = BusinessClock.today(clock);
    if (billing.billedOn() != null && billing.billedOn().isAfter(today)) {
      throw new BusinessRuleException(
          "EB_BILLING_DATE_FUTURE", "The billing date cannot be after today");
    }
    if (billing.direct() && (files == null || files.isEmpty())) {
      throw new BusinessRuleException(
          "EB_BILLING_FILE_REQUIRED", "Attach the insurer's direct billing");
    }
    if (change.isFinancial() && (billing.amount() == null || billing.amount().signum() == 0)) {
      throw new BusinessRuleException(
          "EB_BILLING_AMOUNT_REQUIRED",
          "Enter the amount billed for a change with a premium effect");
    }
    change.billed(
        new EbMemberChange.Billing(
            billing.billedOn() == null ? today : billing.billedOn(),
            billing.reference(),
            billing.amount(),
            billing.direct()));
    EbProgramme programme = records.programme(companyId, change.getProgrammeId());
    if (files != null && !files.isEmpty()) {
      store(programme, change, EbDocumentTypes.DIRECT_BILLING, EbDocumentSource.INSURER, files);
    }
    transition(change, "bill", TransitionNote.NONE);
    trackedItems.receiveLinked(change.getId(), MemberChangeEffects.BILLING, change.getBilledOn());
    Notice notice =
        new Notice(
            change.getChangeNo()
                + ": member change billed"
                + (billing.direct() ? " (direct billing)" : ""),
            programme.getName(),
            EbCodes.MEMBER_CHANGE_LINK + change.getId(),
            EbCodes.ENTITY_MEMBER_CHANGE,
            change.getId().toString());
    notifications.notifyPermission(
        EbCodes.PERMISSION_PROCESS, notice, EbCodes.EVENT_MEMBER_CHANGE_BILLED);
    notifications.notifyPermission(
        EbCodes.PERMISSION_COLLECT, notice, EbCodes.EVENT_MEMBER_CHANGE_BILLED);
    audit.record(
        EbCodes.ENTITY_MEMBER_CHANGE,
        change.getChangeNo(),
        AuditAction.UPDATE,
        "Billed"
            + (billing.direct() ? " directly by the insurer" : "")
            + (billing.reference() == null ? "" : ", billing " + billing.reference()));
    return change;
  }

  /**
   * Validates a billed change (Processing); a change with a premium effect raises its endorsement
   * request unless the parameter holds it until the billing is paid.
   *
   * @param companyId company
   * @param changeId billed change
   * @return the change, VALIDATED
   */
  public EbMemberChange validate(Long companyId, Long changeId) {
    EbMemberChange change = require(companyId, changeId);
    requireStatus(change, EbMemberChange.Status.BILLED);
    EbProgramme programme = records.programme(companyId, change.getProgrammeId());
    change.validated(currentUser.username(), clock.instant());
    if (change.isFinancial() && !parameters.adjustmentRequiresPayment()) {
      effects.raiseEndorsement(change, programme.line(change.getLineNo()));
    }
    transition(change, "validate", TransitionNote.NONE);
    audit.record(
        EbCodes.ENTITY_MEMBER_CHANGE,
        change.getChangeNo(),
        AuditAction.AUTHORIZE,
        "Validated"
            + (change.getEndorsementRequestNo() == null
                ? ""
                : ", endorsement request " + change.getEndorsementRequestNo()));
    return change;
  }

  /**
   * Closes a validated change: its lines are applied to the accepted roster.
   *
   * @param companyId company
   * @param changeId validated change
   * @return the change, CLOSED
   */
  public EbMemberChange close(Long companyId, Long changeId) {
    EbMemberChange change = require(companyId, changeId);
    requireStatus(change, EbMemberChange.Status.VALIDATED);
    EbProgramme programme = records.programme(companyId, change.getProgrammeId());
    EbRosterVersion accepted = acceptedRoster(programme, change.getPolicyYear());
    effects.apply(programme, change, accepted);
    transition(change, "close", TransitionNote.NONE);
    activity.released(TatActivity.MEMBER_CHANGE, change.getChangeNo(), "Closed");
    audit.record(
        EbCodes.ENTITY_MEMBER_CHANGE,
        change.getChangeNo(),
        AuditAction.CLOSE,
        "Applied to the roster "
            + accepted.getPolicyYear()
            + " ("
            + accepted.getHeadcount()
            + " members)");
    return change;
  }

  private void store(
      EbProgramme programme,
      EbMemberChange change,
      String type,
      EbDocumentSource source,
      List<UploadedFile> files) {
    String process =
        change.isFinancial() ? EbDocumentTypes.ENDORSEMENT : EbDocumentTypes.ADJUSTMENT;
    documents.storeOn(
        programme,
        new AttachmentTarget(EbCodes.ENTITY_MEMBER_CHANGE, change.getId().toString()),
        new Registration(type, process, source, false, change.getChangeNo()),
        files,
        List.of());
  }

  private static void requireStatus(EbMemberChange change, EbMemberChange.Status status) {
    if (change.getStatus() != status) {
      throw new BusinessRuleException(
          "EB_MEMBER_CHANGE_STATUS",
          "Member change "
              + change.getChangeNo()
              + " is "
              + change.getStatus().name().toLowerCase(java.util.Locale.ROOT));
    }
  }

  private void transition(EbMemberChange change, String action, TransitionNote note) {
    workflow.systemTransition(
        EbCodes.ENTITY_MEMBER_CHANGE, change.getId().toString(), action, note);
  }

  /**
   * A member change of a company.
   *
   * @param companyId company
   * @param changeId change
   * @return change
   */
  @Transactional(readOnly = true)
  public EbMemberChange require(Long companyId, Long changeId) {
    return changes
        .findByIdAndCompanyId(changeId, companyId)
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_MEMBER_CHANGE, changeId));
  }
}
