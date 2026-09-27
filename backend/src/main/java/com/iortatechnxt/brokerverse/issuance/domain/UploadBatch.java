package com.iortatechnxt.brokerverse.issuance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * A bulk e-policy upload (BRNB.073): the files are held with the account proposed for each (ARN in
 * the file name or in the document) until the user reviews the matches and confirms.
 */
@Entity
@Table(name = "iss_upload_batch")
public class UploadBatch extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UploadStatus status = UploadStatus.REVIEW;

  @Column(name = "file_count", nullable = false)
  private int fileCount;

  @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<UploadItem> items = new ArrayList<>();

  protected UploadBatch() {}

  /**
   * Creates an empty batch.
   *
   * @param companyId company
   */
  public UploadBatch(Long companyId) {
    this.companyId = companyId;
  }

  /**
   * Adds a file.
   *
   * @param file file name, checksum and content
   * @return the item
   */
  public UploadItem add(UploadItem.HeldFile file) {
    UploadItem item = new UploadItem(this, items.size() + 1, file);
    items.add(item);
    fileCount = items.size();
    return item;
  }

  /** Refuses changes once the batch left review. */
  public void requireReview() {
    if (status != UploadStatus.REVIEW) {
      throw new BusinessRuleException("EPOLICY_UPLOAD_CLOSED", "This upload is " + status);
    }
  }

  /** Marks the batch confirmed. */
  public void confirm() {
    requireReview();
    status = UploadStatus.CONFIRMED;
  }

  /** Discards the batch. */
  public void discard() {
    requireReview();
    status = UploadStatus.DISCARDED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public UploadStatus getStatus() {
    return status;
  }

  public int getFileCount() {
    return fileCount;
  }

  public List<UploadItem> getItems() {
    return List.copyOf(items);
  }
}
