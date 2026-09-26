package com.iortatechnxt.brokerverse.brokerclaims.diary.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A diary entry of a claim (BRCLM.022, NFR 15.08; FR-CL-052): a call, e-mail, meeting, note or
 * follow-up with its date, optional due date and assignee, and the text; marked done when completed.
 * Entries are never deleted: a wrong entry is marked done with a remark. Closed claims accept
 * diary entries.
 */
@Entity
@Table(name = "bcl_diary_entry")
public class DiaryEntry extends BaseEntity {

  @Column(name = "claim_id", nullable = false, updatable = false)
  private Long claimId;

  @Column(name = "entry_type", nullable = false, length = 40, updatable = false)
  private String entryType;

  @Column(name = "entry_date", nullable = false, updatable = false)
  private LocalDate entryDate;

  @Column(name = "due_date", updatable = false)
  private LocalDate dueDate;

  @Column(length = 50, updatable = false)
  private String assignee;

  @Column(nullable = false, length = 2000, updatable = false)
  private String text;

  @Column(name = "done_at")
  private Instant doneAt;

  @Column(name = "done_by", length = 50)
  private String doneBy;

  @Column(name = "done_remark", length = 500)
  private String doneRemark;

  protected DiaryEntry() {}

  /**
   * Adds an entry.
   *
   * @param claimId claim
   * @param entryType type ({@code BCL_DIARY_TYPE})
   * @param dates entry date and optional due date
   * @param assignee user who acts on it, may be null
   * @param text text
   */
  public DiaryEntry(Long claimId, String entryType, Dates dates, String assignee, String text) {
    this.claimId = claimId;
    this.entryType = entryType;
    this.entryDate = dates.entryDate();
    this.dueDate = dates.dueDate();
    this.assignee = assignee;
    this.text = text;
  }

  /**
   * Marks the entry done.
   *
   * @param by user
   * @param at time
   * @param remark remark, may be null
   */
  public void markDone(String by, Instant at, String remark) {
    this.doneBy = by;
    this.doneAt = at;
    this.doneRemark = remark;
  }

  /**
   * Whether the entry is completed.
   *
   * @return true when done
   */
  public boolean isDone() {
    return doneAt != null;
  }

  public Long getClaimId() {
    return claimId;
  }

  public String getEntryType() {
    return entryType;
  }

  public LocalDate getEntryDate() {
    return entryDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getAssignee() {
    return assignee;
  }

  public String getText() {
    return text;
  }

  public Instant getDoneAt() {
    return doneAt;
  }

  public String getDoneBy() {
    return doneBy;
  }

  public String getDoneRemark() {
    return doneRemark;
  }

  /**
   * The dates of an entry.
   *
   * @param entryDate date of the activity
   * @param dueDate due date, on or after the entry date, may be null
   */
  public record Dates(LocalDate entryDate, LocalDate dueDate) {}
}
