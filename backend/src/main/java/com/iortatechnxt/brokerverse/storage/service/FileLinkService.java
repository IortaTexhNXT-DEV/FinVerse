package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.storage.FileStore;
import com.iortatechnxt.brokerverse.common.storage.PresignedLink;
import com.iortatechnxt.brokerverse.common.storage.StorageProperties;
import com.iortatechnxt.brokerverse.storage.domain.ScanStatus;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Duration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues short-lived presigned download links (DOCUMENT_STORAGE_DECISION section 3, option C): the
 * owning module's permission check first ({@link FileAccessPolicy}), then the scan state (only
 * clean files), then the link with {@code Content-Disposition: attachment} and {@code no-store},
 * valid for {@code FILE_LINK_TTL_SECONDS} (default 300). Every issue writes an audit entry with
 * who, what, when and from where, which also covers the archive-inquiry logging of BRD-13; a
 * refused request is audited as well.
 */
@Service
@Transactional
public class FileLinkService {

  /** Business parameter of the link validity in seconds. */
  public static final String TTL_PARAMETER = "FILE_LINK_TTL_SECONDS";

  private static final String ENTITY = StoredFileService.ENTITY;

  private final StoredFileService files;
  private final FileScanService scans;
  private final FileAccessPolicy access;
  private final FileStore store;
  private final SystemParameterService parameters;
  private final StorageProperties properties;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param files stored files
   * @param scans scan results
   * @param access owner permission check
   * @param store object store
   * @param parameters business parameters
   * @param properties storage settings
   * @param audit audit trail
   * @param currentUser current user
   */
  public FileLinkService(
      StoredFileService files,
      FileScanService scans,
      FileAccessPolicy access,
      FileStore store,
      SystemParameterService parameters,
      StorageProperties properties,
      AuditTrailService audit,
      CurrentUser currentUser) {
    this.files = files;
    this.scans = scans;
    this.access = access;
    this.store = store;
    this.parameters = parameters;
    this.properties = properties;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  /**
   * Issues a download link.
   *
   * @param id stored file id
   * @param clientAddress address of the requesting client (audit "from where")
   * @return link and file facts
   */
  public IssuedLink issue(Long id, String clientAddress) {
    StoredFile file = files.get(id);
    String from = clientAddress == null ? "unknown" : clientAddress;
    if (!access.mayRead(file.owner(), file.getDocumentType())) {
      audit.recordIndependently(
          currentUser.username(),
          ENTITY,
          id,
          AuditAction.REJECT,
          "Download link refused (no permission on the owner) from "
              + from
              + ": "
              + FileScanService.describe(file));
      throw new AccessDeniedException("You may not open the files of this record");
    }
    // A pending result is read in its own transaction, so a quarantine is kept even though this
    // request is then refused.
    ScanStatus status =
        file.getScanStatus() == ScanStatus.PENDING ? scans.refresh(id) : file.getScanStatus();
    StoredFileService.requireDownloadable(status);
    Duration ttl = ttl();
    PresignedLink link =
        store.presignedGet(file.objectRef(), ttl, file.getFileName(), file.getContentType());
    audit.record(
        ENTITY,
        id,
        AuditAction.EXPORT,
        "Download link issued (valid "
            + ttl.toSeconds()
            + " s) from "
            + from
            + ": "
            + FileScanService.describe(file));
    return new IssuedLink(file, link);
  }

  /**
   * The validity of links: the parameter {@value #TTL_PARAMETER}, else {@code
   * brokerverse.storage.link-ttl}.
   *
   * @return validity
   */
  @Transactional(readOnly = true)
  public Duration ttl() {
    return Duration.ofSeconds(
        parameters.intValue(TTL_PARAMETER, (int) properties.linkTtl().toSeconds()));
  }

  /**
   * An issued link.
   *
   * @param file the file
   * @param link the link
   */
  public record IssuedLink(StoredFile file, PresignedLink link) {}
}
