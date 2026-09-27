package com.iortatechnxt.brokerverse.migration.archive.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An access to the legacy archive (DATA_MIGRATION_DESIGN section 16; FR-DM-111): user, time, source
 * address, action (search, view, download, export), criteria, record keys, result count and reason.
 * Append-only: the database refuses updates and deletes.
 */
@Entity
@Table(name = "mig_access_log")
public class AccessLog {

  private static final int MAX_CRITERIA = 1000;
  private static final int MAX_KEYS = 2000;
  private static final int MAX_REASON = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(name = "accessed_at", nullable = false, updatable = false)
  private Instant accessedAt;

  @Column(name = "source_address", length = 60, updatable = false)
  private String sourceAddress;

  @Column(nullable = false, length = 10, updatable = false)
  private String action;

  @Column(length = 1000, updatable = false)
  private String criteria;

  @Column(name = "record_keys", length = 2000, updatable = false)
  private String recordKeys;

  @Column(name = "result_count", nullable = false, updatable = false)
  private int resultCount;

  @Column(name = "reason_code", length = 40, updatable = false)
  private String reasonCode;

  @Column(name = "reason_text", length = 500, updatable = false)
  private String reasonText;

  protected AccessLog() {}

  /**
   * An access.
   *
   * @param companyId company
   * @param who user, time and source address
   * @param action SEARCH, VIEW, DOWNLOAD or EXPORT
   * @param what criteria, keys and count
   * @param reason reason code and text
   */
  public AccessLog(Long companyId, Who who, String action, What what, Reason reason) {
    this.companyId = companyId;
    this.username = who.username();
    this.accessedAt = who.when();
    this.sourceAddress = who.address();
    this.action = action;
    this.criteria = clip(what.criteria(), MAX_CRITERIA);
    this.recordKeys = clip(what.keys(), MAX_KEYS);
    this.resultCount = what.count();
    this.reasonCode = reason.code();
    this.reasonText = clip(reason.text(), MAX_REASON);
  }

  private static String clip(String text, int max) {
    return text == null || text.length() <= max ? text : text.substring(0, max);
  }

  public Long getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getUsername() {
    return username;
  }

  public Instant getAccessedAt() {
    return accessedAt;
  }

  public String getSourceAddress() {
    return sourceAddress;
  }

  public String getAction() {
    return action;
  }

  public String getCriteria() {
    return criteria;
  }

  public String getRecordKeys() {
    return recordKeys;
  }

  public int getResultCount() {
    return resultCount;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getReasonText() {
    return reasonText;
  }

  /**
   * Who accessed.
   *
   * @param username user
   * @param when time
   * @param address source address
   */
  public record Who(String username, Instant when, String address) {}

  /**
   * What was accessed.
   *
   * @param criteria search criteria
   * @param keys record keys
   * @param count result count
   */
  public record What(String criteria, String keys, int count) {}

  /**
   * Why.
   *
   * @param code reason code (list MIG_ACCESS_REASON)
   * @param text reason text
   */
  public record Reason(String code, String text) {}
}
