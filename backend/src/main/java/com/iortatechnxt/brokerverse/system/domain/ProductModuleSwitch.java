package com.iortatechnxt.brokerverse.system.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The switch of one {@link ProductModule} in this deployment. A change is requested by the system
 * administrator with a reason and takes effect when another user approves it (maker-checker).
 */
@Entity
@Table(name = "sys_product_module")
public class ProductModuleSwitch extends BaseEntity {

  private static final int REASON_LENGTH = 500;

  @Column(nullable = false, length = 40, unique = true, updatable = false)
  private String code;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "pending_enabled")
  private Boolean pendingEnabled;

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

  protected ProductModuleSwitch() {}

  /**
   * Creates a switch (tests; the migration creates the rows of the catalogue).
   *
   * @param code module code
   * @param enabled whether the module is on
   * @param sortOrder display order
   */
  public ProductModuleSwitch(String code, boolean enabled, int sortOrder) {
    this.code = code;
    this.enabled = enabled;
    this.sortOrder = sortOrder;
  }

  /**
   * Keeps a requested change until another user approves it.
   *
   * @param on requested state
   * @param reason why the module is switched
   * @param by requester
   * @param at request time
   */
  public void requestChange(boolean on, String reason, String by, Instant at) {
    if (pendingEnabled != null) {
      throw new BusinessRuleException(
          "MODULE_CHANGE_PENDING",
          "A change of this module already waits for approval; approve or reject it first");
    }
    if (on == enabled) {
      throw new BusinessRuleException(
          "MODULE_ALREADY_IN_STATE",
          "The module is already " + (on ? "switched on" : "switched off"));
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("REASON_REQUIRED", "Give the reason for the change");
    }
    this.pendingEnabled = on;
    this.pendingReason = reason.strip();
    this.pendingBy = by;
    this.pendingAt = at;
  }

  /**
   * Applies the change that waits; the requester may not approve it.
   *
   * @param approver approving user
   * @param at approval time
   * @return the new state
   */
  public boolean approveChange(String approver, Instant at) {
    requirePending();
    if (CurrentUser.sameUser(approver, pendingBy)) {
      throw new BusinessRuleException(
          "SAME_USER_APPROVAL", "Another user must approve the change you requested");
    }
    this.enabled = pendingEnabled;
    this.changedReason = pendingReason;
    this.changedBy = pendingBy;
    this.changedAt = at;
    this.approvedBy = approver;
    clearPending();
    return enabled;
  }

  /** Drops the change that waits. */
  public void rejectChange() {
    requirePending();
    clearPending();
  }

  private void requirePending() {
    if (pendingEnabled == null) {
      throw new BusinessRuleException(
          "NO_MODULE_CHANGE_PENDING", "No change of this module waits for approval");
    }
  }

  private void clearPending() {
    this.pendingEnabled = null;
    this.pendingReason = null;
    this.pendingBy = null;
    this.pendingAt = null;
  }

  /**
   * Sets the state directly (tests of the module switches).
   *
   * @param on new state
   */
  public void forceState(boolean on) {
    this.enabled = on;
  }

  public String getCode() {
    return code;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public Boolean getPendingEnabled() {
    return pendingEnabled;
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
