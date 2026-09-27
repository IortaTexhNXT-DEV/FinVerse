package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * An approval to price one transaction on a non-current package version or on a rate other than the
 * scheme rate (BRPM.007 "a non-current rate triggers an approval workflow and is logged").
 * Requested from the quotation (or account), authorised by a PRODUCT_AUTHORIZE holder other than
 * the requester in My Approvals; its reference number is the {@code rateOverrideRef} of rating.
 */
@Entity
@Table(name = "cat_rate_scheme_exception")
public class RateOverride extends AuthorizableEntity implements CatalogRecord {

  @Column(name = "reference_no", nullable = false, length = 30, updatable = false)
  private String referenceNo;

  @Column(name = "product_code", nullable = false, length = 20, updatable = false)
  private String productCode;

  @Column(nullable = false, length = 20, updatable = false)
  private String purpose;

  @Column(name = "requested_version_no", updatable = false)
  private Integer requestedVersionNo;

  @Column(name = "requested_rate", precision = 19, scale = 8, updatable = false)
  private BigDecimal requestedRate;

  @Column(name = "transaction_ref", nullable = false, length = 40, updatable = false)
  private String transactionRef;

  @Column(nullable = false, length = 1000, updatable = false)
  private String reason;

  @Column(name = "valid_until", nullable = false, updatable = false)
  private LocalDate validUntil;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_comment", length = 1000)
  private String decisionComment;

  protected RateOverride() {}

  /**
   * Records a request, pending authorisation.
   *
   * @param referenceNo reference number (RSE-yyyy-n)
   * @param request what is requested and why
   */
  public RateOverride(String referenceNo, Request request) {
    this.referenceNo = referenceNo;
    this.productCode = request.productCode();
    this.purpose = request.purpose();
    this.requestedVersionNo = request.requestedVersionNo();
    this.requestedRate = request.requestedRate();
    this.transactionRef = request.transactionRef();
    this.reason = request.reason();
    this.validUntil = request.validUntil();
  }

  /**
   * Records the approver's decision (after {@link #authorize} or {@link #reject}).
   *
   * @param approver user who decided
   * @param when decision time
   * @param comment comment of an approval or reason of a rejection, may be null
   */
  public void recordDecision(String approver, Instant when, String comment) {
    this.decidedBy = approver;
    this.decidedAt = when;
    this.decisionComment = comment;
  }

  /**
   * Rejects the pending exception (checker, never the requester); it can no longer be used.
   *
   * @param approver user who rejects
   * @param when decision time
   * @param reason why it is rejected
   */
  public void reject(String approver, Instant when, String reason) {
    if (getRecordStatus() != RecordStatus.PENDING_AUTHORIZATION) {
      throw new BusinessRuleException(
          "RECORD_NOT_PENDING", "Rate exception " + referenceNo + " is already decided");
    }
    if (Objects.equals(getMaker(), approver)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION",
          "A rate exception is decided by someone other than its requester");
    }
    deactivate();
    recordDecision(approver, when, reason);
  }

  /**
   * Whether the approved exception still covers a transaction on a date.
   *
   * @param product risk code
   * @param date business date
   * @return true when authorised, for the product and not past its validity
   */
  public boolean covers(String product, LocalDate date) {
    return isActive() && productCode.equals(product) && !date.isAfter(validUntil);
  }

  @Override
  public String catalogReference() {
    return referenceNo;
  }

  @Override
  public String catalogDescription() {
    String what =
        requestedVersionNo != null
            ? "version " + requestedVersionNo
            : "rate " + requestedRate.stripTrailingZeros().toPlainString() + "%";
    return productCode + " " + what + " for " + transactionRef;
  }

  public String getReferenceNo() {
    return referenceNo;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getPurpose() {
    return purpose;
  }

  public Integer getRequestedVersionNo() {
    return requestedVersionNo;
  }

  public BigDecimal getRequestedRate() {
    return requestedRate;
  }

  public String getTransactionRef() {
    return transactionRef;
  }

  public String getReason() {
    return reason;
  }

  public LocalDate getValidUntil() {
    return validUntil;
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

  /**
   * An exception request.
   *
   * @param productCode risk code
   * @param purpose NEW_BUSINESS, RENEWAL or ENDORSEMENT
   * @param requestedVersionNo non-current version wanted, null when only the rate differs
   * @param requestedRate item rate wanted in percent, null when only the version differs
   * @param transactionRef quotation number or ARN
   * @param reason justification
   * @param validUntil last day it may be used
   */
  public record Request(
      String productCode,
      String purpose,
      Integer requestedVersionNo,
      BigDecimal requestedRate,
      String transactionRef,
      String reason,
      LocalDate validUntil) {}
}
