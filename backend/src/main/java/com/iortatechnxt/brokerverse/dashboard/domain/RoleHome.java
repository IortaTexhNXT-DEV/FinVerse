package com.iortatechnxt.brokerverse.dashboard.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * The home-page widgets of one role, in display order (Role Home Set-up, FR-CR-010). A new set is
 * requested by the System Administrator with a reason and applies when another user approves it
 * (maker-checker). Until a set is first approved the role shows the delivered widgets.
 */
@Entity
@Table(name = "sys_role_home")
public class RoleHome extends BaseEntity {

  private static final int REASON_LENGTH = 500;
  private static final String SEPARATOR = ",";

  @Column(name = "role_code", nullable = false, length = 40, unique = true, updatable = false)
  private String roleCode;

  @Column(length = 400)
  private String widgets;

  @Column(name = "pending_widgets", length = 400)
  private String pendingWidgets;

  @Column(name = "pending_reason", length = REASON_LENGTH)
  private String pendingReason;

  @Column(name = "pending_by", length = 50)
  private String pendingBy;

  @Column(name = "pending_at")
  private Instant pendingAt;

  @Column(name = "changed_reason", length = REASON_LENGTH)
  private String changedReason;

  @Column(name = "changed_by", length = 50)
  private String changedBy;

  @Column(name = "changed_at")
  private Instant changedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  protected RoleHome() {}

  /**
   * Starts the set of a role (it shows the delivered widgets until a change is approved).
   *
   * @param roleCode role code
   */
  public RoleHome(String roleCode) {
    this.roleCode = roleCode;
  }

  /**
   * Keeps a requested set until another user approves it.
   *
   * @param codes widget codes in display order (validated by the caller)
   * @param reason why the set changes
   * @param by requester
   * @param at request time
   */
  public void requestChange(List<String> codes, String reason, String by, Instant at) {
    if (pendingWidgets != null) {
      throw new BusinessRuleException(
          "ROLE_HOME_CHANGE_PENDING",
          "A change of this role's home page already waits for approval; approve or reject it"
              + " first");
    }
    if (codes.equals(getWidgets())) {
      throw new BusinessRuleException(
          "ROLE_HOME_UNCHANGED", "The role's home page already shows these widgets in this order");
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("REASON_REQUIRED", "Give the reason for the change");
    }
    this.pendingWidgets = String.join(SEPARATOR, codes);
    this.pendingReason = reason.strip();
    this.pendingBy = by;
    this.pendingAt = at;
  }

  /**
   * Applies the set that waits; the requester may not approve it.
   *
   * @param approver approving user
   * @param at approval time
   */
  public void approveChange(String approver, Instant at) {
    requirePending();
    if (CurrentUser.sameUser(approver, pendingBy)) {
      throw new BusinessRuleException(
          "SAME_USER_APPROVAL", "Another user must approve the change you requested");
    }
    this.widgets = pendingWidgets;
    this.changedReason = pendingReason;
    this.changedBy = pendingBy;
    this.changedAt = at;
    this.approvedBy = approver;
    clearPending();
  }

  /** Drops the set that waits. */
  public void rejectChange() {
    requirePending();
    clearPending();
  }

  private void requirePending() {
    if (pendingWidgets == null) {
      throw new BusinessRuleException(
          "NO_ROLE_HOME_CHANGE_PENDING", "No change of this role's home page waits for approval");
    }
  }

  private void clearPending() {
    this.pendingWidgets = null;
    this.pendingReason = null;
    this.pendingBy = null;
    this.pendingAt = null;
  }

  private static List<String> split(String value) {
    return value == null || value.isBlank()
        ? List.of()
        : Arrays.stream(value.split(SEPARATOR)).filter(c -> !c.isBlank()).toList();
  }

  public String getRoleCode() {
    return roleCode;
  }

  /**
   * The widgets shown, in order.
   *
   * @return widget codes; the delivered set while no change was approved
   */
  public List<String> getWidgets() {
    return widgets == null ? HomeWidget.defaults() : split(widgets);
  }

  /**
   * Whether the role shows the delivered set.
   *
   * @return true until a change is approved
   */
  public boolean isDelivered() {
    return widgets == null;
  }

  /**
   * The widgets requested.
   *
   * @return widget codes, null when no change waits
   */
  public List<String> getPendingWidgets() {
    return pendingWidgets == null ? null : split(pendingWidgets);
  }

  public String getPendingReason() {
    return pendingReason;
  }

  public String getPendingBy() {
    return pendingBy;
  }

  public Instant getPendingAt() {
    return pendingAt;
  }

  public String getChangedReason() {
    return changedReason;
  }

  public String getChangedBy() {
    return changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }
}
