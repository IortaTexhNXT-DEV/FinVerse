package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * A request to enrol, modify, deactivate or reactivate a user, or to create, change, deactivate or
 * reactivate a group profile (BRNB.085; BRD 1.002-1.009, 3.002; PMADD05). It is saved as a draft,
 * submitted to the approver(s) chosen by the requester, approved in order (with a second approval
 * for risky changes, UAM-NFR-40), and then applied by the system, scheduled for its effective date
 * or, for group profiles, implemented by the System Administrator (USER_ACCESS_DESIGN section 4.3).
 * The change itself is applied by the service; this entity keeps the state machine.
 */
@Entity
@Table(name = "nba_access_request")
@SuppressWarnings("PMD.GodClass") // aggregate root of the request: content, approvers, lifecycle
public class AccessRequest extends BaseEntity {

  private static final String SEPARATOR = ",";
  private static final String REQUEST = "Request ";
  private static final String DECIDED = "ACCESS_REQUEST_DECIDED";
  private static final int MAX_TEXT = 1000;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "request_type", nullable = false, length = 30, updatable = false)
  private AccessRequestType requestType;

  @Enumerated(EnumType.STRING)
  @Column(name = "user_type", nullable = false, length = 10, updatable = false)
  private AccessUserType userType = AccessUserType.INTERNAL;

  @Column(length = 50)
  private String username;

  @Column(name = "full_name", length = 120)
  private String fullName;

  @Column(length = 120)
  private String email;

  @Column(name = "role_codes", length = 500)
  private String roleCodes;

  @Column(name = "home_branch_id")
  private Long homeBranchId;

  @Column(length = 1000)
  private String justification;

  @Column(name = "role_code", length = 40)
  private String roleCode;

  @Column(name = "permissions_added", length = 2000)
  private String permissionsAdded;

  @Column(name = "permissions_removed", length = 2000)
  private String permissionsRemoved;

  @Embedded private RequestedUserData userData;

  @Embedded private RequestedRole role;

  @Embedded private ExternalParty external;

  @Column(name = "effective_from")
  private LocalDate effectiveFrom;

  @Column(name = "batch_id", updatable = false)
  private Long batchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AccessRequestStatus status = AccessRequestStatus.DRAFT;

  @Column(name = "assigned_approver", length = 50)
  private String assignedApprover;

  @OneToMany(
      mappedBy = "request",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("sequence")
  private final List<AccessRequestApprover> approvers = new ArrayList<>();

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_comment", length = 1000)
  private String decisionComment;

  @Column(name = "returned_count", nullable = false)
  private int returnedCount;

  @Column(name = "second_approval_required", nullable = false)
  private boolean secondApprovalRequired;

  @Column(name = "risk_flags", length = 100)
  private String riskFlags;

  @Column(name = "cancel_reason", length = 1000)
  private String cancelReason;

  @Column(name = "cancelled_by", length = 50)
  private String cancelledBy;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "applied_at")
  private Instant appliedAt;

  @Column(name = "implemented_by", length = 50)
  private String implementedBy;

  @Column(name = "implemented_at")
  private Instant implementedAt;

  @Column(name = "apply_error", length = 1000)
  private String applyError;

  protected AccessRequest() {}

  /**
   * Creates a draft request, as a line of a bulk batch when a batch is given.
   *
   * @param requestNo request number
   * @param content what is requested
   * @param batchId bulk batch, null for a single request
   */
  public AccessRequest(String requestNo, AccessRequestContent content, Long batchId) {
    this.requestNo = requestNo;
    this.requestType = content.type();
    this.userType = content.userType();
    this.batchId = batchId;
    setContent(content);
  }

  private void setContent(AccessRequestContent content) {
    this.username = content.username();
    this.fullName = content.fullName();
    this.email = content.email();
    this.roleCodes = joined(content.roleCodes());
    this.homeBranchId = content.homeBranchId();
    this.justification = content.justification();
    this.userData = content.userData();
    this.role = content.role();
    this.external = content.external();
    this.effectiveFrom = content.effectiveFrom();
    RolePermissionChange change = content.permissionChange();
    this.roleCode = change == null ? null : change.roleCode();
    this.permissionsAdded = change == null ? null : joined(change.added());
    this.permissionsRemoved = change == null ? null : joined(change.removed());
  }

  private static String joined(Set<String> codes) {
    return codes == null || codes.isEmpty() ? null : String.join(SEPARATOR, new TreeSet<>(codes));
  }

  private static Set<String> split(String codes) {
    return codes == null ? Set.of() : new TreeSet<>(Arrays.asList(codes.split(SEPARATOR)));
  }

  /**
   * Replaces the content of a draft or returned request (BRD 1.002.1.2, 1.006.1). The type and the
   * user type cannot change.
   *
   * @param content new content
   */
  public void edit(AccessRequestContent content) {
    requireStatus(AccessRequestStatus.EDITABLE, "ACCESS_REQUEST_NOT_EDITABLE");
    if (content.type() != requestType || content.userType() != userType) {
      throw new BusinessRuleException(
          "ACCESS_REQUEST_TYPE_FIXED", "The type of request " + requestNo + " cannot change");
    }
    setContent(content);
  }

  /**
   * Submits a draft or resubmits a returned request to its approvers in order (BRD 1.002.1.4,
   * 1.006.1.5); the first approver decides first. Without approvers any approver may decide
   * (compatibility of the one-step submission).
   *
   * @param approverNames approvers in order
   * @param flags risk flags found on submission
   * @param by submitting user
   * @param when time
   */
  public void submit(
      List<String> approverNames, Set<AccessRiskFlag> flags, String by, Instant when) {
    requireStatus(AccessRequestStatus.EDITABLE, "ACCESS_REQUEST_NOT_EDITABLE");
    approvers.clear();
    for (int i = 0; i < approverNames.size(); i++) {
      approvers.add(new AccessRequestApprover(this, i + 1, approverNames.get(i)));
    }
    this.assignedApprover = approverNames.isEmpty() ? null : approverNames.get(0);
    this.status = AccessRequestStatus.PENDING;
    this.submittedBy = by;
    this.submittedAt = when;
    this.decidedBy = null;
    this.decidedAt = null;
    this.decisionComment = null;
    this.secondApprovalRequired = false;
    this.riskFlags = null;
    addRiskFlags(flags);
  }

  /**
   * Adds risk flags (found on submission or approval); any flag requires a second approval.
   *
   * @param flags flags
   */
  public void addRiskFlags(Set<AccessRiskFlag> flags) {
    Set<String> all = new TreeSet<>(split(riskFlags));
    flags.forEach(f -> all.add(f.name()));
    this.riskFlags = joined(all);
    this.secondApprovalRequired = !all.isEmpty();
  }

  /**
   * Records the approval of the current approver.
   *
   * @param approver approving user
   * @param when time
   * @param comment optional comment
   * @return true when a later approver still has to approve (the request stays PENDING)
   */
  public boolean recordApproval(String approver, Instant when, String comment) {
    requireStatus(Set.of(AccessRequestStatus.PENDING), DECIDED);
    currentApprover().ifPresent(a -> a.decide(AccessApproverDecision.APPROVED, comment, when));
    Optional<AccessRequestApprover> next = currentApprover();
    next.ifPresent(a -> this.assignedApprover = a.getApprover());
    this.decidedBy = approver;
    this.decidedAt = when;
    this.decisionComment = comment;
    return next.isPresent();
  }

  /** After the (last) first-level approval of a flagged request: waits for the second approver. */
  public void awaitSecondApproval() {
    requireStatus(Set.of(AccessRequestStatus.PENDING), DECIDED);
    this.status = AccessRequestStatus.PENDING_SECOND;
    this.assignedApprover = null;
  }

  /**
   * Completes the approval: applied now (APPROVED), on the effective date (SCHEDULED), or by the
   * System Administrator (FOR_IMPLEMENTATION).
   *
   * @param outcome APPROVED, SCHEDULED or FOR_IMPLEMENTATION
   * @param approver deciding user
   * @param when time
   * @param comment optional comment
   */
  public void approve(AccessRequestStatus outcome, String approver, Instant when, String comment) {
    requireStatus(AccessRequestStatus.AWAITING_DECISION, DECIDED);
    this.status = outcome;
    this.assignedApprover = null;
    this.decidedBy = approver;
    this.decidedAt = when;
    this.decisionComment = comment;
    if (outcome == AccessRequestStatus.APPROVED) {
      this.appliedAt = when;
    }
  }

  /**
   * Rejects the request with a reason (BRD 2.002.6); it is final.
   *
   * @param approver rejecting user
   * @param when time
   * @param comment reason
   */
  public void reject(String approver, Instant when, String comment) {
    close(AccessRequestStatus.REJECTED, AccessApproverDecision.REJECTED, approver, when, comment);
  }

  /**
   * Returns the request to its requester with remarks (BRD 2.002.7; BASAU 2.4.1).
   *
   * @param approver returning user
   * @param when time
   * @param comment remarks
   */
  public void returnToRequester(String approver, Instant when, String comment) {
    close(AccessRequestStatus.RETURNED, AccessApproverDecision.RETURNED, approver, when, comment);
    this.returnedCount++;
  }

  private void close(
      AccessRequestStatus to,
      AccessApproverDecision decision,
      String approver,
      Instant when,
      String comment) {
    requireStatus(AccessRequestStatus.AWAITING_DECISION, DECIDED);
    currentApprover().ifPresent(a -> a.decide(decision, comment, when));
    this.status = to;
    this.assignedApprover = null;
    this.decidedBy = approver;
    this.decidedAt = when;
    this.decisionComment = comment;
  }

  /**
   * Cancels the request with a reason (BRD 1.007; drafts too, UQ06 for scheduled requests). It is
   * kept, never deleted.
   *
   * @param reason reason
   * @param by cancelling user
   * @param when time
   */
  public void cancel(String reason, String by, Instant when) {
    requireStatus(AccessRequestStatus.CANCELLABLE, DECIDED);
    this.status = AccessRequestStatus.CANCELLED;
    this.assignedApprover = null;
    this.cancelReason = reason;
    this.cancelledBy = by;
    this.cancelledAt = when;
  }

  /**
   * A scheduled request was applied by the effective-date job (UAM-NFR-14).
   *
   * @param when time
   */
  public void applied(Instant when) {
    requireStatus(Set.of(AccessRequestStatus.SCHEDULED), "ACCESS_REQUEST_NOT_SCHEDULED");
    this.status = AccessRequestStatus.APPROVED;
    this.appliedAt = when;
    this.applyError = null;
  }

  /**
   * Keeps the reason why the change could not be applied (scheduled or bulk line).
   *
   * @param message error
   */
  public void applyFailed(String message) {
    this.applyError =
        message == null || message.length() <= MAX_TEXT ? message : message.substring(0, MAX_TEXT);
  }

  /**
   * The System Administrator implemented the approved group-profile request (BRD-11 p.6).
   *
   * @param by implementing user
   * @param when time
   */
  public void implemented(String by, Instant when) {
    requireStatus(
        Set.of(AccessRequestStatus.FOR_IMPLEMENTATION), "ACCESS_REQUEST_NOT_FOR_IMPLEMENTATION");
    this.status = AccessRequestStatus.IMPLEMENTED;
    this.implementedBy = by;
    this.implementedAt = when;
    this.appliedAt = when;
  }

  private void requireStatus(Set<AccessRequestStatus> allowed, String code) {
    if (!allowed.contains(status)) {
      throw new BusinessRuleException(code, REQUEST + requestNo + " is already " + status);
    }
  }

  private Optional<AccessRequestApprover> currentApprover() {
    return approvers.stream()
        .filter(a -> a.getDecision() == AccessApproverDecision.PENDING)
        .findFirst();
  }

  /**
   * Whether a user decided this request as one of its approvers (second-approval segregation).
   *
   * @param user user name
   * @return true when the user approved it
   */
  public boolean approvedBy(String user) {
    boolean approver =
        approvers.stream()
            .anyMatch(
                a ->
                    a.getDecision() == AccessApproverDecision.APPROVED
                        && sameUser(a.getApprover(), user));
    return approver || sameUser(user, decidedBy);
  }

  private static boolean sameUser(String a, String b) {
    return a != null && b != null && String.CASE_INSENSITIVE_ORDER.compare(a, b) == 0;
  }

  /**
   * Requested roles.
   *
   * @return role codes, empty when none
   */
  public Set<String> roles() {
    return split(roleCodes);
  }

  /**
   * The role and permissions of a group-profile request.
   *
   * @return role and permissions added / removed, null for a user request
   */
  public RolePermissionChange permissionChange() {
    return roleCode == null
        ? null
        : new RolePermissionChange(roleCode, split(permissionsAdded), split(permissionsRemoved));
  }

  /**
   * What the request asks for (to validate it again, edit it or apply it).
   *
   * @return content
   */
  public AccessRequestContent content() {
    return new AccessRequestContent(
        requestType,
        username,
        fullName,
        email,
        roles(),
        homeBranchId,
        justification,
        permissionChange(),
        userData,
        role,
        external,
        effectiveFrom);
  }

  /**
   * Risk flags of the request.
   *
   * @return flags, empty when none
   */
  public Set<AccessRiskFlag> riskFlags() {
    Set<AccessRiskFlag> flags = new TreeSet<>();
    split(riskFlags).forEach(f -> flags.add(AccessRiskFlag.valueOf(f)));
    return flags;
  }

  public List<AccessRequestApprover> getApprovers() {
    return Collections.unmodifiableList(approvers);
  }

  public int getReturnedCount() {
    return returnedCount;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public AccessRequestType getRequestType() {
    return requestType;
  }

  public AccessUserType getUserType() {
    return userType;
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

  public String getRoleCode() {
    return roleCode;
  }

  public String getJustification() {
    return justification;
  }

  public RequestedUserData getUserData() {
    return userData == null ? RequestedUserData.NONE : userData;
  }

  public RequestedRole getRole() {
    return role;
  }

  public ExternalParty getExternal() {
    return external;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public Long getBatchId() {
    return batchId;
  }

  public AccessRequestStatus getStatus() {
    return status;
  }

  public String getAssignedApprover() {
    return assignedApprover;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
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

  public boolean isSecondApprovalRequired() {
    return secondApprovalRequired;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public String getCancelledBy() {
    return cancelledBy;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public Instant getAppliedAt() {
    return appliedAt;
  }

  public String getImplementedBy() {
    return implementedBy;
  }

  public Instant getImplementedAt() {
    return implementedAt;
  }

  public String getApplyError() {
    return applyError;
  }
}
