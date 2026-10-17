package com.iortatechnxt.brokerverse.finreport.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Commentary on one row of a schedule for one month (Appendix A II-45 "variance analysis with
 * commentary", III FS analysis): kept per company, schedule, period ({@code yyyy-MM}) and row key,
 * printed in the schedule's commentary column.
 */
@Entity
@Table(name = "fin_statement_comment")
public class StatementComment extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "schedule_code", nullable = false, length = 40, updatable = false)
  private String scheduleCode;

  @Column(nullable = false, length = 7, updatable = false)
  private String period;

  @Column(name = "row_key", nullable = false, length = 120, updatable = false)
  private String rowKey;

  @Column(name = "comment_text", nullable = false, length = 1000)
  private String text;

  protected StatementComment() {}

  /**
   * A comment.
   *
   * @param key company, schedule, period and row
   * @param text comment
   */
  public StatementComment(CommentKey key, String text) {
    this.companyId = key.companyId();
    this.scheduleCode = key.scheduleCode();
    this.period = key.period();
    this.rowKey = key.rowKey();
    this.text = text;
  }

  /**
   * Changes the comment.
   *
   * @param newText comment
   */
  public void change(String newText) {
    text = newText;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getScheduleCode() {
    return scheduleCode;
  }

  public String getPeriod() {
    return period;
  }

  public String getRowKey() {
    return rowKey;
  }

  public String getText() {
    return text;
  }

  /**
   * Where a comment belongs.
   *
   * @param companyId company
   * @param scheduleCode schedule
   * @param period month {@code yyyy-MM}
   * @param rowKey row
   */
  public record CommentKey(Long companyId, String scheduleCode, String period, String rowKey) {}
}
