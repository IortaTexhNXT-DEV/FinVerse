package com.iortatechnxt.brokerverse.attachment.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentLink;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentLinkRepository;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentRepository;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService.AttachmentFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Broking document features on top of {@link AttachmentService} (BRNB.026/055/056): document types,
 * several files in one upload, inherited or nominated file names, one file linked to several
 * records (no copy: the file, its checksum and audit trail stay unique) and ZIP download. Lists,
 * downloads and ZIP files apply the document access classes (BRID-025, {@link
 * DocumentAccessPolicy}); uploads and links may carry a process tag.
 */
@Service
@Transactional
public class DocumentService {

  /** Largest number of files in one upload or ZIP download. */
  public static final int MAX_FILES = 50;

  private static final String DOCUMENT_TYPE = "DOCUMENT_TYPE";
  private static final String ENTITY = "Attachment";

  private final AttachmentService attachments;
  private final AttachmentRepository repository;
  private final AttachmentLinkRepository links;
  private final DocumentNamingService naming;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final DocumentAccessPolicy access;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param attachments attachment storage
   * @param repository attachment metadata
   * @param links links to further records
   * @param naming file naming
   * @param lovs lists of values (document types)
   * @param audit audit trail
   * @param access document access classes (BRID-025)
   * @param clock clock
   */
  public DocumentService(
      AttachmentService attachments,
      AttachmentRepository repository,
      AttachmentLinkRepository links,
      DocumentNamingService naming,
      LovService lovs,
      AuditTrailService audit,
      DocumentAccessPolicy access,
      Clock clock) {
    this.attachments = attachments;
    this.repository = repository;
    this.links = links;
    this.naming = naming;
    this.lovs = lovs;
    this.audit = audit;
    this.access = access;
    this.clock = clock;
  }

  /**
   * Documents of a record the current user may see: its own files and the files linked to it,
   * oldest first, without the documents whose access class excludes the user (BRID-025).
   *
   * @param target record
   * @return attachments
   */
  @Transactional(readOnly = true)
  public List<Attachment> list(AttachmentTarget target) {
    return access.visible(all(target));
  }

  /**
   * Every document of a record whatever the access classes (mandatory-document checks, naming).
   *
   * @param target record
   * @return attachments, oldest first
   */
  @Transactional(readOnly = true)
  public List<Attachment> all(AttachmentTarget target) {
    List<Attachment> own = attachments.list(target);
    List<Long> linkedIds =
        links.findByEntityTypeAndEntityId(target.entityType(), target.entityId()).stream()
            .map(AttachmentLink::getAttachmentId)
            .toList();
    if (linkedIds.isEmpty()) {
      return own;
    }
    return Stream.concat(
            own.stream(),
            repository.findByIdInAndDeletedFalseOrderByCreatedAtAsc(linkedIds).stream())
        .sorted(Comparator.comparing(Attachment::getCreatedAt))
        .toList();
  }

  /**
   * Document types present on a record (own and linked files), e.g. to check mandatory documents.
   *
   * @param target record
   * @return document type codes
   */
  @Transactional(readOnly = true)
  public Set<String> documentTypesOf(AttachmentTarget target) {
    return all(target).stream()
        .map(Attachment::getDocumentType)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * Uploads one or more files to a record with a document type and inherited or nominated names.
   *
   * @param target record
   * @param files files
   * @param options document type, naming and description
   * @return saved attachments in upload order
   */
  public List<Attachment> upload(
      AttachmentTarget target, List<UploadedFile> files, UploadOptions options) {
    String type = requireValid(files, options);
    int sequence =
        (int) all(target).stream().filter(a -> Objects.equals(type, a.getDocumentType())).count();
    List<Attachment> saved = new ArrayList<>();
    for (UploadedFile file : files) {
      String name =
          options.nominate()
              ? naming.nominate(options.reference(), type, ++sequence, file.name())
              : file.name();
      Attachment a = attachments.upload(target, name, file.content(), options.description());
      a.classify(type);
      a.tagProcess(blankToNull(options.processTag()));
      saved.add(a);
    }
    return saved;
  }

  private String requireValid(List<UploadedFile> files, UploadOptions options) {
    if (files.isEmpty() || files.size() > MAX_FILES) {
      throw new BusinessRuleException(
          "DOCUMENT_FILE_COUNT", "Upload between 1 and " + MAX_FILES + " files at a time");
    }
    String type = blankToNull(options.documentType());
    if (type != null) {
      lovs.requireValid(DOCUMENT_TYPE, type, LocalDate.now(clock));
    }
    if (options.nominate() && blankToNull(options.reference()) == null) {
      throw new BusinessRuleException(
          "DOCUMENT_REFERENCE_REQUIRED", "A reference is needed to nominate the file names");
    }
    return type;
  }

  /**
   * Links an uploaded file to further records (e.g. one IDF for several accounts).
   *
   * @param attachmentId file
   * @param targets records to link
   * @return the file
   */
  public Attachment link(Long attachmentId, List<AttachmentTarget> targets) {
    return link(attachmentId, targets, null);
  }

  /**
   * Links an uploaded file to further records in the context of a process (BRID-025: the link
   * carries the process tag, e.g. an EB renewal placement).
   *
   * @param attachmentId file
   * @param targets records to link
   * @param processTag process code of the owning module, null when none
   * @return the file
   */
  public Attachment link(Long attachmentId, List<AttachmentTarget> targets, String processTag) {
    Attachment file = attachments.get(attachmentId);
    for (AttachmentTarget target : targets) {
      boolean own =
          target.entityType().equals(file.getEntityType())
              && target.entityId().equals(file.getEntityId());
      if (own
          || links.existsByAttachmentIdAndEntityTypeAndEntityId(
              attachmentId, target.entityType(), target.entityId())) {
        continue;
      }
      attachments.list(target); // validates the target's type and id
      links.save(new AttachmentLink(attachmentId, target, blankToNull(processTag)));
      audit.record(
          ENTITY,
          attachmentId,
          AuditAction.UPDATE,
          "Linked " + file.getFileName() + " to " + target.entityType() + " " + target.entityId());
    }
    return file;
  }

  /**
   * Removes a document from a record: a linked file is unlinked; the record's own file is removed
   * logically (and disappears from every record it was linked to).
   *
   * @param attachmentId file
   * @param target record the user removes it from
   */
  public void remove(Long attachmentId, AttachmentTarget target) {
    Attachment file = attachments.get(attachmentId);
    boolean own =
        target == null
            || target.entityType().equals(file.getEntityType())
                && target.entityId().equals(file.getEntityId());
    if (own) {
      attachments.delete(attachmentId);
      return;
    }
    links.findByAttachmentId(attachmentId).stream()
        .filter(
            l ->
                l.getEntityType().equals(target.entityType())
                    && l.getEntityId().equals(target.entityId()))
        .forEach(
            l -> {
              links.delete(l);
              audit.record(
                  ENTITY,
                  attachmentId,
                  AuditAction.DEACTIVATE,
                  "Unlinked "
                      + file.getFileName()
                      + " from "
                      + target.entityType()
                      + " "
                      + target.entityId());
            });
  }

  /**
   * Downloads one document the current user may see (access classes, BRID-025); the file is
   * checksum-verified and its download audited.
   *
   * @param id attachment id
   * @return metadata and bytes
   */
  public AttachmentFile download(Long id) {
    access.requireView(attachments.get(id));
    return attachments.download(id);
  }

  /**
   * Several documents in one ZIP file (BRNB.056); each file is checksum-verified and its download
   * audited. A document the user may not see refuses the whole ZIP (BRID-025).
   *
   * @param ids attachment ids
   * @return ZIP bytes
   */
  public byte[] zip(List<Long> ids) {
    if (ids.isEmpty() || ids.size() > MAX_FILES) {
      throw new BusinessRuleException(
          "DOCUMENT_FILE_COUNT", "Select between 1 and " + MAX_FILES + " documents");
    }
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    Set<String> names = new HashSet<>();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      for (Long id : ids) {
        AttachmentFile file = download(id);
        zip.putNextEntry(new ZipEntry(uniqueName(names, file.metadata().getFileName())));
        zip.write(file.content());
        zip.closeEntry();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not build the ZIP file", e);
    }
    return bytes.toByteArray();
  }

  private static String uniqueName(Set<String> taken, String name) {
    String candidate = name;
    int n = 1;
    while (!taken.add(candidate)) {
      int dot = name.lastIndexOf('.');
      candidate =
          dot < 0
              ? name + " (" + n + ")"
              : name.substring(0, dot) + " (" + n + ")" + name.substring(dot);
      n++;
    }
    return candidate;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * An uploaded file.
   *
   * @param name original file name
   * @param content bytes
   */
  public record UploadedFile(String name, byte[] content) {}

  /**
   * Upload options.
   *
   * @param documentType document type (list DOCUMENT_TYPE), may be empty
   * @param nominate rename the files with the naming syntax
   * @param reference business reference used in nominated names (ARN...)
   * @param description description of the files
   * @param processTag process the documents belong to (BRID-025), may be empty
   */
  public record UploadOptions(
      String documentType,
      boolean nominate,
      String reference,
      String description,
      String processTag) {

    /**
     * Options without a process tag.
     *
     * @param documentType document type, may be empty
     * @param nominate rename the files with the naming syntax
     * @param reference business reference used in nominated names
     * @param description description of the files
     */
    public UploadOptions(
        String documentType, boolean nominate, String reference, String description) {
      this(documentType, nominate, reference, description, null);
    }
  }
}
