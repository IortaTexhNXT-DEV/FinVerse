package com.iortatechnxt.brokerverse.submitted.masterlist.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.issuance.domain.ExtractionPattern;
import com.iortatechnxt.brokerverse.issuance.service.ExtractionProposal;
import com.iortatechnxt.brokerverse.issuance.service.ExtractionRequest;
import com.iortatechnxt.brokerverse.issuance.service.PolicyDataExtractor;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmExtractedValue;
import com.iortatechnxt.brokerverse.submitted.domain.SbmExtraction;
import com.iortatechnxt.brokerverse.submitted.domain.SbmExtractionRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyOrigin;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService.UpsertContext;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Document extraction with confirmation (BRIDSP-02; FRS FR-SP-002): a policy document is attached
 * to a record (or waits for a new one), its fields are proposed by the issuance extractor (kind
 * SUBMITTED_POLICY; a scanned document without text goes to manual entry while OCR is parked), and
 * nothing is written to the masterlist until the user confirms the values, corrected where needed.
 * A rejection keeps the document with the reason.
 */
@Service("sbmExtractionService")
@Transactional
public class ExtractionService {

  /** Attachment entity type of a document waiting for its new record. */
  public static final String PENDING_ENTITY = "SubmittedExtraction";

  private static final String EXTRACTOR = "Policy text reader";

  private final SbmExtractionRepository extractions;
  private final MasterlistService masterlist;
  private final AttachmentService attachments;
  private final DocumentService documents;
  private final PolicyDataExtractor extractor;
  private final NotificationService notifications;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param extractions extractions
   * @param masterlist masterlist
   * @param attachments attachments
   * @param documents documents (links)
   * @param extractor issuance extractor
   * @param notifications notifications
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the extraction
  public ExtractionService(
      SbmExtractionRepository extractions,
      MasterlistService masterlist,
      AttachmentService attachments,
      DocumentService documents,
      PolicyDataExtractor extractor,
      NotificationService notifications,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.extractions = extractions;
    this.masterlist = masterlist;
    this.attachments = attachments;
    this.documents = documents;
    this.extractor = extractor;
    this.notifications = notifications;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Uploads a policy document and proposes its fields.
   *
   * @param upload company, record or segment, file
   * @return the extraction
   */
  public SbmExtraction upload(DocumentUpload upload) {
    SbmPolicy policy = upload.policyId() == null ? null : masterlist.get(upload.policyId());
    String no = numbers.next("SBX-" + BusinessClock.today(clock).getYear());
    AttachmentTarget target =
        policy == null
            ? new AttachmentTarget(PENDING_ENTITY, no)
            : new AttachmentTarget(SubmittedCodes.ENTITY, policy.getId().toString());
    Attachment file =
        attachments.upload(
            target,
            upload.fileName(),
            upload.content(),
            "Policy document",
            SubmittedCodes.DOC_POLICY);
    ExtractionProposal proposal =
        extractor.propose(
            new ExtractionRequest(
                ExtractionPattern.KIND_SUBMITTED,
                upload.content(),
                policy == null ? null : policy.getTerms().insurerCode()));
    Map<String, SbmExtractedValue> fields = new LinkedHashMap<>();
    proposal
        .fields()
        .forEach((k, v) -> fields.put(k, new SbmExtractedValue(v.value(), v.confidence())));
    SbmExtraction saved =
        extractions.save(
            new SbmExtraction(
                upload.companyId(),
                no,
                new SbmExtraction.Target(
                    policy == null ? null : policy.getId(),
                    policy == null ? upload.segment() : policy.getSegment(),
                    policy == null ? upload.businessType() : policy.getBusinessType()),
                new SbmExtraction.Document(file.getId(), file.getFileName(), EXTRACTOR),
                new SbmExtraction.Proposal(fields, proposal.readable(), proposal.note())));
    if (policy != null) {
      policy.documentAttached();
    }
    notifications.notifyPermission(
        "SBM_MAINTAIN",
        new Notice(
            "Policy document " + no + " to confirm",
            file.getFileName(),
            "/submitted/extractions",
            "SubmittedExtraction",
            saved.getId().toString()),
        SubmittedCodes.EVT_MANUAL_VALIDATION);
    audit.record(PENDING_ENTITY, no, AuditAction.CREATE, "Proposal from " + file.getFileName());
    return saved;
  }

  /**
   * Confirms the values of an extraction: they are written to the record (a new record when the
   * document had none), which becomes VALIDATED.
   *
   * @param id extraction
   * @param data values as confirmed by the user
   * @return the record
   */
  public SbmPolicy confirm(Long id, SbmPolicyData data) {
    SbmExtraction x = get(id);
    SbmPolicy p;
    if (x.getPolicyId() != null) {
      p = masterlist.get(x.getPolicyId());
      masterlist.write(p, data, SbmHistorySource.EXTRACTION, x.getExtractionNo());
    } else {
      p =
          masterlist
              .upsert(
                  x.getCompanyId(),
                  data,
                  new UpsertContext(
                      new SbmPolicyOrigin(
                          SubmittedCodes.SOURCE_DOCUMENT,
                          null,
                          BusinessClock.today(clock),
                          SbmPolicyStatus.VALIDATED),
                      SbmHistorySource.EXTRACTION,
                      x.getExtractionNo(),
                      currentUser.username()))
              .policy();
      if (p.getStatus() == SbmPolicyStatus.RECEIVED) {
        masterlist.write(p, data, SbmHistorySource.EXTRACTION, x.getExtractionNo());
      }
      documents.link(
          x.getAttachmentId(),
          List.of(new AttachmentTarget(SubmittedCodes.ENTITY, p.getId().toString())));
    }
    p.documentAttached();
    x.confirm(p.getId(), currentUser.username(), clock.instant());
    audit.record(
        PENDING_ENTITY, x.getExtractionNo(), AuditAction.AUTHORIZE, "Confirmed to " + p.getSbmNo());
    return p;
  }

  /**
   * Rejects an extraction with a reason; the document is kept.
   *
   * @param id extraction
   * @param reason reason
   * @return the extraction
   */
  public SbmExtraction reject(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException(
          "SBM_REJECT_REASON_REQUIRED", "Enter the reason of the rejection");
    }
    SbmExtraction x = get(id);
    x.reject(reason.strip(), currentUser.username(), clock.instant());
    audit.record(PENDING_ENTITY, x.getExtractionNo(), AuditAction.REJECT, reason.strip());
    return x;
  }

  /**
   * An extraction.
   *
   * @param id extraction
   * @return extraction
   */
  @Transactional(readOnly = true)
  public SbmExtraction get(Long id) {
    return extractions
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Extraction", id));
  }

  /**
   * Extractions in some statuses, newest first.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return extractions
   */
  @Transactional(readOnly = true)
  public Page<SbmExtraction> list(
      Long companyId, List<SbmExtraction.Status> statuses, Pageable pageable) {
    return extractions.findByCompanyIdAndStatusInOrderByIdDesc(companyId, statuses, pageable);
  }

  /**
   * Extractions of a record.
   *
   * @param policyId record
   * @return extractions
   */
  @Transactional(readOnly = true)
  public List<SbmExtraction> ofPolicy(Long policyId) {
    return extractions.findByPolicyIdOrderByIdDesc(policyId);
  }

  /**
   * A document to extract.
   *
   * @param companyId company
   * @param policyId existing record, null for a new one
   * @param segment segment of a new record
   * @param businessType business type of a new record
   * @param fileName file name
   * @param content file bytes
   */
  public record DocumentUpload(
      Long companyId,
      Long policyId,
      String segment,
      SbmBusinessType businessType,
      String fileName,
      byte[] content) {

    /** Defensive copy. */
    public DocumentUpload {
      content = content == null ? new byte[0] : content.clone();
    }

    /**
     * The file bytes.
     *
     * @return a copy
     */
    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof DocumentUpload d
          && java.util.Objects.equals(fileName, d.fileName)
          && java.util.Arrays.equals(content, d.content);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(fileName, java.util.Arrays.hashCode(content));
    }

    @Override
    public String toString() {
      return "DocumentUpload[" + fileName + "]";
    }
  }
}
