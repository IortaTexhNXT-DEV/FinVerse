package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A controlled override (BRD 1.011; BRRN.023/031/035): explicit, with a reason and mandatory
 * remarks, logged with the user and time. An active check override lets the renewal be posted, get
 * its Renewal Advice or be accepted although the check fails; it never makes the bucket Clean.
 */
@Entity
@Table(name = "rnw_override")
public class RenewalOverride extends BaseEntity {

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private OverrideKind kind;

  @Column(name = "check_code", length = 40, updatable = false)
  private String checkCode;

  @Column(name = "from_value", length = 60, updatable = false)
  private String fromValue;

  @Column(name = "to_value", length = 60, updatable = false)
  private String toValue;

  @Column(name = "reason_code", nullable = false, length = 40, updatable = false)
  private String reasonCode;

  @Column(nullable = false, length = 200, updatable = false)
  private String remarks;

  @Column(nullable = false)
  private boolean active = true;

  protected RenewalOverride() {}

  /**
   * Records an override.
   *
   * @param candidateId candidate
   * @param kind kind
   * @param checkCode check overridden, null when not a check
   * @param change from and to values
   * @param reason reason code and remarks
   */
  public RenewalOverride(
      Long candidateId, OverrideKind kind, String checkCode, Change change, Reason reason) {
    this.candidateId = candidateId;
    this.kind = kind;
    this.checkCode = checkCode;
    this.fromValue = change.from();
    this.toValue = change.to();
    this.reasonCode = reason.code();
    this.remarks = reason.remarks();
  }

  /** Ends a check override (the check passed or a new renewal term started). */
  public void end() {
    this.active = false;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public OverrideKind getKind() {
    return kind;
  }

  public String getCheckCode() {
    return checkCode;
  }

  public String getFromValue() {
    return fromValue;
  }

  public String getToValue() {
    return toValue;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * From and to values of an override.
   *
   * @param from previous value
   * @param to new value
   */
  public record Change(String from, String to) {}

  /**
   * Why an override was made.
   *
   * @param code reason (list RNW_OVERRIDE_REASON)
   * @param remarks remarks
   */
  public record Reason(String code, String remarks) {}
}
