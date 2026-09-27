package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Legacy status of the Excel masterlists mapped to a status and bucket (BRIDSP-33). */
@Entity
@Table(name = "sbm_status_map")
public class SbmStatusMap extends BaseEntity {

  @Column(name = "legacy_status", nullable = false, length = 60)
  private String legacyStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SbmPolicyStatus status;

  @Column(length = 40)
  private String bucket;

  protected SbmStatusMap() {}

  /**
   * A mapping.
   *
   * @param legacyStatus legacy status (upper case)
   * @param status status
   * @param bucket bucket, may be null
   */
  public SbmStatusMap(String legacyStatus, SbmPolicyStatus status, String bucket) {
    this.legacyStatus = legacyStatus;
    maintain(status, bucket);
  }

  /**
   * Changes the mapping.
   *
   * @param newStatus status
   * @param newBucket bucket, may be null
   */
  public void maintain(SbmPolicyStatus newStatus, String newBucket) {
    this.status = newStatus;
    this.bucket = newBucket;
  }

  public String getLegacyStatus() {
    return legacyStatus;
  }

  public SbmPolicyStatus getStatus() {
    return status;
  }

  public String getBucket() {
    return bucket;
  }
}
