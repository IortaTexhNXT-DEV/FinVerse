package com.iortatechnxt.brokerverse.migration.signoff.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoff;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoffRepository;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.domain.StoredFile;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evidence files of the sign-offs (FR-DM-003: "the signer approves or rejects with a comment and
 * optional evidence file"): kept in the file store as general documents of the sign-off.
 */
@Service
@Transactional
public class EvidenceService {

  private static final String RECORD_CLASS = "GENERAL_DOCUMENT";

  private final MigSignoffRepository signoffs;
  private final StoredFileService files;

  /**
   * Creates the service.
   *
   * @param signoffs sign-offs
   * @param files file store
   */
  public EvidenceService(MigSignoffRepository signoffs, StoredFileService files) {
    this.signoffs = signoffs;
    this.files = files;
  }

  /**
   * Attaches the evidence of a sign-off.
   *
   * @param signoffId sign-off
   * @param fileName file name
   * @param content bytes
   * @return the sign-off
   */
  public MigSignoff attach(Long signoffId, String fileName, byte[] content) {
    MigSignoff s = get(signoffId);
    StoredFile f =
        files.store(
            new StoreRequest(
                new FileOwner(
                    s.getCompanyId(), MigrationCodes.ENTITY_SIGNOFF, String.valueOf(s.getId())),
                MigrationCodes.DOC_EVIDENCE,
                RECORD_CLASS,
                fileName,
                content,
                null));
    s.evidence(f.getId(), f.getFileName());
    return s;
  }

  /**
   * The evidence file of a sign-off.
   *
   * @param signoffId sign-off
   * @return download
   */
  @Transactional(readOnly = true)
  public FileDownload download(Long signoffId) {
    MigSignoff s = get(signoffId);
    if (s.getEvidenceFileId() == null) {
      throw new ResourceNotFoundException("Evidence", signoffId);
    }
    return FileDownload.stored(s.getEvidenceFileId());
  }

  private MigSignoff get(Long id) {
    return signoffs
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_SIGNOFF, id));
  }
}
