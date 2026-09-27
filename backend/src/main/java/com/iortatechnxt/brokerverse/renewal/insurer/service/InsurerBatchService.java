package com.iortatechnxt.brokerverse.renewal.insurer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatch;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchLine;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchLineRepository;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchRepository;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer batches (FR-RN-070): the For Renewal accounts in processing of one insurer and expiry
 * range, frozen with the 28 columns of the insurer extract, downloaded as a spreadsheet and sent to
 * the insurer's mailbox protected with a password sent separately. Sending moves the accounts to
 * With Insurer and sets the reply date.
 */
@Service
@Transactional
public class InsurerBatchService {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final Set<InsurerBatchStatus> OPEN =
      EnumSet.of(
          InsurerBatchStatus.DRAFT,
          InsurerBatchStatus.SENT,
          InsurerBatchStatus.PARTIALLY_RESPONDED);
  private static final TypeReference<List<String>> ROW = new TypeReference<>() {};

  private final InsurerBatchRepository batches;
  private final InsurerBatchLineRepository lines;
  private final RenewalCandidateRepository candidates;
  private final InsurerExtract extract;
  private final InsurerProfileRepository insurers;
  private final RenewalFlow flow;
  private final RenewalParameters parameters;
  private final DocumentNumberService numbers;
  private final DocumentComposer composer;
  private final DocTemplateService templates;
  private final DocumentService documents;
  private final MessageService messages;
  private final ObjectMapper json;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param lines batch lines
   * @param candidates renewals
   * @param extract extract columns
   * @param insurers insurers
   * @param flow workflow
   * @param parameters parameters
   * @param numbers document numbers
   * @param composer spreadsheet writer
   * @param templates document templates
   * @param documents attachments
   * @param messages e-mail
   * @param json JSON
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings({"java:S107", "PMD.ExcessiveParameterList"}) // constructor injection
  public InsurerBatchService(
      InsurerBatchRepository batches,
      InsurerBatchLineRepository lines,
      RenewalCandidateRepository candidates,
      InsurerExtract extract,
      InsurerProfileRepository insurers,
      RenewalFlow flow,
      RenewalParameters parameters,
      DocumentNumberService numbers,
      DocumentComposer composer,
      DocTemplateService templates,
      DocumentService documents,
      MessageService messages,
      ObjectMapper json,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.batches = batches;
    this.lines = lines;
    this.candidates = candidates;
    this.extract = extract;
    this.insurers = insurers;
    this.flow = flow;
    this.parameters = parameters;
    this.numbers = numbers;
    this.composer = composer;
    this.templates = templates;
    this.documents = documents;
    this.messages = messages;
    this.json = json;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The batches of a company, newest first.
   *
   * @param companyId company
   * @return batches
   */
  @Transactional(readOnly = true)
  public List<InsurerBatch> list(Long companyId) {
    return batches.findByCompanyIdOrderByIdDesc(companyId);
  }

  /**
   * A batch.
   *
   * @param companyId company
   * @param batchNo batch number
   * @return batch
   */
  @Transactional(readOnly = true)
  public InsurerBatch get(Long companyId, String batchNo) {
    return batches
        .findByCompanyIdAndBatchNo(companyId, batchNo)
        .orElseThrow(() -> new ResourceNotFoundException("Insurer batch", batchNo));
  }

  /**
   * The lines of a batch with their frozen columns.
   *
   * @param companyId company
   * @param batchNo batch number
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<Line> lines(Long companyId, String batchNo) {
    InsurerBatch batch = get(companyId, batchNo);
    List<Line> result = new ArrayList<>();
    for (InsurerBatchLine l : lines.findByBatchIdOrderByIdAsc(batch.getId())) {
      String ref =
          candidates.findById(l.getCandidateId()).map(RenewalCandidate::getRenewalRef).orElse(null);
      result.add(new Line(ref, l.isResponded(), read(l.getSnapshot())));
    }
    return result;
  }

  /**
   * Builds a batch of the For Renewal accounts of an insurer expiring in a range.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param from first expiry
   * @param to last expiry
   * @return the batch
   */
  public InsurerBatch create(Long companyId, String insurerCode, LocalDate from, LocalDate to) {
    requireSelection(insurerCode, from, to);
    InsurerProfile insurer = insurer(companyId, insurerCode);
    List<RenewalCandidate> selected =
        candidates
            .findByCompanyIdAndStageIn(companyId, List.of(RenewalStage.IN_PROCESSING))
            .stream()
            .filter(c -> eligible(c, insurerCode, from, to))
            .toList();
    if (selected.isEmpty()) {
      throw new BusinessRuleException(
          "RNW_BATCH_EMPTY",
          "No For Renewal account of " + insurer.getName() + " expires in the range");
    }
    String no = numbers.next("RIB-" + BusinessClock.today(clock).getYear());
    InsurerBatch batch = batches.save(new InsurerBatch(companyId, no, insurerCode, from, to));
    for (RenewalCandidate c : selected) {
      lines.save(new InsurerBatchLine(batch.getId(), c.getId(), write(extract.row(c))));
    }
    batch.built(selected.size(), null);
    audit.record(
        RenewalCodes.ENTITY_BATCH,
        no,
        AuditAction.CREATE,
        insurer.getName() + ", " + selected.size() + " account(s) expiring " + from + " to " + to);
    return batch;
  }

  /**
   * The extract of a batch as a spreadsheet.
   *
   * @param companyId company
   * @param batchNo batch number
   * @return file
   */
  @Transactional(readOnly = true)
  public MessageFile file(Long companyId, String batchNo) {
    InsurerBatch batch = get(companyId, batchNo);
    List<List<Object>> rows = new ArrayList<>();
    for (InsurerBatchLine l : lines.findByBatchIdOrderByIdAsc(batch.getId())) {
      rows.add(new ArrayList<>(read(l.getSnapshot())));
    }
    byte[] xlsx = composer.xlsx(new SheetSpec("Renewals", InsurerExtract.HEADERS, rows));
    return new MessageFile(batchNo + ".xlsx", XLSX, xlsx);
  }

  /**
   * Sends a batch to the insurer, protected, and moves its accounts to With Insurer.
   *
   * @param companyId company
   * @param batchNo batch number
   * @return the batch
   */
  public InsurerBatch send(Long companyId, String batchNo) {
    InsurerBatch batch = get(companyId, batchNo);
    if (batch.getStatus() != InsurerBatchStatus.DRAFT) {
      throw new BusinessRuleException("RNW_BATCH_SENT", "Batch " + batchNo + " is already sent");
    }
    InsurerProfile insurer = insurer(companyId, batch.getInsurerCode());
    List<String> to = insurer.getPlacementEmailList();
    if (to.isEmpty()) {
      throw new BusinessRuleException(
          "RNW_BATCH_MAILBOX", insurer.getName() + " has no e-mail address for renewals");
    }
    MessageFile file = file(companyId, batchNo);
    Attachment stored = store(batch, file);
    LocalDate today = BusinessClock.today(clock);
    LocalDate due = today.plusDays(parameters.insurerReplyDays());
    Map<String, Object> values =
        Map.of(
            "batchNo", batchNo,
            "insurerName", insurer.getName(),
            "expiryFrom", batch.getExpiryFrom(),
            "expiryTo", batch.getExpiryTo(),
            "count", batch.getLineCount(),
            "replyDue", due);
    MergedText text = templates.merge(RenewalCodes.TEMPLATE_INSURER_COVER, today, values);
    QueuedEmail queued =
        messages.queueEmail(
            new OutboundEmail(
                companyId,
                RenewalCodes.PURPOSE_INSURER,
                to,
                List.of(),
                DocTemplateService.fill(text.title(), values),
                text.text(),
                List.of(file),
                new OutboundEmail.Protection(null, true, null),
                new RecordLink(RenewalCodes.ENTITY_BATCH, batch.getId().toString(), batchNo)));
    batch.sent(
        queued.messageId(), String.join(", ", to), currentUser.username(), clock.instant(), due);
    batch.built(batch.getLineCount(), stored.getId());
    for (InsurerBatchLine l : lines.findByBatchIdOrderByIdAsc(batch.getId())) {
      candidates
          .findById(l.getCandidateId())
          .filter(c -> c.getStage() == RenewalStage.IN_PROCESSING)
          .ifPresent(
              c ->
                  flow.act(
                      c, "send_to_insurer", TransitionNote.comment("Insurer batch " + batchNo)));
    }
    audit.record(
        RenewalCodes.ENTITY_BATCH,
        batchNo,
        AuditAction.SUBMIT,
        "Sent to " + String.join(", ", to) + ", reply due " + due);
    return batch;
  }

  /**
   * The open batch line of a renewal, if any.
   *
   * @param c renewal
   * @return batch and line
   */
  @Transactional(readOnly = true)
  public java.util.Optional<InsurerBatch> openBatchOf(RenewalCandidate c) {
    return lines.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .map(l -> batches.findById(l.getBatchId()).orElse(null))
        .filter(Objects::nonNull)
        .filter(b -> OPEN.contains(b.getStatus()))
        .findFirst();
  }

  private static void requireSelection(String insurerCode, LocalDate from, LocalDate to) {
    if (insurerCode == null || insurerCode.isBlank()) {
      throw new BusinessRuleException("RNW_BATCH_INSURER", "Select the insurer");
    }
    if (from == null || to == null || to.isBefore(from)) {
      throw new BusinessRuleException("RNW_RANGE_INVALID", "Enter a valid start and end date");
    }
  }

  private boolean eligible(RenewalCandidate c, String insurerCode, LocalDate from, LocalDate to) {
    boolean inRange = !c.getExpiryDate().isBefore(from) && !c.getExpiryDate().isAfter(to);
    return c.getDisposition().code() == RenewalDisposition.FOR_RENEWAL
        && insurerCode.equals(c.getSnapshot().insurerCode())
        && inRange
        && !inOpenBatch(c);
  }

  private boolean inOpenBatch(RenewalCandidate c) {
    return lines.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .filter(l -> !l.isResponded())
        .map(l -> batches.findById(l.getBatchId()).orElse(null))
        .anyMatch(b -> b != null && OPEN.contains(b.getStatus()));
  }

  private Attachment store(InsurerBatch batch, MessageFile file) {
    return documents
        .upload(
            new AttachmentTarget(RenewalCodes.ENTITY_BATCH, batch.getId().toString()),
            List.of(new UploadedFile(file.fileName(), file.content())),
            new UploadOptions(
                RenewalCodes.DOC_INSURER_FILE, false, batch.getBatchNo(), "Insurer extract", null))
        .get(0);
  }

  private InsurerProfile insurer(Long companyId, String code) {
    return insurers
        .findByCompanyIdAndPartyCode(companyId, code)
        .orElseThrow(() -> new ResourceNotFoundException("Insurer", code));
  }

  private String write(List<String> row) {
    try {
      return json.writeValueAsString(row);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Insurer extract row not serialisable", e);
    }
  }

  private List<String> read(String snapshot) {
    try {
      return json.readValue(snapshot, ROW);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Insurer extract row not readable", e);
    }
  }

  /**
   * A line of a batch.
   *
   * @param renewalRef renewal
   * @param responded whether the insurer answered
   * @param columns the 28 columns
   */
  public record Line(String renewalRef, boolean responded, List<String> columns) {}
}
