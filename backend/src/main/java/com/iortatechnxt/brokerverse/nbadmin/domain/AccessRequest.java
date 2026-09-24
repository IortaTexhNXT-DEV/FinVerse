package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * A request by the Business Administrator to create a user, change a user's roles or disable /
 * enable a user (BRNB.085, BRD 3.3.5 and 3.4.2). Nothing changes until the Approver approves it;
 * the approval applies the change.
 */
@Entity
@Table(name = "nba_access_request")
public class AccessRequest extends BaseEntity {

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "request_type", nullable = false, length = 20, updatable = false)
  private AccessRequestType requestType;

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(name = "full_name", length = 120, updatable = false)
  private String fullName;

  @Column(length = 120, updatable = false)
  private String email;

  @Column(name = "role_codes", length = 500, updatable = false)
  private String roleCodes;

  @Column(name = "home_branch_id", updatable = false)
  private Long homeBranchId;

  @Column(nullable = false, length = 1000, updatable = false)
  private String justification;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AccessRequestStatus status = AccessRequestStatus.PENDING;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_comment", length = 1000)
  private String decisionComment;

  protected AccessRequest() {}

  /**
   * Creates a pending request.
   *
   * @param requestNo request number
   * @param content what is requested
   */
  public AccessRequest(String requestNo, AccessRequestContent content) {
    this.requestNo = requestNo;
    this.requestType = content.type();
    this.username = content.username();
    this.fullName = content.fullName();
    this.email = content.email();
    this.roleCodes =
        content.roleCodes() == null || content.roleCodes().isEmpty()
            ? null
            : String.join(",", new TreeSet<>(content.roleCodes()));
    this.homeBranchId = content.homeBranchId();
    this.justification = content.justification();
  }

  /**
   * Records the decision (the change itself is applied by the service).
   *
   * @param approved approved or rejected
   * @param approver deciding user
   * @param when time
   * @param comment comment
   */
  public void decide(boolean approved, String approver, Instant when, String comment) {
    if (status != AccessRequestStatus.PENDING) {
      throw new BusinessRuleException(
          "ACCESS_REQUEST_DECIDED", "Request " + requestNo + " is already " + status);
    }
    this.status = approved ? AccessRequestStatus.APPROVED : AccessRequestStatus.REJECTED;
    this.decidedBy = approver;
    this.decidedAt = when;
    this.decisionComment = comment;
  }

  /**
   * Requested roles.
   *
   * @return role codes, empty when none
   */
  public Set<String> roles() {
    return roleCodes == null ? Set.of() : new TreeSet<>(Arrays.asList(roleCodes.split(",")));
  }

  public String getRequestNo() {
    return requestNo;
  }

  public AccessRequestType getRequestType() {
    return requestType;
  }

  public String getUsername() {
    return username;
  }

  public String getFullName() {
    return fullName;
  }

  public String getEmail() {
    return email;
  }

  public Long getHomeBranchId() {
    return homeBranchId;
  }

  public String getJustification() {
    return justification;
  }

  public AccessRequestStatus getStatus() {
    return status;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionComment() {
    return decisionComment;
  }
}
