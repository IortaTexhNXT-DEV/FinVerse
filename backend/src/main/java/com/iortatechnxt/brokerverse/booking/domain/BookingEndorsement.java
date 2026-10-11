package com.iortatechnxt.brokerverse.booking.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An endorsement or cancellation posted on a booked account (BRNB.076/081/094). A financial one
 * references the invoice it produced; a non-financial one is recorded only.
 */
@Entity
@Table(name = "bkg_endorsement")
public class BookingEndorsement extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "endorsement_no", nullable = false, length = 40, updatable = false)
  private String endorsementNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "endorsement_type", nullable = false, length = 20, updatable = false)
  private EndorsementType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "cancellation_kind", length = 20, updatable = false)
  private CancellationKind cancellationKind;

  @Column(name = "period_basis", length = 20, updatable = false)
  private String periodBasis;

  @Column(name = "effective_date", nullable = false, updatable = false)
  private LocalDate effectiveDate;

  @Column(name = "policy_year", nullable = false, updatable = false)
  private int policyYear;

  @Column(name = "sum_insured_change", precision = 19, scale = 2, updatable = false)
  private BigDecimal sumInsuredChange;

  @Column(name = "rate_percent", precision = 19, scale = 8, updatable = false)
  private BigDecimal ratePercent;

  @Column(nullable = false, length = 1000, updatable = false)
  private String description;

  @Column(name = "reason_code", length = 40, updatable = false)
  private String reasonCode;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(name = "source_reference", length = 80, updatable = false)
  private String sourceReference;

  protected BookingEndorsement() {}

  /**
   * Records an endorsement.
   *
   * @param companyId company
   * @param endorsementNo endorsement number
   * @param arn account
   * @param accountId account id
   * @param type endorsement type
   * @param cancellationKind cancellation kind, null unless a cancellation
   * @param periodBasis PRO_RATA or SHORT_PERIOD for calculated amounts, else null
   * @param effectiveDate effective date
   * @param policyYear policy year concerned
   * @param change sum insured change and rate for calculated endorsements (may be null)
   * @param description description of the change
   * @param reasonCode reason (list CANCELLATION_REASON for cancellations)
   * @param sourceReference caller's idempotency key (e.g. an Operations adjustment request)
   */
  public BookingEndorsement(
      Long companyId,
      String endorsementNo,
      String arn,
      Long accountId,
      EndorsementType type,
      CancellationKind cancellationKind,
      String periodBasis,
      LocalDate effectiveDate,
      int policyYear,
      SumInsuredChange change,
      String description,
      String reasonCode,
      String sourceReference) {
    this.companyId = companyId;
    this.endorsementNo = endorsementNo;
    this.arn = arn;
    this.accountId = accountId;
    this.type = type;
    this.cancellationKind = cancellationKind;
    this.periodBasis = periodBasis;
    this.effectiveDate = effectiveDate;
    this.policyYear = policyYear;
    this.sumInsuredChange = change == null ? null : change.amount();
    this.ratePercent = change == null ? null : change.ratePercent();
    this.description = description;
    this.reasonCode = reasonCode;
    this.sourceReference = sourceReference;
  }

  /**
   * Links the invoice the endorsement produced.
   *
   * @param number invoice number
   */
  public void linkInvoice(String number) {
    this.invoiceNo = number;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getEndorsementNo() {
    return endorsementNo;
  }

  public String getArn() {
    return arn;
  }

  public Long getAccountId() {
    return accountId;
  }

  public EndorsementType getType() {
    return type;
  }

  public CancellationKind getCancellationKind() {
    return cancellationKind;
  }

  public String getPeriodBasis() {
    return periodBasis;
  }

  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public BigDecimal getSumInsuredChange() {
    return sumInsuredChange;
  }

  public BigDecimal getRatePercent() {
    return ratePercent;
  }

  public String getDescription() {
    return description;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  /**
   * Sum insured change of a calculated endorsement.
   *
   * @param amount change of the sum insured (negative reduces the cover)
   * @param ratePercent premium rate in percent, null for the product default
   */
  public record SumInsuredChange(BigDecimal amount, BigDecimal ratePercent) {}
}
