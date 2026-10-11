package com.iortatechnxt.brokerverse.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One granted company (with all its branches, {@code branchId} null) or one granted branch of a
 * user whose data scope is not "all companies" (V1240, docs/architecture/DATA_SCOPE_DESIGN.md). A
 * scope change replaces the rows of the user.
 */
@Entity
@Table(name = "sec_user_data_scope")
public class UserDataScopeGrant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", updatable = false)
  private Long branchId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, updatable = false, length = 50)
  private String createdBy;

  protected UserDataScopeGrant() {}

  /**
   * Creates a grant.
   *
   * @param userId user
   * @param companyId company
   * @param branchId branch, null for all branches of the company
   * @param createdAt grant time
   * @param createdBy granting user
   */
  public UserDataScopeGrant(
      Long userId, Long companyId, Long branchId, Instant createdAt, String createdBy) {
    this.userId = userId;
    this.companyId = companyId;
    this.branchId = branchId;
    this.createdAt = createdAt;
    this.createdBy = createdBy;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }
}
