package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.issuance.domain.UploadBatch;
import com.iortatechnxt.brokerverse.issuance.domain.UploadBatchRepository;
import com.iortatechnxt.brokerverse.issuance.domain.UploadItem;
import com.iortatechnxt.brokerverse.issuance.domain.UploadItem.HeldFile;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService.ReceivedFile;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bulk e-policy upload (BRNB.073): many PDFs at once. The account of each file is proposed from the
 * file name convention (the ARN, e.g. {@code ARN-2026-000123_policy.pdf}) or the ARN printed in the
 * document; the user reviews and corrects the matches, then confirms. Each confirmed file is
 * received like a single upload (see {@link EpolicyUploadConfirmation}). The files wait in the file
 * store (owner type {@value #OWNER_TYPE}, the upload; record class {@code INBOUND_FILE}; build step
 * ST1) and are used once their malware scan is clean.
 */
@Service
@Transactional
public class EpolicyUploadService {

  /** Largest number of files in one upload. */
  public static final int MAX_FILES = 50;

  /** Owner entity type of the stored upload files (the upload). */
  public static final String OWNER_TYPE = "EpolicyUpload";

  /** Record class of the upload files. */
  public static final String RECORD_CLASS = "INBOUND_FILE";

  private static final String ENTITY = OWNER_TYPE;
  private static final String PDF = "application/pdf";

  private final UploadBatchRepository batches;
  private final EpolicyMatcher matcher;
  private final StoredFileService storedFiles;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param batches uploads
   * @param matcher account matching
   * @param storedFiles file store (the uploaded files)
   * @param audit audit trail
   */
  public EpolicyUploadService(
      UploadBatchRepository batches,
      EpolicyMatcher matcher,
      StoredFileService storedFiles,
      AuditTrailService audit) {
    this.batches = batches;
    this.matcher = matcher;
    this.storedFiles = storedFiles;
    this.audit = audit;
  }

  /**
   * Holds the files and proposes their accounts.
   *
   * @param companyId company
   * @param files PDF files
   * @return the upload under review
   */
  public UploadBatch upload(Long companyId, List<IncomingFile> files) {
    if (files.isEmpty() || files.size() > MAX_FILES) {
      throw new BusinessRuleException(
          "EPOLICY_FILE_COUNT", "Upload between 1 and " + MAX_FILES + " e-policies at a time");
    }
    UploadBatch batch = new UploadBatch(companyId);
    for (IncomingFile file : files) {
      UploadItem item =
          batch.add(new HeldFile(file.name(), sha256(file.content()), file.content()));
      propose(companyId, item, file);
    }
    UploadBatch saved = batches.save(batch);
    List<UploadItem> items = saved.getItems();
    for (int i = 0; i < items.size(); i++) {
      items.get(i).storedIn(store(saved, items.get(i), files.get(i)));
    }
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        files.size() + " e-policies uploaded for review");
    return saved;
  }

  private Long store(UploadBatch batch, UploadItem item, IncomingFile file) {
    return storedFiles
        .storeChecked(
            new StoreRequest(
                new FileOwner(batch.getCompanyId(), OWNER_TYPE, String.valueOf(batch.getId())),
                null,
                RECORD_CLASS,
                item.getFileName(),
                file.content(),
                item.getSha256()),
            PDF,
            FileOrigin.UPLOADED)
        .getId();
  }

  private void propose(Long companyId, UploadItem item, IncomingFile file) {
    matcher
        .match(new ReceivedFile(companyId, file.name(), file.content(), null, null))
        .ifPresentOrElse(
            m -> item.propose(m.account().getArn(), m.method(), m.account().getClientName()),
            () -> item.propose(null, null, "No account found: choose the ARN"));
  }

  /**
   * Changes the account of a file or leaves it out.
   *
   * @param batchId upload
   * @param itemId file
   * @param arn account chosen, null to keep the proposal
   * @param included whether the file is stored on confirmation
   * @return the upload
   */
  public UploadBatch choose(Long batchId, Long itemId, String arn, boolean included) {
    UploadBatch batch = get(batchId);
    batch.requireReview();
    item(batch, itemId).choose(arn, included);
    return batch;
  }

  /**
   * Marks the upload confirmed and returns the files to store.
   *
   * @param batchId upload
   * @return included items
   */
  public List<Long> markConfirmed(Long batchId) {
    UploadBatch batch = get(batchId);
    batch.confirm();
    audit.record(ENTITY, batchId, AuditAction.AUTHORIZE, "E-policy upload confirmed");
    return batch.getItems().stream().filter(UploadItem::isIncluded).map(UploadItem::getId).toList();
  }

  /**
   * The file of an item as a received file.
   *
   * @param batchId upload
   * @param itemId item
   * @return file with its chosen account
   */
  public ReceivedFile fileOf(Long batchId, Long itemId) {
    UploadBatch batch = get(batchId);
    UploadItem item = item(batch, itemId);
    byte[] content =
        item.getStoredFileId() == null
            ? item.getContent()
            : storedFiles.read(item.getStoredFileId());
    return new ReceivedFile(batch.getCompanyId(), item.getFileName(), content, item.getArn(), null);
  }

  /**
   * Records the outcome of one file.
   *
   * @param batchId upload
   * @param itemId item
   * @param epolicyId e-policy created, null when refused
   * @param outcome outcome
   */
  public void recordOutcome(Long batchId, Long itemId, Long epolicyId, String outcome) {
    item(get(batchId), itemId).done(epolicyId, outcome);
  }

  /**
   * Discards an upload.
   *
   * @param batchId upload
   * @return the upload
   */
  public UploadBatch discard(Long batchId) {
    UploadBatch batch = get(batchId);
    batch.discard();
    audit.record(ENTITY, batchId, AuditAction.DEACTIVATE, "E-policy upload discarded");
    return batch;
  }

  /**
   * One upload with its files.
   *
   * @param id upload
   * @return upload
   */
  @Transactional(readOnly = true)
  public UploadBatch get(Long id) {
    UploadBatch batch =
        batches.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    batch.getItems();
    return batch;
  }

  private static UploadItem item(UploadBatch batch, Long itemId) {
    return batch.getItems().stream()
        .filter(i -> i.getId().equals(itemId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("E-policy upload file", itemId));
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * An uploaded file.
   *
   * @param name file name
   * @param content bytes
   */
  public record IncomingFile(String name, byte[] content) {

    /** Defensive copy. */
    public IncomingFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof IncomingFile f
          && Objects.equals(name, f.name)
          && Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, Arrays.hashCode(content));
    }

    @Override
    public String toString() {
      return name + " (" + content.length + " bytes)";
    }
  }
}
