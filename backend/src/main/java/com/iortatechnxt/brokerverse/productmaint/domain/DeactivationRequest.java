package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

/**
 * A package deactivation request (BDOI FRS FRPM.003.04 to FRPM.003.07): the package, its
 * deactivation effective date (today or later), the reason, remarks and the approver chosen by the
 * requestor. On approval the package expiry date becomes the later of the effective date and the
 * approval date; on rejection the package stays active and the remarks are kept.
 */
@Entity
@Table(name = "pm_deactivation_request")
public class DeactivationRequest extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "request_no", nullable = false, length = 30, updatable = false)
  private String requestNo;

  @Column(name = "product_code", nullable = false, length = 20, updatable = false)
  private String productCode;

  @Column(name = "version_no", updatable = false)
  private Integer versionNo;

  @Column(name = "package_name", nullable = false, length = 200, updatable = false)
  private String packageName;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(nullable = false, length = 40)
  private String reason;

  @Column(length = 1000)
  private String remarks;

  @Column(nullable = false, length = 50)
  private String approver;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DeactivationStatus status = DeactivationStatus.PENDING;

  @Column(name = "package_end_date")
  private LocalDate packageEndDate;

  @Column(name = "decision_remarks", length = 1000)
  private String decisionRemarks;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  /** For JPA. */
  protected DeactivationRequest() {}

  /**
   * Creates a pending request.
   *
   * @param companyId company
   * @param requestNo request number
   * @param target the package and its expiry date before the request
   * @param details effective date, reason, remarks and approver
   */
  public DeactivationRequest(Long companyId, String requestNo, Target target, Details details) {
    this.companyId = companyId;
    this.requestNo = requestNo;
    this.productCode = target.productCode();
    this.versionNo = target.versionNo();
    this.packageName = target.packageName();
    this.packageEndDate = target.packageEndDate();
    this.effectiveDate = details.effectiveDate();
    this.reason = details.reason();
    this.remarks = details.remarks();
    this.approver = details.approver();
  }

  /**
   * Approves the request; the expiry date is the later of the effective date and the approval date.
   *
   * @param user approver
   * @param remarks approval remarks (optional)
   * @param when approval time
   * @param approvalDate approval date (business date)
   * @return the package expiry date to apply
   */
  public LocalDate approve(String user, String remarks, Instant when, LocalDate approvalDate) {
    requirePending();
    this.status = DeactivationStatus.APPROVED;
    decide(user, remarks, when);
    this.expiryDate = effectiveDate.isAfter(approvalDate) ? effectiveDate : approvalDate;
    return expiryDate;
  }

  /**
   * Rejects the request; the package stays active.
   *
   * @param user approver
   * @param remarks approval remarks (mandatory)
   * @param when decision time
   */
  public void reject(String user, String remarks, Instant when) {
    requirePending();
    if (remarks == null || remarks.isBlank()) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_REMARKS", "Enter the approval remarks of the rejection");
    }
    this.status = DeactivationStatus.REJECTED;
    decide(user, remarks, when);
  }

  /**
   * Withdraws the request before the decision.
   *
   * @param user requestor
   * @param when time
   */
  public void cancel(String user, Instant when) {
    requirePending();
    this.status = DeactivationStatus.CANCELLED;
    decide(user, null, when);
  }

  /**
   * Gives the request to another approver (the first one is not available).
   *
   * @param user the new approver
   */
  public void reassign(String user) {
    requirePending();
    this.approver = user;
  }

  private void decide(String user, String text, Instant when) {
    this.decidedBy = user;
    this.decidedAt = when;
    this.decisionRemarks = text == null || text.isBlank() ? null : text.strip();
  }

  private void requirePending() {
    if (status != DeactivationStatus.PENDING) {
      throw new BusinessRuleException(
          "PKG_DEACTIVATION_DECIDED",
          "Deactivation request "
              + requestNo
              + " is already "
              + status.label().toLowerCase(Locale.ROOT));
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public String getProductCode() {
    return productCode;
  }

  public Integer getVersionNo() {
    return versionNo;
  }

  public String getPackageName() {
    return packageName;
  }

  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  public String getReason() {
    return reason;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getApprover() {
    return approver;
  }

  public DeactivationStatus getStatus() {
    return status;
  }

  public LocalDate getPackageEndDate() {
    return packageEndDate;
  }

  public String getDecisionRemarks() {
    return decisionRemarks;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  /**
   * The package of the request.
   *
   * @param productCode package code
   * @param versionNo version in force
   * @param packageName package name
   * @param packageEndDate expiry date before the request
   */
  public record Target(
      String productCode, Integer versionNo, String packageName, LocalDate packageEndDate) {}

  /**
   * What the requestor enters.
   *
   * @param effectiveDate deactivation effective date
   * @param reason reason code (list PKG_DEACTIVATION_REASON)
   * @param remarks remarks (optional)
   * @param approver approver chosen
   */
  public record Details(LocalDate effectiveDate, String reason, String remarks, String approver) {

    /**
     * The details without surrounding blanks; blank remarks become null.
     *
     * @return trimmed details
     */
    public Details trimmed() {
      return new Details(
          effectiveDate,
          reason == null ? null : reason.strip(),
          remarks == null || remarks.isBlank() ? null : remarks.strip(),
          approver == null ? null : approver.strip());
    }
  }
}
