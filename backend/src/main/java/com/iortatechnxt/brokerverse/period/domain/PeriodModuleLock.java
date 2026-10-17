package com.iortatechnxt.brokerverse.period.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Cut-off of one group of books in a period (FRBS 3.4.0): while locked, postings of the group's
 * source modules into the period are refused, even though the period itself stays open for GL
 * adjustments. The only group today is {@link #BROKING}.
 */
@Entity
@Table(name = "acc_period_module_lock")
public class PeriodModuleLock extends BaseEntity {

  /** The broking books (booking, Operations, cashiering, remittance, disbursement...). */
  public static final String BROKING = "BROKING";

  private static final int MAX_NOTE = 300;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "period_id", nullable = false)
  private Long periodId;

  @Column(nullable = false, length = 30)
  private String module;

  @Column(nullable = false)
  private boolean locked;

  @Column(name = "locked_by", length = 50)
  private String lockedBy;

  @Column(name = "locked_at")
  private Instant lockedAt;

  @Column(name = "unlocked_by", length = 50)
  private String unlockedBy;

  @Column(name = "unlocked_at")
  private Instant unlockedAt;

  @Column(length = MAX_NOTE)
  private String note;

  protected PeriodModuleLock() {}

  /**
   * Creates an unlocked cut-off record.
   *
   * @param companyId company
   * @param periodId period
   * @param module group of books
   */
  public PeriodModuleLock(Long companyId, Long periodId, String module) {
    this.companyId = companyId;
    this.periodId = periodId;
    this.module = module;
  }

  /**
   * Locks the books.
   *
   * @param user acting user
   * @param when time
   * @param note note (e.g. the pending items at cut-off)
   */
  public void lock(String user, Instant when, String note) {
    this.locked = true;
    this.lockedBy = user;
    this.lockedAt = when;
    this.note = limit(note);
  }

  /**
   * Reopens the books.
   *
   * @param user acting user
   * @param when time
   * @param reason reason
   */
  public void unlock(String user, Instant when, String reason) {
    this.locked = false;
    this.unlockedBy = user;
    this.unlockedAt = when;
    this.note = limit(reason);
  }

  private static String limit(String text) {
    return text == null || text.length() <= MAX_NOTE ? text : text.substring(0, MAX_NOTE);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getPeriodId() {
    return periodId;
  }

  public String getModule() {
    return module;
  }

  public boolean isLocked() {
    return locked;
  }

  public String getLockedBy() {
    return lockedBy;
  }

  public Instant getLockedAt() {
    return lockedAt;
  }

  public String getUnlockedBy() {
    return unlockedBy;
  }

  public Instant getUnlockedAt() {
    return unlockedAt;
  }

  public String getNote() {
    return note;
  }
}
