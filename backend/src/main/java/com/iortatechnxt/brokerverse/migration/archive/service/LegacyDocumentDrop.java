package com.iortatechnxt.brokerverse.migration.archive.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The legacy documents staged in the console for the document index (object H02): read by the
 * default document source ({@link ArchivePorts}). Each file is kept in file storage under the owner
 * {@code MigDocumentDrop} of its legacy system, with the short-lived migration record class; the
 * archive keeps its own copy as an attachment of the archive record.
 */
@Service
@Transactional
public class LegacyDocumentDrop {

  private final StoredFileService files;

  /**
   * Creates the drop.
   *
   * @param files file storage
   */
  public LegacyDocumentDrop(StoredFileService files) {
    this.files = files;
  }

  /**
   * Stages a legacy document.
   *
   * @param companyId company
   * @param sourceSystem legacy system
   * @param fileName file name as named in the document index
   * @param content bytes
   * @return the stored file
   */
  public StoredFile stage(Long companyId, String sourceSystem, String fileName, byte[] content) {
    if (sourceSystem == null || sourceSystem.isBlank()) {
      throw new BusinessRuleException("MIG_SOURCE_REQUIRED", "Give the legacy system");
    }
    return files.store(
        new StoreRequest(
            owner(companyId, sourceSystem),
            "LEGACY_DOCUMENT",
            MigrationCodes.RECORD_CLASS_EXTRACT,
            fileName,
            content,
            null));
  }

  /**
   * The documents staged for a legacy system.
   *
   * @param companyId company
   * @param sourceSystem legacy system
   * @return files, oldest first
   */
  @Transactional(readOnly = true)
  public List<StoredFile> staged(Long companyId, String sourceSystem) {
    return files.filesOf(owner(companyId, sourceSystem));
  }

  /**
   * The latest staged document of a name.
   *
   * @param companyId company
   * @param sourceSystem legacy system
   * @param fileName file name
   * @return the bytes, empty when not staged
   */
  @Transactional(readOnly = true)
  public Optional<byte[]> read(Long companyId, String sourceSystem, String fileName) {
    return staged(companyId, sourceSystem).stream()
        .filter(f -> f.getFileName().equals(fileName))
        .reduce((first, second) -> second)
        .map(f -> files.read(f.getId()));
  }

  private static FileOwner owner(Long companyId, String sourceSystem) {
    return new FileOwner(
        companyId,
        MigrationCodes.ENTITY_DOCUMENT_DROP,
        sourceSystem.strip().toUpperCase(Locale.ROOT));
  }
}
