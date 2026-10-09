package com.iortatechnxt.brokerverse.productmaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A file of product master changes received by the simulated receiving system (SIT and UAT). */
@Entity
@Table(name = "pm_master_sim_inbox")
public class MasterInboxFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "file_name", nullable = false, length = 120, updatable = false)
  private String fileName;

  @Column(nullable = false, columnDefinition = "text", updatable = false)
  private String content;

  @Column(name = "record_count", nullable = false, updatable = false)
  private int recordCount;

  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  /** For JPA. */
  protected MasterInboxFile() {}

  /**
   * A received file.
   *
   * @param fileName file name
   * @param content content
   * @param recordCount detail records
   * @param receivedAt when
   */
  public MasterInboxFile(String fileName, String content, int recordCount, Instant receivedAt) {
    this.fileName = fileName;
    this.content = content;
    this.recordCount = recordCount;
    this.receivedAt = receivedAt;
  }

  public Long getId() {
    return id;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContent() {
    return content;
  }

  public int getRecordCount() {
    return recordCount;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}
