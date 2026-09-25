package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A list record that failed ingestion, with its line, raw record and reason (SNSRP-202). The digest
 * job e-mails the records not yet digested and stamps them.
 */
@Entity
@Table(name = "scr_ingestion_error")
public class IngestionError extends BaseEntity {

  private static final int MAX_RAW = 4000;
  private static final int MAX_REASON = 1000;

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(name = "raw_record", length = MAX_RAW, updatable = false)
  private String rawRecord;

  @Column(name = "reason", nullable = false, length = MAX_REASON, updatable = false)
  private String reason;

  @Column(name = "digested_at")
  private Instant digestedAt;

  /** For JPA. */
  protected IngestionError() {}

  /**
   * Records a failed record.
   *
   * @param runId run
   * @param lineNo line in the file (header = 1)
   * @param rawRecord the record as read
   * @param reason why it failed
   */
  public IngestionError(Long runId, int lineNo, String rawRecord, String reason) {
    this.runId = runId;
    this.lineNo = lineNo;
    this.rawRecord = cut(rawRecord, MAX_RAW);
    this.reason = cut(reason, MAX_REASON);
  }

  private static String cut(String value, int max) {
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }

  /**
   * Stamps the record as sent in a digest.
   *
   * @param when time
   */
  public void digested(Instant when) {
    this.digestedAt = when;
  }

  public Long getRunId() {
    return runId;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getRawRecord() {
    return rawRecord;
  }

  public String getReason() {
    return reason;
  }

  public Instant getDigestedAt() {
    return digestedAt;
  }
}
