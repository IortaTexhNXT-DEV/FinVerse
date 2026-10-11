package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFileInfo;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFileRepository;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DroppedFile;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-system extract repository, the default shared-drive drop (RMTID.001, CMRID.001, OQ17):
 * Operations extracts are kept by folder with their checksum and origin, listed and downloaded on
 * the Interfaces screen. A folder never holds two files with the same name. The content is kept in
 * the file store (owner type {@value #OWNER_TYPE}, record class {@code WORKING_FILE}; build step
 * ST1).
 */
@Service
@Transactional
public class ExtractRepositoryService {

  /** Owner entity type of the stored extract files. */
  public static final String OWNER_TYPE = "ExtractFile";

  /** Record class of extract files. */
  public static final String RECORD_CLASS = "WORKING_FILE";

  private static final String ENTITY = OWNER_TYPE;

  private final ExtractFileRepository files;
  private final StoredFileService storedFiles;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param files files
   * @param storedFiles file store
   * @param audit audit trail
   */
  public ExtractRepositoryService(
      ExtractFileRepository files, StoredFileService storedFiles, AuditTrailService audit) {
    this.files = files;
    this.storedFiles = storedFiles;
    this.audit = audit;
  }

  /**
   * Stores a file.
   *
   * @param companyId company
   * @param location folder and name
   * @param content type and bytes
   * @param origin module and reference
   * @return the stored file
   */
  public DroppedFile store(
      Long companyId,
      ExtractFile.Location location,
      DropContent content,
      ExtractFile.Origin origin) {
    files
        .findByCompanyIdAndFolderAndFileName(companyId, location.folder(), location.fileName())
        .ifPresent(
            f -> {
              throw new DuplicateResourceException(
                  "Extract file", location.folder() + "/" + location.fileName());
            });
    byte[] bytes = content.bytes();
    String sha256 = Sha256.hex(bytes);
    ExtractFile saved =
        files.save(
            new ExtractFile(
                companyId,
                location,
                new ExtractFile.Content(content.contentType(), sha256, bytes),
                origin));
    saved.storedIn(
        storedFiles
            .storeChecked(
                new StoreRequest(
                    new FileOwner(companyId, OWNER_TYPE, String.valueOf(saved.getId())),
                    origin.module(),
                    RECORD_CLASS,
                    location.fileName(),
                    bytes,
                    sha256),
                content.contentType(),
                FileOrigin.GENERATED)
            .getId());
    String path = location.folder() + "/" + location.fileName();
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        "Stored " + path + " (" + bytes.length + " bytes) from " + origin.module());
    return new DroppedFile(saved.getId(), path, sha256);
  }

  /**
   * Files of a company, optionally one folder, without content.
   *
   * @param companyId company
   * @param folder folder, null or blank for all
   * @return files, newest first
   */
  @Transactional(readOnly = true)
  public List<ExtractFileInfo> list(Long companyId, String folder) {
    return folder == null || folder.isBlank()
        ? files.list(companyId)
        : files.list(companyId, folder.strip());
  }

  /**
   * A file for a download endpoint (the caller checked its own permission): a presigned link to the
   * stored file, or the bytes of a file kept before ST1; audited.
   *
   * @param id file
   * @return download
   */
  public FileDownload downloadable(Long id) {
    ExtractFile file = audited(id, "Downloaded ");
    return file.getStoredFileId() == null
        ? FileDownload.inline(file.getFileName(), file.getContentType(), legacyContent(file))
        : FileDownload.stored(file.getStoredFileId());
  }

  /**
   * The content of a file for internal use (e.g. sending it by e-mail); audited.
   *
   * @param id file
   * @return bytes
   */
  public byte[] content(Long id) {
    ExtractFile file = audited(id, "Read ");
    return file.getStoredFileId() == null
        ? legacyContent(file)
        : storedFiles.read(file.getStoredFileId());
  }

  private ExtractFile audited(Long id, String verb) {
    ExtractFile file =
        files.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    audit.record(
        ENTITY, id, AuditAction.EXPORT, verb + file.getFolder() + "/" + file.getFileName());
    return file;
  }

  private static byte[] legacyContent(ExtractFile file) {
    byte[] bytes = file.getContent();
    if (bytes == null || !Sha256.hex(bytes).equals(file.getSha256())) {
      throw new BusinessRuleException(
          "EXTRACT_FILE_INTEGRITY_FAILURE", "The stored file failed its checksum verification");
    }
    return bytes;
  }
}
