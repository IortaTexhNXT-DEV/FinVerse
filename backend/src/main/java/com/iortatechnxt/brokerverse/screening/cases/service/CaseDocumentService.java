package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentNamingService;
import com.iortatechnxt.brokerverse.attachment.service.NamingFacts;
import com.iortatechnxt.brokerverse.attachment.service.NamingPattern;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.crm.service.KycDocumentService;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocument;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocument.DocumentMeta;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocumentRepository;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KYC and supporting documents of a case (SNSRP-601; FR-SS-052): stored as attachments of the case
 * with their form type, document type, date received and source, named by the BRD convention {@code
 * <Form Type>_<Client Name>_<Date Received>_<Document Type>_<n>} (pattern SCREENING), shown on the
 * timeline and, for KYC document types, registered on the client's KYC documents as well. Documents
 * are never removed from a case.
 */
@Service
@Transactional
public class CaseDocumentService {

  private static final String CLIENT_DOCUMENT_TYPES = "DOCUMENT_TYPE";
  private static final String OTHERS = "OTHERS";

  private final CaseDocumentRepository documents;
  private final AttachmentService attachments;
  private final DocumentNamingService naming;
  private final KycDocumentService kyc;
  private final ClientService clients;
  private final LovService lovs;
  private final CaseAccess access;
  private final CaseTimeline timeline;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param documents document metadata
   * @param attachments attachments
   * @param naming document naming
   * @param kyc client KYC documents
   * @param clients client master (read)
   * @param lovs lists of values
   * @param access case access
   * @param timeline case timeline
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the document flow
  public CaseDocumentService(
      CaseDocumentRepository documents,
      AttachmentService attachments,
      DocumentNamingService naming,
      KycDocumentService kyc,
      ClientService clients,
      LovService lovs,
      CaseAccess access,
      CaseTimeline timeline,
      AuditTrailService audit,
      Clock clock) {
    this.documents = documents;
    this.attachments = attachments;
    this.naming = naming;
    this.kyc = kyc;
    this.clients = clients;
    this.lovs = lovs;
    this.access = access;
    this.timeline = timeline;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Uploads a document with its metadata.
   *
   * @param c the case
   * @param meta form type, document type, date received and source
   * @param fileName the uploaded file name
   * @param content the file
   * @return the document
   */
  public CaseDocument upload(ScreeningCase c, DocumentMeta meta, String fileName, byte[] content) {
    if (!c.isOpen()) {
      throw new BusinessRuleException("SCR_CASE_CLOSED", "Case " + c.getCaseNo() + " is closed");
    }
    access.requireActor(c, "Upload", EnumSet.of(c.getStage()));
    LocalDate today = LocalDate.now(clock);
    validate(meta, today);
    LovValue type = lovs.requireValid(CaseCodes.DOCUMENT_TYPE_LOV, meta.documentType(), today);
    int sequence = (int) documents.countByCaseIdAndDocumentType(c.getId(), meta.documentType()) + 1;
    String name =
        naming.nominate(
            NamingPattern.SCREENING,
            new NamingFacts(
                c.getCaseNo(),
                meta.formType(),
                c.getClientName(),
                meta.dateReceived(),
                meta.documentType(),
                sequence,
                fileName));
    Attachment stored =
        attachments.upload(
            new AttachmentTarget(CaseCodes.ENTITY, String.valueOf(c.getId())),
            name,
            content,
            type.getLabel() + " (" + meta.source().strip() + ")");
    CaseDocument document =
        documents.save(new CaseDocument(c.getId(), stored.getId(), meta, sequence, name));
    registerOnClient(c, type, name, content).ifPresent(t -> document.registeredOnClient());
    timeline.record(
        c,
        CaseEventType.DOCUMENT_ADDED,
        new EventFacts(
            null,
            null,
            null,
            name,
            meta.documentType(),
            "Received "
                + meta.dateReceived()
                + " from "
                + meta.source().strip()
                + "; SHA-256 "
                + stored.getSha256()));
    audit.record(
        CaseCodes.ENTITY, c.getCaseNo(), AuditAction.UPDATE, "Document " + name + " uploaded");
    return document;
  }

  private void validate(DocumentMeta meta, LocalDate today) {
    requirePresent(meta.formType(), "form type");
    requirePresent(meta.documentType(), "document type");
    if (meta.dateReceived() == null) {
      throw missing("date received");
    }
    requirePresent(meta.source(), "source");
    if (meta.dateReceived().isAfter(today)) {
      throw new BusinessRuleException(
          "SCR_DATE_RECEIVED_FUTURE", "The date received cannot be in the future");
    }
    lovs.requireValid(CaseCodes.FORM_TYPE_LOV, meta.formType(), today);
  }

  private static void requirePresent(String value, String field) {
    if (value == null || value.isBlank()) {
      throw missing(field);
    }
  }

  private static BusinessRuleException missing(String field) {
    return new BusinessRuleException(
        "SCR_DOCUMENT_META_REQUIRED", "Enter the " + field + " of the document");
  }

  private Optional<String> registerOnClient(
      ScreeningCase c, LovValue type, String name, byte[] content) {
    String clientType = type.getParentCode();
    boolean kycType =
        clientType != null
            && !OTHERS.equals(clientType)
            && lovs.activeValues(CLIENT_DOCUMENT_TYPES, LocalDate.now(clock)).stream()
                .anyMatch(v -> v.getCode().equals(clientType));
    if (!kycType || clients.get(c.getClientId()).getStatus() == ClientStatus.INACTIVE) {
      return Optional.empty();
    }
    kyc.upload(c.getClientId(), clientType, name, content);
    return Optional.of(clientType);
  }

  /**
   * The documents of a case in upload order.
   *
   * @param caseId the case
   * @return documents
   */
  @Transactional(readOnly = true)
  public List<CaseDocument> of(Long caseId) {
    return documents.findByCaseIdOrderByIdAsc(caseId);
  }
}
