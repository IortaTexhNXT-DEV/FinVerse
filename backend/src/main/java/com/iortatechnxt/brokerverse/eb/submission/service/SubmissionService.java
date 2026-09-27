package com.iortatechnxt.brokerverse.eb.submission.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.document.service.CycleDocuments;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChangeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.domain.EbRequiredDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbSubmission;
import com.iortatechnxt.brokerverse.eb.domain.EbSubmissionRepository;
import com.iortatechnxt.brokerverse.eb.service.EbMailer;
import com.iortatechnxt.brokerverse.eb.service.EbMailer.Mail;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbTemplates;
import com.iortatechnxt.brokerverse.eb.service.RequiredDocumentCheck;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Submissions of documents to an insurer per process (BRID-026; FR-EB-034): new-business or renewal
 * placement, adjustment, endorsement, franchise or proposal, on a cycle or a member change. The
 * checklist of the required documents of the process and benefit lines is shown with the documents
 * found; the submission is refused while a mandatory one is missing, then sent by protected e-mail
 * (template {@code EB_SUBMISSION_COVER}) to the insurer's placement mailboxes, logged, and
 * Processing and Collection are notified.
 */
@Service
@Transactional
public class SubmissionService {

  private final EbSubmissionRepository submissions;
  private final EbMemberChangeRepository changes;
  private final EbRecords records;
  private final EbParties parties;
  private final RequiredDocumentCheck required;
  private final CycleDocuments cycleDocuments;
  private final DocumentService documents;
  private final EbMailer mailer;
  private final EbTemplates templates;
  private final LovService lovs;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param submissions submissions
   * @param changes member changes
   * @param records programme and cycle look-up
   * @param parties insurers and template values
   * @param required required documents
   * @param cycleDocuments documents of a cycle
   * @param documents documents of a member change
   * @param mailer e-mails
   * @param templates cover template
   * @param lovs process list
   * @param notifications in-app notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public SubmissionService(
      EbSubmissionRepository submissions,
      EbMemberChangeRepository changes,
      EbRecords records,
      EbParties parties,
      RequiredDocumentCheck required,
      CycleDocuments cycleDocuments,
      DocumentService documents,
      EbMailer mailer,
      EbTemplates templates,
      LovService lovs,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.submissions = submissions;
    this.changes = changes;
    this.records = records;
    this.parties = parties;
    this.required = required;
    this.cycleDocuments = cycleDocuments;
    this.documents = documents;
    this.mailer = mailer;
    this.templates = templates;
    this.lovs = lovs;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The checklist of a submission: the required documents of the process and the files found.
   *
   * @param companyId company
   * @param scope programme with its cycle or member change, and the process
   * @return checklist, mandatory first
   */
  @Transactional(readOnly = true)
  public List<ChecklistItem> checklist(Long companyId, Scope scope) {
    Found found = found(companyId, scope);
    List<ChecklistItem> items = new ArrayList<>();
    for (EbRequiredDocument r : required.of(companyId, scope.processType(), found.lines())) {
      List<Long> files = found.files().getOrDefault(r.getDocumentType(), List.of());
      items.add(
          new ChecklistItem(
              r.getDocumentType(),
              lovs.label("DOCUMENT_TYPE", r.getDocumentType()),
              r.isMandatory(),
              !files.isEmpty(),
              files));
    }
    return items;
  }

  /**
   * Submits the documents to an insurer.
   *
   * @param companyId company
   * @param scope programme with its cycle or member change, and the process
   * @param input insurer, further documents and remarks
   * @return the submission
   */
  public EbSubmission submit(Long companyId, Scope scope, SubmissionInput input) {
    lovs.requireValid(EbDocumentTypes.PROCESS_TYPE_LOV, scope.processType(), BusinessClock.today(clock));
    InsurerProfile insurer = parties.insurer(companyId, input.insurerCode());
    Found found = found(companyId, scope);
    required.require(companyId, scope.processType(), found.lines(), found.files().keySet());
    Set<Long> files = new HashSet<>();
    Map<Long, String> types = new LinkedHashMap<>();
    for (EbRequiredDocument r : required.of(companyId, scope.processType(), found.lines())) {
      found.files().getOrDefault(r.getDocumentType(), List.of()).forEach(id -> types.put(id, r.getDocumentType()));
    }
    for (Long extra : input.attachmentIds()) {
      String type = found.typeOf().get(extra);
      if (type == null) {
        throw new BusinessRuleException(
            "EB_SUBMISSION_DOCUMENT", "Select documents of the programme record being submitted");
      }
      types.put(extra, type);
    }
    if (types.isEmpty()) {
      throw new BusinessRuleException("EB_SUBMISSION_EMPTY", "Select the documents to submit");
    }
    files.addAll(types.keySet());
    EbProgramme programme = found.programme();
    List<String> to = EbParties.mailboxes(insurer);
    EbSubmission submission =
        new EbSubmission(
            new EbSubmission.Scope(companyId, programme.getId(), scope.cycleId()),
            scope.memberChangeId(),
            scope.processType(),
            insurer.getPartyCode(),
            new EbSubmission.Sending(
                clock.instant(), currentUser.username(), String.join(", ", to), input.remarks()));
    types.forEach(submission::addDocument);
    EbSubmission saved = submissions.save(submission);
    saved.sentAs(send(programme, saved, insurer, List.copyOf(files)));
    tellProcessing(programme, saved, insurer);
    audit.record(
        EbCodes.ENTITY_SUBMISSION,
        saved.getId(),
        AuditAction.SUBMIT,
        lovs.label(EbDocumentTypes.PROCESS_TYPE_LOV, scope.processType()) + " documents ("
            + files.size() + ") sent to " + insurer.getName());
    return saved;
  }

  private Long send(
      EbProgramme programme, EbSubmission submission, InsurerProfile insurer, List<Long> files) {
    Map<String, Object> values = parties.values(programme, null);
    values.put("insurerName", insurer.getName());
    values.put("process", lovs.label(EbDocumentTypes.PROCESS_TYPE_LOV, submission.getProcessType()));
    values.put(
        "documents",
        submission.getDocuments().stream()
            .map(d -> "- " + lovs.label("DOCUMENT_TYPE", d.getDocumentType()))
            .collect(Collectors.joining("\n")));
    values.put("remarks", submission.getRemarks() == null ? "" : submission.getRemarks());
    MergedText text = templates.merge(EbCodes.TEMPLATE_SUBMISSION_COVER, values);
    return mailer
        .send(
            programme.getCompanyId(),
            EbCodes.PURPOSE_SUBMISSION,
            new Mail(
                EbParties.mailboxes(insurer),
                parties.aoCopy(programme),
                text.title(),
                text.text(),
                mailer.files(files)),
            new RecordLink(
                EbCodes.ENTITY_SUBMISSION,
                submission.getId().toString(),
                programme.getProgrammeNo()))
        .messageId();
  }

  private void tellProcessing(EbProgramme programme, EbSubmission submission, InsurerProfile insurer) {
    Notice notice =
        new Notice(
            programme.getProgrammeNo() + ": documents submitted to " + insurer.getName(),
            lovs.label(EbDocumentTypes.PROCESS_TYPE_LOV, submission.getProcessType()) + " - "
                + programme.getName(),
            EbCodes.PROGRAMME_LINK + programme.getId() + "?tab=submissions",
            EbCodes.ENTITY_PROGRAMME,
            programme.getId().toString());
    notifications.notifyPermission(EbCodes.PERMISSION_PROCESS, notice, EbCodes.EVENT_SUBMISSION_SENT);
    notifications.notifyPermission(EbCodes.PERMISSION_COLLECT, notice, EbCodes.EVENT_SUBMISSION_SENT);
  }

  private Found found(Long companyId, Scope scope) {
    EbProgramme programme = records.programme(companyId, scope.programmeId());
    if (scope.processType() == null || scope.processType().isBlank()) {
      throw new BusinessRuleException("EB_PROCESS_REQUIRED", "Select the process of the document");
    }
    Map<String, List<Long>> byType = new LinkedHashMap<>();
    Map<Long, String> typeOf = new LinkedHashMap<>();
    List<String> lines;
    if (scope.memberChangeId() != null) {
      EbMemberChange change =
          changes
              .findByIdAndCompanyId(scope.memberChangeId(), companyId)
              .filter(c -> c.getProgrammeId().equals(programme.getId()))
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          EbCodes.ENTITY_MEMBER_CHANGE, scope.memberChangeId()));
      for (Attachment a :
          documents.all(new AttachmentTarget(EbCodes.ENTITY_MEMBER_CHANGE, change.getId().toString()))) {
        if (a.getDocumentType() != null) {
          byType.computeIfAbsent(a.getDocumentType(), t -> new ArrayList<>()).add(a.getId());
          typeOf.put(a.getId(), a.getDocumentType());
        }
      }
      lines = List.of(change.getBenefitLine());
    } else {
      if (scope.cycleId() == null) {
        throw new BusinessRuleException(
            "EB_DOCUMENT_TRANSACTION_REQUIRED", "Link the document to its cycle or member change");
      }
      EbCycle cycle = records.cycle(companyId, scope.cycleId());
      LocalDate today = BusinessClock.today(clock);
      for (String type : cycleDocuments.presentTypes(cycle, today)) {
        List<Long> ids = cycleDocuments.files(cycle, List.of(type), today);
        byType.put(type, ids);
        ids.forEach(id -> typeOf.put(id, type));
      }
      lines =
          programme.getLines().stream()
              .filter(EbProgrammeLine::isActive)
              .map(EbProgrammeLine::getBenefitLine)
              .distinct()
              .toList();
    }
    return new Found(programme, lines, byType, typeOf);
  }

  /**
   * Records the insurer's acknowledgement.
   *
   * @param companyId company
   * @param submissionId submission
   * @param date date acknowledged; today when null
   * @return the submission
   */
  public EbSubmission acknowledge(Long companyId, Long submissionId, LocalDate date) {
    EbSubmission submission =
        submissions
            .findById(submissionId)
            .filter(s -> s.getCompanyId().equals(companyId))
            .orElseThrow(
                () -> new ResourceNotFoundException(EbCodes.ENTITY_SUBMISSION, submissionId));
    LocalDate today = BusinessClock.today(clock);
    LocalDate on = date == null ? today : date;
    if (on.isAfter(today)) {
      throw new BusinessRuleException(
          "EB_SUBMISSION_DATE_FUTURE", "The acknowledgement date cannot be after today");
    }
    submission.acknowledge(on);
    return submission;
  }

  /**
   * The submissions of a programme.
   *
   * @param companyId company
   * @param programmeId programme
   * @return submissions, latest first
   */
  @Transactional(readOnly = true)
  public List<EbSubmission> ofProgramme(Long companyId, Long programmeId) {
    return submissions.findByProgrammeIdOrderBySentAtDescIdDesc(
        records.programme(companyId, programmeId).getId());
  }

  /**
   * What is submitted: the programme with its cycle or member change, and the process.
   *
   * @param programmeId programme
   * @param cycleId cycle, may be null
   * @param memberChangeId member change, may be null
   * @param processType process (list EB_PROCESS_TYPE)
   */
  public record Scope(Long programmeId, Long cycleId, Long memberChangeId, String processType) {}

  /**
   * The insurer and the further documents.
   *
   * @param insurerCode insurer
   * @param attachmentIds further documents of the record, may be empty
   * @param remarks remarks, may be null
   */
  public record SubmissionInput(String insurerCode, List<Long> attachmentIds, String remarks) {

    /** Null list becomes empty. */
    public SubmissionInput {
      attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }
  }

  /**
   * A line of the checklist.
   *
   * @param documentType document type
   * @param label its label
   * @param mandatory whether the submission is refused without it
   * @param present whether a file is found
   * @param attachmentIds the files found
   */
  public record ChecklistItem(
      String documentType, String label, boolean mandatory, boolean present, List<Long> attachmentIds) {}

  private record Found(
      EbProgramme programme,
      List<String> lines,
      Map<String, List<Long>> files,
      Map<Long, String> typeOf) {}
}
