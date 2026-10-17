package com.iortatechnxt.brokerverse.attachment.service;

import com.iortatechnxt.brokerverse.attachment.domain.AllowedFileType;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentContentRepository;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentRepository;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.domain.StoredFile;
import com.iortatechnxt.brokerverse.attachment.service.VirusScanner.ScanVerdict;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Document attachments for any record: upload with size, type, signature and malware checks,
 * SHA-256 checksum, download with integrity verification, and logical deletion. Every action is
 * audited against the owning record.
 *
 * <p>The content is kept in the file store through {@link StoredFileService} (owner type {@value
 * #OWNER_TYPE}, the attachment id; build step ST1). Files attached before ST1 are read from {@code
 * doc_attachment_content} until {@code FILE_BYTEA_MIGRATION} has copied them.
 */
@Service
@Transactional
@EnableConfigurationProperties(AttachmentProperties.class)
public class AttachmentService {

  /** Owner entity type of the stored files of attachments. */
  public static final String OWNER_TYPE = "Attachment";

  private static final String ENTITY = OWNER_TYPE;
  private static final Pattern ENTITY_TYPE = Pattern.compile("[A-Za-z][A-Za-z0-9_]{1,59}");
  private static final Pattern ENTITY_ID = Pattern.compile("[A-Za-z0-9_.:/-]{1,60}");
  private static final Pattern UNSAFE_NAME_CHARS = Pattern.compile("[\\p{Cntrl}\"\\\\/:*?<>|]");
  private static final int MAX_NAME = 255;

  private final AttachmentRepository attachments;
  private final AttachmentContentRepository contents;
  private final StoredFileService storedFiles;
  private final List<VirusScanner> scanners;
  private final AttachmentProperties properties;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param attachments metadata repository
   * @param contents content repository (files attached before ST1)
   * @param storedFiles file store
   * @param scanners malware scanners
   * @param properties settings
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AttachmentService(
      AttachmentRepository attachments,
      AttachmentContentRepository contents,
      StoredFileService storedFiles,
      List<VirusScanner> scanners,
      AttachmentProperties properties,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.attachments = attachments;
    this.contents = contents;
    this.storedFiles = storedFiles;
    this.scanners = List.copyOf(scanners);
    this.properties = properties;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists the live attachments of a record.
   *
   * @param target record
   * @return attachments, oldest first
   */
  @Transactional(readOnly = true)
  public List<Attachment> list(AttachmentTarget target) {
    requireValidTarget(target);
    return attachments.findByEntityTypeAndEntityIdAndDeletedFalseOrderByCreatedAtAsc(
        target.entityType(), target.entityId());
  }

  /**
   * Rejects files above the configured size before they are read into memory.
   *
   * @param sizeBytes declared size
   */
  public void requireWithinLimit(long sizeBytes) {
    if (sizeBytes <= 0) {
      throw new BusinessRuleException("ATTACHMENT_EMPTY", "The file is empty");
    }
    if (sizeBytes > properties.maxSize().toBytes()) {
      throw new BusinessRuleException(
          "ATTACHMENT_TOO_LARGE",
          "The file exceeds the maximum size of " + properties.maxSize().toMegabytes() + " MB");
    }
  }

  /**
   * Stores a new attachment.
   *
   * @param target record the file belongs to
   * @param originalName file name as uploaded
   * @param content file bytes
   * @param description optional description
   * @return saved metadata
   */
  public Attachment upload(
      AttachmentTarget target, String originalName, byte[] content, String description) {
    return upload(target, originalName, content, description, null);
  }

  /**
   * Stores a new attachment of a document type, which sets its record class ({@link
   * AttachmentRecordClasses}).
   *
   * @param target record the file belongs to
   * @param originalName file name as uploaded
   * @param content file bytes
   * @param description optional description
   * @param documentType document type, null when unclassified
   * @return saved metadata
   */
  public Attachment upload(
      AttachmentTarget target,
      String originalName,
      byte[] content,
      String description,
      String documentType) {
    requireValidTarget(target);
    requireWithinLimit(content.length);
    String fileName = sanitize(originalName);
    AllowedFileType type =
        AllowedFileType.fromFileName(fileName)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "ATTACHMENT_TYPE_NOT_ALLOWED",
                        "Only these file types are allowed: "
                            + AllowedFileType.allowedExtensions()));
    if (!type.matches(content)) {
      throw new BusinessRuleException(
          "ATTACHMENT_CONTENT_MISMATCH", "The file content does not match its ." + type + " type");
    }
    scan(fileName, content);
    StoredFile file = new StoredFile(fileName, type.mimeType(), content.length, sha256(content));
    Attachment saved = attachments.save(new Attachment(target, file, blankToNull(description)));
    saved.storedIn(
        storedFiles
            .storeChecked(
                new StoreRequest(
                    new FileOwner(null, OWNER_TYPE, String.valueOf(saved.getId())),
                    documentType,
                    AttachmentRecordClasses.of(documentType),
                    fileName,
                    content,
                    file.sha256()),
                type.mimeType(),
                FileOrigin.UPLOADED)
            .getId());
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        "Attached "
            + fileName
            + " ("
            + content.length
            + " bytes, SHA-256 "
            + file.sha256()
            + ") to "
            + target.entityType()
            + " "
            + target.entityId());
    return saved;
  }

  /**
   * Gets live attachment metadata.
   *
   * @param id id
   * @return metadata
   */
  @Transactional(readOnly = true)
  public Attachment get(Long id) {
    return attachments
        .findById(id)
        .filter(a -> !a.isDeleted())
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Reads a file after verifying its checksum (streamed flows: ZIP files, e-mail attachments,
   * feeds); the read is audited.
   *
   * @param id id
   * @return metadata and bytes
   */
  public AttachmentFile download(Long id) {
    Attachment attachment = get(id);
    byte[] bytes =
        attachment.getStoredFileId() == null
            ? legacyContent(attachment)
            : storedFiles.read(attachment.getStoredFileId());
    auditDownload(attachment);
    return new AttachmentFile(attachment, bytes);
  }

  /**
   * A file for the download endpoint: the stored file (answered with a presigned link) or, for a
   * file attached before ST1 and not yet copied, its checksum-verified bytes; audited.
   *
   * @param id id
   * @return download
   */
  public FileDownload downloadable(Long id) {
    Attachment attachment = get(id);
    FileDownload download =
        attachment.getStoredFileId() == null
            ? FileDownload.inline(
                attachment.getFileName(), attachment.getContentType(), legacyContent(attachment))
            : FileDownload.stored(attachment.getStoredFileId());
    auditDownload(attachment);
    return download;
  }

  private byte[] legacyContent(Attachment attachment) {
    Long id = attachment.getId();
    byte[] bytes =
        contents
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attachment content", id))
            .getContent();
    if (!sha256(bytes).equals(attachment.getSha256())) {
      throw new BusinessRuleException(
          "ATTACHMENT_INTEGRITY_FAILURE", "Stored file failed its checksum verification");
    }
    return bytes;
  }

  private void auditDownload(Attachment attachment) {
    audit.record(
        ENTITY,
        attachment.getId(),
        AuditAction.EXPORT,
        "Downloaded "
            + attachment.getFileName()
            + " of "
            + attachment.getEntityType()
            + " "
            + attachment.getEntityId());
  }

  /**
   * Removes an attachment logically (retained for audit).
   *
   * @param id id
   */
  public void delete(Long id) {
    Attachment attachment = get(id);
    attachment.markDeleted(currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        id,
        AuditAction.DEACTIVATE,
        "Removed "
            + attachment.getFileName()
            + " from "
            + attachment.getEntityType()
            + " "
            + attachment.getEntityId());
  }

  /**
   * Configured maximum size.
   *
   * @return bytes
   */
  public long maxSizeBytes() {
    return properties.maxSize().toBytes();
  }

  private void scan(String fileName, byte[] content) {
    for (VirusScanner scanner : scanners) {
      ScanVerdict verdict = scanner.scan(fileName, content);
      if (!verdict.clean()) {
        throw new BusinessRuleException(
            "ATTACHMENT_INFECTED",
            "The file was rejected by the malware scan: " + verdict.detail());
      }
    }
  }

  private static void requireValidTarget(AttachmentTarget target) {
    if (target.entityType() == null
        || !ENTITY_TYPE.matcher(target.entityType()).matches()
        || target.entityId() == null
        || !ENTITY_ID.matcher(target.entityId()).matches()) {
      throw new BusinessRuleException("INVALID_ATTACHMENT_TARGET", "Invalid entity type or id");
    }
  }

  /**
   * Keeps only the last path segment and removes characters unsafe in file names and headers.
   *
   * @param name original name
   * @return safe name
   */
  static String sanitize(String name) {
    String base = name == null ? "" : name;
    int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
    String clean = UNSAFE_NAME_CHARS.matcher(base.substring(slash + 1)).replaceAll("_").strip();
    if (clean.isEmpty() || clean.startsWith(".")) {
      clean = "file" + clean;
    }
    return clean.length() > MAX_NAME ? clean.substring(clean.length() - MAX_NAME) : clean;
  }

  /**
   * Computes a SHA-256 checksum.
   *
   * @param content bytes
   * @return lower-case hex digest
   */
  static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * Downloaded file.
   *
   * @param metadata attachment metadata
   * @param content bytes
   */
  public record AttachmentFile(Attachment metadata, byte[] content) {}
}
