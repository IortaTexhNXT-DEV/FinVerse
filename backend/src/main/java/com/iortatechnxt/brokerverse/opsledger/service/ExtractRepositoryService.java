package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFileInfo;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFileRepository;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DroppedFile;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-system extract repository, the default shared-drive drop (RMTID.001, CMRID.001, OQ17):
 * Operations extracts are kept by folder with their checksum and origin, listed and downloaded on
 * the Interfaces screen. A folder never holds two files with the same name.
 */
@Service
@Transactional
public class ExtractRepositoryService {

  private static final String ENTITY = "ExtractFile";

  private final ExtractFileRepository files;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param files files
   * @param audit audit trail
   */
  public ExtractRepositoryService(ExtractFileRepository files, AuditTrailService audit) {
    this.files = files;
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
   * A file with its content (download is audited).
   *
   * @param id file
   * @return file
   */
  public ExtractFile download(Long id) {
    ExtractFile file =
        files.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    audit.record(
        ENTITY,
        id,
        AuditAction.EXPORT,
        "Downloaded " + file.getFolder() + "/" + file.getFileName());
    return file;
  }
}
