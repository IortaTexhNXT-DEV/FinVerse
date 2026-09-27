package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.csf.domain.ActivityAction;
import com.iortatechnxt.brokerverse.csf.domain.CsfActivity;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.DocumentView;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.service.QuotationQueryService;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documents of the Servicing View (FR-CSF-030, 032, 033; BRCSF-006, 007, 009): the documents of the
 * client, its accounts and its quotations (renewal advices, e-policies, claims reports, quotations
 * and uploads) under the document access classes, the upload of a document to the client or an
 * account with a type of list CSF_DOCUMENT_TYPE, and downloads, one file or a ZIP. Every upload and
 * download is logged.
 */
@Service
@Transactional
public class CsfDocumentService {

  private final DocumentService documents;
  private final CsfClients clients;
  private final QuotationQueryService quotations;
  private final CsfSupport support;

  /**
   * Creates the service.
   *
   * @param documents platform documents
   * @param clients clients of the company
   * @param quotations quotations of the client
   * @param support platform collaborators
   */
  public CsfDocumentService(
      DocumentService documents,
      CsfClients clients,
      QuotationQueryService quotations,
      CsfSupport support) {
    this.documents = documents;
    this.clients = clients;
    this.quotations = quotations;
    this.support = support;
  }

  /**
   * Documents of the client's records the user may see, newest first.
   *
   * @param companyId company
   * @param clientId client
   * @return documents, each once
   */
  @Transactional(readOnly = true)
  public List<DocumentView> list(Long companyId, Long clientId) {
    Client client = clients.require(companyId, clientId);
    return list(client);
  }

  /**
   * Documents of a type (the Renewal Advice tab lists RENEWAL_ADVICE).
   *
   * @param companyId company
   * @param clientId client
   * @param documentType document type
   * @return documents
   */
  @Transactional(readOnly = true)
  public List<DocumentView> ofType(Long companyId, Long clientId, String documentType) {
    return list(companyId, clientId).stream()
        .filter(d -> documentType.equals(d.documentType()))
        .toList();
  }

  private List<DocumentView> list(Client client) {
    Map<Long, DocumentView> found = new LinkedHashMap<>();
    for (Owner owner : owners(client)) {
      for (Attachment a : documents.list(owner.target())) {
        found.putIfAbsent(a.getId(), view(a, owner));
      }
    }
    return found.values().stream()
        .sorted(Comparator.comparing(DocumentView::uploadedAt).reversed())
        .toList();
  }

  private List<Owner> owners(Client client) {
    List<Owner> owners = new ArrayList<>();
    owners.add(
        new Owner(
            new AttachmentTarget(CsfCodes.ENTITY_CLIENT, String.valueOf(client.getId())),
            client.getCode()));
    for (Account a : clients.accountsOf(client)) {
      owners.add(
          new Owner(
              new AttachmentTarget(CsfCodes.ENTITY_ACCOUNT, String.valueOf(a.getId())),
              a.getArn()));
    }
    for (Quotation q : quotations.byClient(client.getId())) {
      owners.add(
          new Owner(
              new AttachmentTarget(CsfCodes.ENTITY_QUOTATION, String.valueOf(q.getId())),
              q.getQuotationNo()));
    }
    return owners;
  }

  private static DocumentView view(Attachment a, Owner owner) {
    return new DocumentView(
        a.getId(),
        a.getFileName(),
        a.getDocumentType(),
        a.getContentType(),
        a.getSizeBytes(),
        a.getCreatedBy(),
        a.getCreatedAt(),
        owner.target().entityType(),
        owner.reference());
  }

  /**
   * One document of the client's records, checked to belong to them and visible to the user.
   *
   * @param companyId company
   * @param clientId client
   * @param attachmentId attachment
   * @return the document
   */
  @Transactional(readOnly = true)
  public DocumentView require(Long companyId, Long clientId, Long attachmentId) {
    return list(companyId, clientId).stream()
        .filter(d -> d.id().equals(attachmentId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Document", attachmentId));
  }

  /**
   * Uploads a document to the client or one of its accounts.
   *
   * @param companyId company
   * @param clientId client
   * @param upload target account (null for the client), document type, description and file
   * @return the stored document
   */
  public DocumentView upload(Long companyId, Long clientId, Upload upload) {
    Client client = clients.require(companyId, clientId);
    if (upload.file() == null || upload.file().content() == null) {
      throw new BusinessRuleException("CSF_FILE_REQUIRED", "Choose the file to upload");
    }
    if (upload.documentType() == null || upload.documentType().isBlank()) {
      throw new BusinessRuleException("CSF_DOCUMENT_TYPE_REQUIRED", "Select the document type");
    }
    support
        .lovs()
        .requireValid(
            CsfCodes.LOV_DOCUMENT_TYPE,
            upload.documentType(),
            BusinessClock.today(support.clock()));
    Owner owner =
        upload.accountId() == null
            ? new Owner(
                new AttachmentTarget(CsfCodes.ENTITY_CLIENT, String.valueOf(client.getId())),
                client.getCode())
            : accountOwner(client, upload.accountId());
    Attachment saved =
        documents
            .upload(
                owner.target(),
                List.of(upload.file()),
                new UploadOptions(upload.documentType(), false, null, upload.description()))
            .get(0);
    support
        .activity()
        .record(
            companyId,
            ActivityAction.UPLOAD,
            new CsfActivity.Subject(
                client.getId(),
                client.getCode(),
                owner.reference() + " / " + saved.getId(),
                saved.getFileName()
                    + " ("
                    + saved.getSizeBytes()
                    + " bytes, SHA-256 "
                    + saved.getSha256()
                    + ")"));
    return view(saved, owner);
  }

  private Owner accountOwner(Client client, Long accountId) {
    Account a = clients.requireAccount(client, accountId);
    return new Owner(
        new AttachmentTarget(CsfCodes.ENTITY_ACCOUNT, String.valueOf(a.getId())), a.getArn());
  }

  /**
   * Downloads one document of the client's records (access classes apply; audited and logged).
   *
   * @param companyId company
   * @param clientId client
   * @param attachmentId attachment
   * @return download
   */
  public FileDownload download(Long companyId, Long clientId, Long attachmentId) {
    Client client = clients.require(companyId, clientId);
    DocumentView d = require(companyId, clientId, attachmentId);
    FileDownload download = documents.downloadable(attachmentId);
    support
        .activity()
        .record(
            companyId,
            ActivityAction.DOWNLOAD,
            new CsfActivity.Subject(
                clientId, client.getCode(), d.reference() + " / " + d.id(), d.fileName()));
    return download;
  }

  /**
   * Several documents of the client's records as one ZIP file (logged).
   *
   * @param companyId company
   * @param clientId client
   * @param attachmentIds attachments
   * @return ZIP bytes
   */
  public byte[] zip(Long companyId, Long clientId, List<Long> attachmentIds) {
    Client client = clients.require(companyId, clientId);
    Set<Long> own = list(client).stream().map(DocumentView::id).collect(Collectors.toSet());
    List<Long> missing = attachmentIds.stream().filter(id -> !own.contains(id)).toList();
    if (!missing.isEmpty()) {
      throw new ResourceNotFoundException("Document", missing.get(0));
    }
    byte[] zip = documents.zip(attachmentIds);
    support
        .activity()
        .record(
            companyId,
            ActivityAction.DOWNLOAD,
            new CsfActivity.Subject(
                clientId,
                client.getCode(),
                "ZIP",
                attachmentIds.size() + " documents: " + attachmentIds));
    return zip;
  }

  /**
   * A record documents belong to.
   *
   * @param target record
   * @param reference client code, ARN or quotation number
   */
  private record Owner(AttachmentTarget target, String reference) {}

  /**
   * A document to upload.
   *
   * @param accountId account of the client, null for the client itself
   * @param documentType document type (list CSF_DOCUMENT_TYPE)
   * @param description description, may be null
   * @param file the file
   */
  public record Upload(
      Long accountId, String documentType, String description, UploadedFile file) {}
}
