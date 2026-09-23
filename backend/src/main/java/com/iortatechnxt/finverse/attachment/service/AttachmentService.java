package com.iortatechnxt.finverse.attachment.service;

import com.iortatechnxt.finverse.attachment.domain.AllowedFileType;
import com.iortatechnxt.finverse.attachment.domain.Attachment;
import com.iortatechnxt.finverse.attachment.domain.AttachmentContent;
import com.iortatechnxt.finverse.attachment.domain.AttachmentContentRepository;
import com.iortatechnxt.finverse.attachment.domain.AttachmentRepository;
import com.iortatechnxt.finverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.finverse.attachment.domain.StoredFile;
import com.iortatechnxt.finverse.attachment.service.VirusScanner.ScanVerdict;
import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
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
 */
@Service
@Transactional
@EnableConfigurationProperties(AttachmentProperties.class)
public class AttachmentService {

  private static final String ENTITY = "Attachment";
  private static final Pattern ENTITY_TYPE = Pattern.compile("[A-Za-z][A-Za-z0-9_]{1,59}");
  private static final Pattern ENTITY_ID = Pattern.compile("[A-Za-z0-9_.:/-]{1,60}");
  private static final Pattern UNSAFE_NAME_CHARS = Pattern.compile("[\\p{Cntrl}\"\\\\/:*?<>|]");
  private static final int MAX_NAME = 255;

  private final AttachmentRepository attachments;
  private final AttachmentContentRepository contents;
  private final List<VirusScanner> scanners;
  private final AttachmentProperties properties;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param attachments metadata repository
   * @param contents content repository
   * @param scanners malware scanners
   * @param properties settings
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AttachmentService(
      AttachmentRepository attachments,
      AttachmentContentRepository contents,
      List<VirusScanner> scanners,
      AttachmentProperties properties,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.attachments = attachments;
    this.contents = contents;
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
    contents.save(new AttachmentContent(saved.getId(), content));
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
   * Downloads a file after verifying its checksum; the download is audited.
   *
   * @param id id
   * @return metadata and bytes
   */
  public AttachmentFile download(Long id) {
    Attachment attachment = get(id);
    byte[] bytes =
        contents
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Attachment content", id))
            .getContent();
    if (!sha256(bytes).equals(attachment.getSha256())) {
      throw new BusinessRuleException(
          "ATTACHMENT_INTEGRITY_FAILURE", "Stored file failed its checksum verification");
    }
    audit.record(
        ENTITY,
        id,
        AuditAction.EXPORT,
        "Downloaded "
            + attachment.getFileName()
            + " of "
            + attachment.getEntityType()
            + " "
            + attachment.getEntityId());
    return new AttachmentFile(attachment, bytes);
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
