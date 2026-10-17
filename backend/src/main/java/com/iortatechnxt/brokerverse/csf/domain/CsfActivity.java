package com.iortatechnxt.brokerverse.csf.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One action of an agent in the Customer Servicing Facility (FR-CSF-042): searches with their
 * criteria, views, downloads, resends, uploads, verifications, contact changes and referrals.
 * Append-only; the log cannot be switched off.
 */
@Entity
@Table(name = "csf_activity")
public class CsfActivity extends BaseEntity {

  private static final int MAX_REFERENCE = 120;
  private static final int MAX_DETAIL = 500;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 50, updatable = false)
  private String agent;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private ActivityAction action;

  @Column(name = "client_id", updatable = false)
  private Long clientId;

  @Column(name = "client_code", length = 30, updatable = false)
  private String clientCode;

  @Column(length = MAX_REFERENCE, updatable = false)
  private String reference;

  @Column(length = MAX_DETAIL, updatable = false)
  private String detail;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected CsfActivity() {}

  /**
   * An activity row.
   *
   * @param companyId company
   * @param agent agent
   * @param action action
   * @param subject client, reference and detail
   * @param occurredAt time
   */
  public CsfActivity(
      Long companyId, String agent, ActivityAction action, Subject subject, Instant occurredAt) {
    this.companyId = companyId;
    this.agent = agent;
    this.action = action;
    this.clientId = subject.clientId();
    this.clientCode = subject.clientCode();
    this.reference = cut(subject.reference(), MAX_REFERENCE);
    this.detail = cut(subject.detail(), MAX_DETAIL);
    this.occurredAt = occurredAt;
  }

  private static String cut(String value, int max) {
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }

  /**
   * What an activity concerns.
   *
   * @param clientId client, may be null (a search)
   * @param clientCode client code, may be null
   * @param reference ARN, change number, attachment or message reference, may be null
   * @param detail search criteria or description, may be null
   */
  public record Subject(Long clientId, String clientCode, String reference, String detail) {}

  public Long getCompanyId() {
    return companyId;
  }

  public String getAgent() {
    return agent;
  }

  public ActivityAction getAction() {
    return action;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getReference() {
    return reference;
  }

  public String getDetail() {
    return detail;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
