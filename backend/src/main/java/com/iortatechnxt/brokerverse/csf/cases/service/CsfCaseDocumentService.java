package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseQueryService.DocumentView;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documents of a case (FR-CSF-054): upload with a type of the contact centre list, under the file
 * checks of the platform, linked to the case and to its client so that it also shows on the
 * Documents tab of the Servicing View; downloads checked against the access to the case. Uploads
 * and downloads are in the case history and the audit trail.
 */
@Service
@Transactional
public class CsfCaseDocumentService {

  /** Entity type of a client in the attachments. */
  static final String CLIENT_ENTITY = "Client";

  private final CsfCaseQueryService queries;
  private final CsfCaseAccess service;
  private final DocumentService documents;
  private final AttachmentService attachments;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param queries case access
   * @param service actions allowed
   * @param documents document upload
   * @param attachments downloads
   * @param lovs document types
   * @param audit case history
   * @param clock clock
   */
  public CsfCaseDocumentService(
      CsfCaseQueryService queries,
      CsfCaseAccess service,
      DocumentService documents,
      AttachmentService attachments,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.queries = queries;
    this.service = service;
    this.documents = documents;
    this.attachments = attachments;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Uploads a document to an open case.
   *
   * @param companyId company
   * @param id case
   * @param documentType type of list CSF_DOCUMENT_TYPE
   * @param file file
   * @return the document
   */
  public DocumentView upload(Long companyId, Long id, String documentType, UploadedFile file) {
    CsfCase c = queries.inCompany(companyId, id);
    if (c.status().closed()) {
      throw new BusinessRuleException(
          "CSF_CASE_ACTION_NOT_ALLOWED",
          "The case is " + c.status().label() + ". This action is not allowed");
    }
    service.requireMayWork(c);
    requireUploadable(documentType, file);
    Attachment saved =
        documents
            .upload(
                CsfCaseQueryService.target(c),
                List.of(file),
                new UploadOptions(documentType, false, null, "Case " + c.caseNo()))
            .get(0);
    if (c.party().clientId() != null) {
      documents.link(
          saved.getId(),
          List.of(new AttachmentTarget(CLIENT_ENTITY, String.valueOf(c.party().clientId()))));
    }
    audit.recordChange(
        CsfCaseCodes.ENTITY,
        c.caseNo(),
        AuditAction.UPDATE,
        "Document uploaded",
        null,
        saved.getFileName());
    return new DocumentView(
        saved.getId(),
        saved.getFileName(),
        saved.getDocumentType(),
        saved.getSizeBytes(),
        saved.getCreatedBy(),
        saved.getCreatedAt());
  }

  private void requireUploadable(String documentType, UploadedFile file) {
    if (file == null || file.content() == null || file.content().length == 0) {
      throw new BusinessRuleException("CSF_FILE_REQUIRED", "Choose the file to upload");
    }
    boolean allowed =
        documentType != null
            && lovs
                .activeValues(CsfCaseCodes.LOV_DOCUMENT_TYPE, BusinessClock.today(clock))
                .stream()
                .anyMatch(v -> v.getCode().equals(documentType));
    if (!allowed) {
      throw new BusinessRuleException("CSF_DOCUMENT_TYPE_REQUIRED", "Select the document type");
    }
  }

  /**
   * Downloads a document of a case (logged in the case history).
   *
   * @param companyId company
   * @param id case
   * @param attachmentId document
   * @return download
   */
  public FileDownload download(Long companyId, Long id, Long attachmentId) {
    CsfCase c = queries.inCompany(companyId, id);
    Attachment a = queries.requireDocument(c, attachmentId);
    audit.recordChange(
        CsfCaseCodes.ENTITY,
        c.caseNo(),
        AuditAction.EXPORT,
        "Document downloaded",
        null,
        a.getFileName());
    return attachments.downloadable(a.getId());
  }
}
