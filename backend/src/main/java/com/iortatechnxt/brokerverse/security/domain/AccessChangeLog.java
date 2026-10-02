package com.iortatechnxt.brokerverse.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One changed attribute of a user or role: from, to, the request that asked for it, who did it and
 * who approved it (BRD 4.003.1, reports A-D; UAM-NFR-41). Insert-only: a database trigger rejects
 * update, delete and truncate (V1061), as for the audit trail.
 */
@Entity
@Table(name = "sec_access_change_log")
public class AccessChangeLog {

  private static final int MAX_VALUE = 2000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "subject_type", nullable = false, updatable = false, length = 20)
  private AccessSubjectType subjectType;

  @Column(nullable = false, updatable = false, length = 50)
  private String subject;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 30)
  private AccessChangeActivity activity;

  @Column(nullable = false, updatable = false, length = 40)
  private String attribute;

  @Column(name = "from_value", updatable = false, length = MAX_VALUE)
  private String fromValue;

  @Column(name = "to_value", updatable = false, length = MAX_VALUE)
  private String toValue;

  @Column(name = "request_no", updatable = false, length = 40)
  private String requestNo;

  @Column(name = "done_by", nullable = false, updatable = false, length = 50)
  private String doneBy;

  @Column(name = "approved_by", updatable = false, length = 50)
  private String approvedBy;

  protected AccessChangeLog() {}

  /**
   * Records one changed attribute.
   *
   * @param change subject, activity, attribute and values
   * @param source request number, actor and approver
   * @param occurredAt time
   */
  public AccessChangeLog(AccessChange change, AccessChangeSource source, Instant occurredAt) {
    this.occurredAt = occurredAt;
    this.subjectType = change.subjectType();
    this.subject = change.subject();
    this.activity = change.activity();
    this.attribute = change.attribute();
    this.fromValue = clip(change.from());
    this.toValue = clip(change.to());
    this.requestNo = source.requestNo();
    this.doneBy = source.doneBy();
    this.approvedBy = source.approvedBy();
  }

  private static String clip(String value) {
    return value == null || value.length() <= MAX_VALUE ? value : value.substring(0, MAX_VALUE);
  }

  public Long getId() {
    return id;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public AccessSubjectType getSubjectType() {
    return subjectType;
  }

  public String getSubject() {
    return subject;
  }

  public AccessChangeActivity getActivity() {
    return activity;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getFromValue() {
    return fromValue;
  }

  public String getToValue() {
    return toValue;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public String getDoneBy() {
    return doneBy;
  }

  public String getApprovedBy() {
    return approvedBy;
  }
}
