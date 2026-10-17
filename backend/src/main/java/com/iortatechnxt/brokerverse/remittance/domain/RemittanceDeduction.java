package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A remittance deduction {@code RDN-<yyyy>-n} (ACSL 2.9.2; ACCOUNTING_DISBURSEMENT_DESIGN 12.2): an
 * amount the insurer confirmed in writing (for example an AR insurer's refund after a return
 * premium) that BDOI deducts from its next remittance to that insurer. Prepared by ACSL, submitted
 * with the insurer's confirmation, confirmed by another user (REMIT_DEDUCTION_CONFIRM), then
 * consumed by the next approved batches of the insurer and currency, each time capped at what the
 * batch pays (workflow {@code REM_DEDUCTION}).
 */
@Entity
@Table(name = "rem_deduction")
public class RemittanceDeduction extends BaseEntity {

  private static final String DEDUCTION = "Deduction ";
  private static final int SCALE = 2;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "deduction_no", nullable = false, length = 30, updatable = false)
  private String deductionNo;

  @Column(name = "insurer_code", nullable = false, length = 30)
  private String insurerCode;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "source_type", nullable = false, length = 30)
  private String sourceType;

  @Column(name = "source_ref", nullable = false, length = 60)
  private String sourceRef;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal appliedAmount = BigDecimal.ZERO.setScale(SCALE);

  @Column(name = "confirmation_ref", length = 100)
  private String confirmationRef;

  @Column(name = "confirmation_date")
  private LocalDate confirmationDate;

  @Column(length = 1000)
  private String remarks;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private DeductionStage stage = DeductionStage.DRAFT;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "confirmed_by", length = 50)
  private String confirmedBy;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  protected RemittanceDeduction() {}

  /**
   * A new draft deduction.
   *
   * @param companyId company
   * @param deductionNo number
   * @param terms insurer, currency, source, amount and confirmation
   */
  public RemittanceDeduction(Long companyId, String deductionNo, Terms terms) {
    this.companyId = companyId;
    this.deductionNo = deductionNo;
    update(terms);
  }

  /**
   * Changes a draft.
   *
   * @param terms new terms
   */
  public final void update(Terms terms) {
    if (stage != DeductionStage.DRAFT) {
      throw new BusinessRuleException(
          "REMIT_DEDUCTION_STAGE", DEDUCTION + deductionNo + " can only change as a draft");
    }
    if (terms.amount() == null || terms.amount().signum() <= 0) {
      throw new BusinessRuleException(
          "REMIT_DEDUCTION_AMOUNT", "A deduction needs a positive amount");
    }
    this.insurerCode = terms.insurerCode();
    this.currency = terms.currency();
    this.sourceType = terms.sourceType();
    this.sourceRef = terms.sourceRef();
    this.invoiceNo = terms.invoiceNo();
    this.amount = terms.amount().setScale(SCALE, RoundingMode.HALF_UP);
    this.confirmationRef = terms.confirmationRef();
    this.confirmationDate = terms.confirmationDate();
    this.remarks = terms.remarks();
  }

  /**
   * Records the submission: only with the insurer's confirmation (ACSL 2.9.2).
   *
   * @param by user
   * @param at time
   */
  public void submitted(String by, Instant at) {
    if (confirmationRef == null || confirmationRef.isBlank() || confirmationDate == null) {
      throw new BusinessRuleException(
          "REMIT_DEDUCTION_UNCONFIRMED",
          DEDUCTION
              + deductionNo
              + " needs the insurer's confirmation reference and date before submission");
    }
    this.submittedBy = by;
    this.submittedAt = at;
  }

  /**
   * Records the confirmation by another user (four eyes).
   *
   * @param by user
   * @param at time
   */
  public void confirmed(String by, Instant at) {
    if (CurrentUser.sameUser(by, submittedBy) || CurrentUser.sameUser(by, getCreatedBy())) {
      throw new BusinessRuleException(
          "REMIT_DEDUCTION_FOUR_EYES",
          DEDUCTION + deductionNo + " must be confirmed by another user");
    }
    this.confirmedBy = by;
    this.confirmedAt = at;
  }

  /**
   * Consumes part of the deduction for a batch: never more than is left, nor than the cap.
   *
   * @param cap most the batch can take
   * @return amount consumed (zero when nothing is left or the cap is zero)
   */
  public BigDecimal consume(BigDecimal cap) {
    BigDecimal take = remaining().min(cap.max(BigDecimal.ZERO.setScale(SCALE)));
    appliedAmount = appliedAmount.add(take);
    return take;
  }

  /**
   * Gives back an amount consumed by a batch whose DV was cancelled.
   *
   * @param given amount
   */
  public void giveBack(BigDecimal given) {
    appliedAmount = appliedAmount.subtract(given).max(BigDecimal.ZERO.setScale(SCALE));
  }

  /**
   * What is still to deduct.
   *
   * @return amount less applied
   */
  public BigDecimal remaining() {
    return amount.subtract(appliedAmount);
  }

  /**
   * Mirrors the workflow stage.
   *
   * @param newStage stage
   */
  public void markStage(DeductionStage newStage) {
    this.stage = newStage;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getDeductionNo() {
    return deductionNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getCurrency() {
    return currency;
  }

  public String getSourceType() {
    return sourceType;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getAppliedAmount() {
    return appliedAmount;
  }

  public String getConfirmationRef() {
    return confirmationRef;
  }

  public LocalDate getConfirmationDate() {
    return confirmationDate;
  }

  public String getRemarks() {
    return remarks;
  }

  public DeductionStage getStage() {
    return stage;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getConfirmedBy() {
    return confirmedBy;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  /**
   * What a deduction settles.
   *
   * @param insurerCode insurer party code
   * @param currency currency
   * @param sourceType source (LOV REMIT_DEDUCTION_SOURCE)
   * @param sourceRef source reference (AR insurer's refund, OR, memo)
   * @param invoiceNo invoice concerned, may be null
   * @param amount amount to deduct
   * @param confirmationRef insurer's confirmation reference, required to submit
   * @param confirmationDate date of the insurer's confirmation, required to submit
   * @param remarks remarks
   */
  public record Terms(
      String insurerCode,
      String currency,
      String sourceType,
      String sourceRef,
      String invoiceNo,
      BigDecimal amount,
      String confirmationRef,
      LocalDate confirmationDate,
      String remarks) {}

  /** Stage of a deduction in the REM_DEDUCTION workflow. */
  public enum DeductionStage {
    /** Prepared by ACSL. */
    DRAFT,
    /** Submitted with the insurer's confirmation. */
    FOR_CONFIRMATION,
    /** Confirmed; waiting for the next batch of the insurer. */
    CONFIRMED,
    /** Fully applied to remittance batches. */
    APPLIED,
    /** Cancelled. */
    CANCELLED
  }
}
