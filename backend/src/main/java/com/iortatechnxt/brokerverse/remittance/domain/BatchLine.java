package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.OrStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One invoice of a remittance batch (OPERATIONS_DESIGN 4.3): the invoice facts printed on the
 * schedule, the read-only amounts (RMTID.002 addendum), the exclusion with its reason, user and
 * restore (RMTID.002), the insurer OR and its comparison with the paid AR (RMTID.012/013/016).
 */
@Entity
@Table(name = "rem_batch_line")
public class BatchLine extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "batch_id", nullable = false, updatable = false)
  private RemittanceBatch batch;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "endorsement_no", length = 40, updatable = false)
  private String endorsementNo;

  @Column(name = "policy_no", length = 60, updatable = false)
  private String policyNo;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
  private String assuredName;

  @Column(name = "risk_code", length = 20, updatable = false)
  private String riskCode;

  @Column(name = "product_line", length = 30, updatable = false)
  private String productLine;

  @Column(length = 40, updatable = false)
  private String segment;

  @Column(name = "inception_date", nullable = false, updatable = false)
  private LocalDate inceptionDate;

  @Column(name = "expiry_date", nullable = false, updatable = false)
  private LocalDate expiryDate;

  @Column(name = "booking_date", nullable = false, updatable = false)
  private LocalDate bookingDate;

  @Column(name = "last_paid_on", updatable = false)
  private LocalDate lastPaidOn;

  @Enumerated(EnumType.STRING)
  @Column(name = "prev_remittance_status", nullable = false, length = 30, updatable = false)
  private RemittanceStatus previousStatus;

  @Column(name = "basic_premium", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal basicPremium;

  @Embedded private RemittanceAmounts amounts;

  @Column(name = "cpc2_code", length = 30, updatable = false)
  private String cpc2Code;

  @Column(name = "cpc2_rate", precision = 9, scale = 4, updatable = false)
  private BigDecimal cpc2Rate;

  @Column(nullable = false)
  private boolean excluded;

  @Column(name = "exclusion_reason", length = 40)
  private String exclusionReason;

  @Column(name = "exclusion_comment", length = 500)
  private String exclusionComment;

  @Column(name = "excluded_by", length = 50)
  private String excludedBy;

  @Column(name = "excluded_at")
  private Instant excludedAt;

  @Column(name = "restored_by", length = 50)
  private String restoredBy;

  @Column(name = "restored_at")
  private Instant restoredAt;

  @Column(name = "insurer_or_no", length = 40)
  private String insurerOrNo;

  @Column(name = "insurer_or_date")
  private LocalDate insurerOrDate;

  @Column(name = "insurer_or_amount", precision = 19, scale = 2)
  private BigDecimal insurerOrAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "or_status", length = 20)
  private OrStatus orStatus;

  @Column(name = "or_run_no", length = 30)
  private String orRunNo;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "remitted_status", length = 30)
  private RemittanceStatus remittedStatus;

  protected BatchLine() {}

  /**
   * A line extracted from the ledger.
   *
   * @param facts invoice facts
   * @param previousStatus remittance status before the extraction (restored on exclusion)
   * @param basicPremium basic premium part of the paid AR (incentive base)
   * @param amounts amounts
   */
  public BatchLine(
      LineFacts facts,
      RemittanceStatus previousStatus,
      BigDecimal basicPremium,
      RemittanceAmounts amounts) {
    this.invoiceNo = facts.invoiceNo();
    this.arn = facts.arn();
    this.endorsementNo = facts.endorsementNo();
    this.policyNo = facts.policyNo();
    this.clientCode = facts.clientCode();
    this.assuredName = facts.assuredName();
    this.riskCode = facts.riskCode();
    this.productLine = facts.productLine();
    this.segment = facts.segment();
    this.inceptionDate = facts.inceptionDate();
    this.expiryDate = facts.expiryDate();
    this.bookingDate = facts.bookingDate();
    this.lastPaidOn = facts.lastPaidOn();
    this.previousStatus = previousStatus;
    this.basicPremium = basicPremium;
    this.amounts = amounts;
  }

  /**
   * Records the CPC2 criterion applied to the line (DIS 3.29.2); the amounts already carry it.
   *
   * @param code incentive criterion code
   * @param rate rate in percent
   */
  public void cpc2Criterion(String code, BigDecimal rate) {
    this.cpc2Code = code;
    this.cpc2Rate = rate;
  }

  void attach(RemittanceBatch owner) {
    this.batch = owner;
  }

  /**
   * Excludes the line with a reason (RMTID.002 addendum); amounts stay untouched.
   *
   * @param reason reason code (LOV REMIT_EXCLUSION_REASON)
   * @param comment comment, may be null
   * @param by user
   * @param at time
   */
  void exclude(String reason, String comment, String by, Instant at) {
    if (excluded) {
      throw new BusinessRuleException(
          "REMIT_LINE_ALREADY_EXCLUDED", "Invoice " + invoiceNo + " is already excluded");
    }
    excluded = true;
    exclusionReason = reason;
    exclusionComment = comment;
    excludedBy = by;
    excludedAt = at;
  }

  /**
   * Restores an excluded line before submission (RMTID.002 addendum).
   *
   * @param by user
   * @param at time
   */
  void restore(String by, Instant at) {
    if (!excluded) {
      throw new BusinessRuleException(
          "REMIT_LINE_NOT_EXCLUDED", "Invoice " + invoiceNo + " is not excluded");
    }
    excluded = false;
    restoredBy = by;
    restoredAt = at;
  }

  /**
   * Records the insurer's OR (RMTID.013) and compares its amount with the paid AR (RMTID.016).
   *
   * @param or OR number, date and amount
   * @param runNo upload run
   * @return matched or mismatch
   */
  public OrStatus recordInsurerOr(InsurerOr or, String runNo) {
    this.insurerOrNo = or.number();
    this.insurerOrDate = or.date();
    this.insurerOrAmount = or.amount();
    this.orRunNo = runNo;
    this.orStatus =
        or.amount().compareTo(amounts.paidAr()) == 0 ? OrStatus.MATCHED : OrStatus.AMOUNT_MISMATCH;
    return orStatus;
  }

  /**
   * Records the journal of the remittance posting.
   *
   * @param journal journal batch number
   */
  public void posted(String journal) {
    this.journalBatchNo = journal;
  }

  /**
   * Records the invoice's remittance status after the DV was assigned (RMTID.019).
   *
   * @param status partially or fully remitted
   */
  public void remitted(RemittanceStatus status) {
    this.remittedStatus = status;
  }

  public RemittanceBatch getBatch() {
    return batch;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getArn() {
    return arn;
  }

  public String getEndorsementNo() {
    return endorsementNo;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getRiskCode() {
    return riskCode;
  }

  public String getProductLine() {
    return productLine;
  }

  public String getSegment() {
    return segment;
  }

  public LocalDate getInceptionDate() {
    return inceptionDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public LocalDate getBookingDate() {
    return bookingDate;
  }

  public LocalDate getLastPaidOn() {
    return lastPaidOn;
  }

  public RemittanceStatus getPreviousStatus() {
    return previousStatus;
  }

  public BigDecimal getBasicPremium() {
    return basicPremium;
  }

  public RemittanceAmounts getAmounts() {
    return amounts;
  }

  public String getCpc2Code() {
    return cpc2Code;
  }

  public BigDecimal getCpc2Rate() {
    return cpc2Rate;
  }

  public boolean isExcluded() {
    return excluded;
  }

  public String getExclusionReason() {
    return exclusionReason;
  }

  public String getExclusionComment() {
    return exclusionComment;
  }

  public String getExcludedBy() {
    return excludedBy;
  }

  public Instant getExcludedAt() {
    return excludedAt;
  }

  public String getRestoredBy() {
    return restoredBy;
  }

  public Instant getRestoredAt() {
    return restoredAt;
  }

  public String getInsurerOrNo() {
    return insurerOrNo;
  }

  public LocalDate getInsurerOrDate() {
    return insurerOrDate;
  }

  public BigDecimal getInsurerOrAmount() {
    return insurerOrAmount;
  }

  public OrStatus getOrStatus() {
    return orStatus;
  }

  public String getOrRunNo() {
    return orRunNo;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public RemittanceStatus getRemittedStatus() {
    return remittedStatus;
  }

  /**
   * Invoice facts of a line.
   *
   * @param invoiceNo invoice
   * @param arn ARN
   * @param endorsementNo endorsement, may be null
   * @param policyNo policy
   * @param clientCode client
   * @param assuredName assured
   * @param riskCode risk code
   * @param productLine product line
   * @param segment segment
   * @param inceptionDate inception
   * @param expiryDate expiry
   * @param bookingDate booking date
   * @param lastPaidOn value date of the last payment applied
   */
  public record LineFacts(
      String invoiceNo,
      String arn,
      String endorsementNo,
      String policyNo,
      String clientCode,
      String assuredName,
      String riskCode,
      String productLine,
      String segment,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      LocalDate bookingDate,
      LocalDate lastPaidOn) {}

  /**
   * An insurer OR for a line.
   *
   * @param number OR number
   * @param date OR date
   * @param amount OR amount
   */
  public record InsurerOr(String number, LocalDate date, BigDecimal amount) {}
}
