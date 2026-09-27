package com.iortatechnxt.brokerverse.eb.document.service;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentSource;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentStatus;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The EB document register (BRID-007, 024, 025; FR-EB-002, FR-EB-030): every EB file is stored
 * through the attachment module (file store, access classes of its type) on its cycle, linked to
 * the programme and the client with its process tag, and registered with its type, process, version
 * and source. A new upload of a type supersedes the active version of that type on the cycle.
 * Upload without a process or a cycle is refused. Insurers and clients send their files by e-mail;
 * the EB user records them with the source INSURER or CLIENT.
 */
@Service
@Transactional
public class EbDocumentService {

  private final EbDocumentRepository register;
  private final EbRecords records;
  private final DocumentService documents;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param register EB document register
   * @param records cycle and programme look-up
   * @param documents attachment documents
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  public EbDocumentService(
      EbDocumentRepository register,
      EbRecords records,
      DocumentService documents,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.register = register;
    this.records = records;
    this.documents = documents;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Uploads documents of one type on a cycle from the Documents tab (FR-EB-030).
   *
   * @param companyId company
   * @param cycleId cycle (the transaction; required)
   * @param upload type, process, source, description and files
   * @return the registered documents
   */
  public List<EbDocument> upload(Long companyId, Long cycleId, Upload upload) {
    if (cycleId == null) {
      throw new BusinessRuleException(
          "EB_DOCUMENT_TRANSACTION_REQUIRED", "Link the document to its cycle or member change");
    }
    EbCycle cycle = records.openCycle(companyId, cycleId);
    String type = requireType(upload.documentType());
    String process = requireProcess(upload.processType());
    EbDocumentSource source = upload.source() == null ? EbDocumentSource.AO : upload.source();
    return store(
        cycle, new Registration(type, process, source, true, upload.description()), upload.files());
  }

  /**
   * Stores files on a cycle and registers them (used by the feedback, BOR and renewal advice).
   *
   * @param cycle cycle
   * @param registration type, process, source and whether the new version supersedes
   * @param files files
   * @return the registered documents, one per file
   */
  public List<EbDocument> store(
      EbCycle cycle, Registration registration, List<UploadedFile> files) {
    return store(cycle, registration, files, List.of());
  }

  /**
   * Stores files on a cycle, registers them and links them to further records (e.g. the accounts a
   * renewal advice renews).
   *
   * @param cycle cycle
   * @param registration type, process, source and whether the new version supersedes
   * @param files files
   * @param extraTargets further records to link, besides the programme and the client
   * @return the registered documents, one per file
   */
  public List<EbDocument> store(
      EbCycle cycle,
      Registration registration,
      List<UploadedFile> files,
      List<AttachmentTarget> extraTargets) {
    EbProgramme programme = records.programmeOf(cycle);
    List<Attachment> stored =
        documents.upload(
            new AttachmentTarget(EbCodes.ENTITY_CYCLE, cycle.getId().toString()),
            files,
            new UploadOptions(
                registration.documentType(),
                false,
                cycle.getCycleNo(),
                registration.description(),
                registration.processType()));
    List<AttachmentTarget> links = new ArrayList<>();
    links.add(new AttachmentTarget(EbCodes.ENTITY_PROGRAMME, programme.getId().toString()));
    links.add(new AttachmentTarget("Client", programme.getClientId().toString()));
    links.addAll(extraTargets);
    int version = nextVersion(cycle, registration);
    List<EbDocument> saved = new ArrayList<>();
    for (Attachment a : stored) {
      documents.link(a.getId(), links, registration.processType());
      saved.add(
          register.save(
              new EbDocument(
                  cycle.getCompanyId(),
                  new EbDocument.Place(programme.getId(), cycle.getId()),
                  registration.documentType(),
                  registration.processType(),
                  version,
                  a.getId(),
                  registration.source())));
    }
    audit.record(
        EbCodes.ENTITY_CYCLE,
        cycle.getCycleNo(),
        AuditAction.CREATE,
        lovs.label("DOCUMENT_TYPE", registration.documentType())
            + " version "
            + version
            + " ("
            + stored.size()
            + " file(s), from "
            + registration.source()
            + ")");
    return saved;
  }

  private static String requireType(String documentType) {
    String type = blankToNull(documentType);
    if (type == null) {
      throw new BusinessRuleException("EB_DOCUMENT_TYPE_REQUIRED", "Select the document type");
    }
    if (EbDocumentTypes.BOR.equals(type)) {
      throw new BusinessRuleException(
          "EB_BOR_ON_BOR_TAB", "Upload the Broker on Record on the BOR tab");
    }
    if (!EbDocumentTypes.ALL.contains(type) && !EbDocumentTypes.RENEWAL_ADVICE.equals(type)) {
      throw new BusinessRuleException(
          "EB_DOCUMENT_TYPE_INVALID", "Select an Employee Benefits document type");
    }
    return type;
  }

  private String requireProcess(String processType) {
    String process = blankToNull(processType);
    if (process == null) {
      throw new BusinessRuleException("EB_PROCESS_REQUIRED", "Select the process of the document");
    }
    lovs.requireValid(EbDocumentTypes.PROCESS_TYPE_LOV, process, BusinessClock.today(clock));
    return process;
  }

  private int nextVersion(EbCycle cycle, Registration registration) {
    List<EbDocument> versions =
        register.findByCycleIdAndDocumentTypeOrderByVersionNoDesc(
            cycle.getId(), registration.documentType());
    if (registration.supersedes()) {
      versions.stream()
          .filter(d -> d.getStatus() == EbDocumentStatus.ACTIVE)
          .forEach(EbDocument::supersede);
    }
    return versions.isEmpty() ? 1 : versions.get(0).getVersionNo() + 1;
  }

  /**
   * Marks the register entry of a stored file rejected (e.g. a rejected BOR).
   *
   * @param attachmentId stored file
   */
  public void reject(Long attachmentId) {
    register.findByAttachmentId(attachmentId).forEach(EbDocument::reject);
  }

  /**
   * The process tag of a cycle's placement documents.
   *
   * @param cycle cycle
   * @return RENEWAL_PLACEMENT or NB_PLACEMENT
   */
  public static String placementProcess(EbCycle cycle) {
    return cycle.getBusinessType() == BusinessType.RENEWAL
        ? EbDocumentTypes.RENEWAL_PLACEMENT
        : EbDocumentTypes.NB_PLACEMENT;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * Documents to upload from the Documents tab.
   *
   * @param documentType document type (EB types of list DOCUMENT_TYPE)
   * @param processType process tag (list EB_PROCESS_TYPE)
   * @param source where they came from; AO when null
   * @param description description, may be null
   * @param files files, at least one
   */
  public record Upload(
      String documentType,
      String processType,
      EbDocumentSource source,
      String description,
      List<UploadedFile> files) {

    /** Defensive copy. */
    public Upload {
      files = files == null ? List.of() : List.copyOf(files);
    }
  }

  /**
   * How stored files are registered.
   *
   * @param documentType document type
   * @param processType process tag
   * @param source where they came from
   * @param supersedes whether the new version supersedes the active one of the type
   * @param description description of the files, may be null
   */
  public record Registration(
      String documentType,
      String processType,
      EbDocumentSource source,
      boolean supersedes,
      String description) {}
}
