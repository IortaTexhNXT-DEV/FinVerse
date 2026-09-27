package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A batch generation or sending of renewal letters (BRRN.010; job {@code RNW_LETTER_BATCH}): the
 * selection, the counts and the messages of the failures.
 */
@Entity
@Table(name = "rnw_letter_batch")
public class LetterBatch extends BaseEntity {

  /** What a batch does. */
  public enum Action {
    /** Generate letters. */
    GENERATE,
    /** Send generated letters. */
    SEND
  }

  private static final int MESSAGE_LENGTH = 2000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "batch_no", nullable = false, length = 30, updatable = false)
  private String batchNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private Action action;

  @Enumerated(EnumType.STRING)
  @Column(name = "letter_type", nullable = false, length = 20, updatable = false)
  private LetterType letterType;

  @Enumerated(EnumType.STRING)
  @Column(length = 10, updatable = false)
  private RaNotice notice;

  @Column(name = "selected_count", nullable = false)
  private int selectedCount;

  @Column(name = "done_count", nullable = false)
  private int doneCount;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  @Column(nullable = false, length = 20)
  private String status = "RUNNING";

  @Column(length = MESSAGE_LENGTH)
  private String message;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  protected LetterBatch() {}

  /**
   * Starts a batch.
   *
   * @param companyId company
   * @param batchNo batch number
   * @param action generate or send
   * @param kind letter type and notice
   * @param selected number of renewals selected
   * @param at start
   */
  public LetterBatch(
      Long companyId, String batchNo, Action action, Kind kind, int selected, Instant at) {
    this.companyId = companyId;
    this.batchNo = batchNo;
    this.action = action;
    this.letterType = kind.type();
    this.notice = kind.notice();
    this.selectedCount = selected;
    this.startedAt = at;
  }

  /**
   * Completes the batch.
   *
   * @param done letters generated or sent
   * @param failed failures
   * @param text failure messages
   * @param at end
   */
  public void complete(int done, int failed, String text, Instant at) {
    this.doneCount = done;
    this.failedCount = failed;
    this.status = "COMPLETED";
    this.message =
        text == null || text.length() <= MESSAGE_LENGTH ? text : text.substring(0, MESSAGE_LENGTH);
    this.endedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public Action getAction() {
    return action;
  }

  public LetterType getLetterType() {
    return letterType;
  }

  public RaNotice getNotice() {
    return notice;
  }

  public int getSelectedCount() {
    return selectedCount;
  }

  public int getDoneCount() {
    return doneCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public String getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  /**
   * Letter type and notice of a batch.
   *
   * @param type letter type
   * @param notice notice of an RA batch, else null
   */
  public record Kind(LetterType type, RaNotice notice) {}
}
