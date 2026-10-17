package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A record of the Submitted Masterlist (BRIDSP-04; SUBMITTED_POLICIES_DESIGN section 4.1): one
 * submitted or inforced policy per segment, business type, PN or policy number and expiry. It is
 * not an account: it carries the loan, the assured, the policy and the risk as submitted, the
 * outcome of the last processing run (classification, bucket, renewal tag, RA template, limit flag,
 * fallout), the tracking fields of the handlers and, from the renewal hand-off on, the renewal
 * account and its booking. Field changes are written to {@link SbmPolicyHistory} by the services
 * ({@link SbmPolicyFields}).
 */
@SuppressWarnings(
    "PMD.CyclomaticComplexity") // aggregate of the masterlist record: data, outcome, tracking,
// renewal
@Entity
@Table(name = "sbm_policy")
public class SbmPolicy extends BaseEntity {

  /** Renewal tag set by a rule. */
  public static final String TAG_RULE = "RULE";

  /** Renewal tag set by hand. */
  public static final String TAG_MANUAL = "MANUAL";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "sbm_no", nullable = false, updatable = false, length = 30)
  private String sbmNo;

  @Column(nullable = false, length = 30)
  private String segment;

  @Enumerated(EnumType.STRING)
  @Column(name = "business_type", nullable = false, length = 2)
  private SbmBusinessType businessType;

  @Column(name = "natural_key", nullable = false, length = 80)
  private String naturalKey;

  @Column(name = "source_code", nullable = false, length = 30)
  private String sourceCode;

  @Column(name = "intake_run_id")
  private Long intakeRunId;

  @Column(name = "date_received", nullable = false)
  private LocalDate dateReceived;

  @Column(name = "renewal_month", length = 7)
  private String renewalMonth;

  @Embedded private SbmLoan loan;
  @Embedded private SbmAssured assured;
  @Embedded private SbmTerms terms;
  @Embedded private SbmRisk risk;
  @Embedded private SbmMarks marks;

  @Column(name = "loan_status", length = 20)
  private String loanStatus;

  @Column(nullable = false)
  private boolean amortised;

  @Column(nullable = false)
  private boolean migrated;

  @Column(name = "legacy_ref", length = 60)
  private String legacyRef;

  @Column(name = "has_documents", nullable = false)
  private boolean hasDocuments;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private SbmClassification classification;

  @Column(length = 40)
  private String bucket;

  @Column(name = "bucket_reason", length = 40)
  private String bucketReason;

  @Column(name = "ra_template", length = 20)
  private String raTemplate;

  @Column(name = "renewal_tag", length = 20)
  private String renewalTag;

  @Column(name = "renewal_tag_source", length = 10)
  private String renewalTagSource;

  @Column(name = "renewal_tag_reason", length = 40)
  private String renewalTagReason;

  @Column(name = "renewal_tagged_by", length = 50)
  private String renewalTaggedBy;

  @Column(name = "renewal_tagged_at")
  private Instant renewalTaggedAt;

  @Column(name = "insurer_approval_required", nullable = false)
  private boolean insurerApprovalRequired;

  @Column(nullable = false)
  private boolean fallout;

  @Column(name = "fallout_reason", length = 40)
  private String falloutReason;

  @Column(name = "last_run_no", length = 30)
  private String lastRunNo;

  @Column(name = "handler_username", length = 50)
  private String handlerUsername;

  @Column(name = "ao_username", length = 50)
  private String aoUsername;

  @Column(name = "conversion_status", length = 40)
  private String conversionStatus;

  @Column(name = "opportunity_tag", length = 40)
  private String opportunityTag;

  @Column(name = "adequacy_status", length = 20)
  private String adequacyStatus;

  @Column(length = 1000)
  private String remarks;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SbmPolicyStatus status;

  @Column(name = "status_reason", length = 250)
  private String statusReason;

  @Column(name = "renewal_ref", length = 40)
  private String renewalRef;

  @Column(name = "renewal_arn", length = 30)
  private String renewalArn;

  @Column(name = "booked_invoice_no", length = 40)
  private String bookedInvoiceNo;

  @Column(name = "booked_on")
  private LocalDate bookedOn;

  protected SbmPolicy() {}

  /**
   * A new masterlist record.
   *
   * @param companyId company
   * @param sbmNo masterlist number
   * @param data policy data
   * @param origin source, intake run, date received and first status
   */
  public SbmPolicy(Long companyId, String sbmNo, SbmPolicyData data, SbmPolicyOrigin origin) {
    this.companyId = companyId;
    this.sbmNo = sbmNo;
    this.sourceCode = origin.sourceCode();
    this.intakeRunId = origin.intakeRunId();
    this.dateReceived = origin.dateReceived();
    this.status = origin.status();
    apply(data);
  }

  /**
   * Replaces the policy data (intake, extraction, manual edit, migration).
   *
   * @param data policy data
   */
  public final void apply(SbmPolicyData data) {
    if (data.terms() == null || data.terms().expiryDate() == null) {
      throw new BusinessRuleException("SBM_EXPIRY_REQUIRED", "Expiry date is required");
    }
    LocalDate inception = data.terms().inceptionDate();
    if (inception != null && !data.terms().expiryDate().isAfter(inception)) {
      throw new BusinessRuleException(
          "SBM_PERIOD_INVALID", "The expiry date must be after the inception date");
    }
    this.segment = data.segment();
    this.businessType = data.businessType();
    this.loan = data.loan();
    this.assured = data.assured();
    this.risk = data.risk();
    this.marks = data.marks();
    this.terms = data.terms().withCoverageDays();
    this.naturalKey = naturalKey(loan.pnNo(), terms.policyNo());
    this.renewalMonth = YearMonth.from(terms.expiryDate()).toString();
  }

  /**
   * The natural key part of the PN or policy number (BRIDSP-01 R2).
   *
   * @param pnNo PN number, may be null
   * @param policyNo policy number, may be null
   * @return the PN, or {@code POL:} and the policy number
   */
  public static String naturalKey(String pnNo, String policyNo) {
    if (pnNo != null && !pnNo.isBlank()) {
      return pnNo.strip();
    }
    if (policyNo != null && !policyNo.isBlank()) {
      return "POL:" + policyNo.strip();
    }
    throw new BusinessRuleException(
        "SBM_KEY_REQUIRED", "Enter the PN number or the policy number of the policy");
  }

  /**
   * Moves the record to a status (mirror of its work case).
   *
   * @param newStatus status
   * @param reason reason, may be null
   */
  public void enter(SbmPolicyStatus newStatus, String reason) {
    this.status = newStatus;
    this.statusReason = reason;
  }

  /**
   * Records the loan found by the matching step.
   *
   * @param newLoanStatus LAMD loan status, null when unmatched
   * @param isAmortised amortised flag
   */
  public void matched(String newLoanStatus, boolean isAmortised) {
    this.loanStatus = newLoanStatus;
    this.amortised = isAmortised;
  }

  /**
   * Records the classification.
   *
   * @param value INFORCED or SUBMITTED
   */
  public void classify(SbmClassification value) {
    this.classification = value;
  }

  /**
   * Records the bucket of the disposition step.
   *
   * @param newBucket bucket (LOV SBM_BUCKET)
   * @param reason reason code (LOV SBM_REASON), may be null
   * @param template RA template, may be null
   */
  public void bucket(String newBucket, String reason, String template) {
    this.bucket = newBucket;
    this.bucketReason = reason;
    if (template != null) {
      this.raTemplate = template;
    }
  }

  /**
   * The rule's renewal tag; a manual tag is kept (BRIDSP-03 R2).
   *
   * @param tag RENEWABLE or NON_RENEWABLE
   * @param reason reason code
   * @return true when the rule tag was applied, false when a manual tag overrides it
   */
  public boolean tagByRule(String tag, String reason) {
    if (TAG_MANUAL.equals(renewalTagSource)) {
      return false;
    }
    this.renewalTag = tag;
    this.renewalTagReason = reason;
    this.renewalTagSource = TAG_RULE;
    return true;
  }

  /**
   * A manual renewal tag (BRIDSP-03).
   *
   * @param tag RENEWABLE or NON_RENEWABLE
   * @param reason reason, required for NON_RENEWABLE
   * @param by user
   * @param at time
   */
  public void tagManually(String tag, String reason, String by, Instant at) {
    this.renewalTag = tag;
    this.renewalTagReason = reason;
    this.renewalTagSource = TAG_MANUAL;
    this.renewalTaggedBy = by;
    this.renewalTaggedAt = at;
  }

  /**
   * The limits step result (BRIDSP-16).
   *
   * @param required whether the insurer's approval is required
   */
  public void insurerApproval(boolean required) {
    this.insurerApprovalRequired = required;
  }

  /**
   * Flags or clears the fallout of the last run (BRIDSP-10).
   *
   * @param reason reason code, null to clear
   * @param runNo run
   */
  public void fallout(String reason, String runNo) {
    this.fallout = reason != null;
    this.falloutReason = reason;
    this.lastRunNo = runNo;
  }

  /**
   * Updates the tracking fields (BRIDSP-29).
   *
   * @param tracking handler, AO, conversion status, opportunity tag and remarks
   */
  public void track(SbmTracking tracking) {
    this.handlerUsername = tracking.handlerUsername();
    this.aoUsername = tracking.aoUsername();
    this.conversionStatus = tracking.conversionStatus();
    this.opportunityTag = tracking.opportunityTag();
    this.remarks = tracking.remarks();
  }

  /**
   * Sets the conversion status only (renewal progress).
   *
   * @param value conversion status (LOV SBM_CONVERSION_STATUS)
   */
  public void convert(String value) {
    this.conversionStatus = value;
  }

  /**
   * Records the adequacy of the last policy review.
   *
   * @param value ADEQUATE or WITH_FINDINGS
   */
  public void adequacy(String value) {
    this.adequacyStatus = value;
  }

  /**
   * Marks a migrated record (BRIDSP-33).
   *
   * @param legacyReference legacy reference
   */
  public void migratedFrom(String legacyReference) {
    this.migrated = true;
    this.legacyRef = legacyReference;
  }

  /** A document is attached to the record. */
  public void documentAttached() {
    this.hasDocuments = true;
  }

  /**
   * The renewal started (hand-off) or its account is known.
   *
   * @param reference renewal reference, may be null while pending
   * @param arn renewal account, may be null
   */
  public void renewal(String reference, String arn) {
    this.renewalRef = reference;
    if (arn != null) {
      this.renewalArn = arn;
    }
  }

  /**
   * The renewal was booked (BRIDSP-27).
   *
   * @param invoiceNo booked invoice
   * @param date booking date
   */
  public void booked(String invoiceNo, LocalDate date) {
    this.bookedInvoiceNo = invoiceNo;
    this.bookedOn = date;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSbmNo() {
    return sbmNo;
  }

  public String getSegment() {
    return segment;
  }

  public SbmBusinessType getBusinessType() {
    return businessType;
  }

  public String getNaturalKey() {
    return naturalKey;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  public Long getIntakeRunId() {
    return intakeRunId;
  }

  public LocalDate getDateReceived() {
    return dateReceived;
  }

  public String getRenewalMonth() {
    return renewalMonth;
  }

  public SbmLoan getLoan() {
    return loan == null ? SbmLoan.NONE : loan;
  }

  public SbmAssured getAssured() {
    return assured;
  }

  public SbmTerms getTerms() {
    return terms;
  }

  public SbmRisk getRisk() {
    return risk == null ? SbmRisk.NONE : risk;
  }

  public SbmMarks getMarks() {
    return marks == null ? SbmMarks.NONE : marks;
  }

  /**
   * The data of the record, for an edit.
   *
   * @return policy data
   */
  public SbmPolicyData data() {
    return new SbmPolicyData(
        segment, businessType, getLoan(), assured, terms, getRisk(), getMarks());
  }

  public String getLoanStatus() {
    return loanStatus;
  }

  public boolean isAmortised() {
    return amortised;
  }

  public boolean isMigrated() {
    return migrated;
  }

  public String getLegacyRef() {
    return legacyRef;
  }

  public boolean isHasDocuments() {
    return hasDocuments;
  }

  public SbmClassification getClassification() {
    return classification;
  }

  public String getBucket() {
    return bucket;
  }

  public String getBucketReason() {
    return bucketReason;
  }

  public String getRaTemplate() {
    return raTemplate;
  }

  public String getRenewalTag() {
    return renewalTag;
  }

  public String getRenewalTagSource() {
    return renewalTagSource;
  }

  public String getRenewalTagReason() {
    return renewalTagReason;
  }

  public String getRenewalTaggedBy() {
    return renewalTaggedBy;
  }

  public Instant getRenewalTaggedAt() {
    return renewalTaggedAt;
  }

  public boolean isInsurerApprovalRequired() {
    return insurerApprovalRequired;
  }

  public boolean isFallout() {
    return fallout;
  }

  public String getFalloutReason() {
    return falloutReason;
  }

  public String getLastRunNo() {
    return lastRunNo;
  }

  public String getHandlerUsername() {
    return handlerUsername;
  }

  public String getAoUsername() {
    return aoUsername;
  }

  public String getConversionStatus() {
    return conversionStatus;
  }

  public String getOpportunityTag() {
    return opportunityTag;
  }

  public String getAdequacyStatus() {
    return adequacyStatus;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * The tracking fields.
   *
   * @return tracking
   */
  public SbmTracking tracking() {
    return new SbmTracking(handlerUsername, aoUsername, conversionStatus, opportunityTag, remarks);
  }

  public SbmPolicyStatus getStatus() {
    return status;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public String getRenewalRef() {
    return renewalRef;
  }

  public String getRenewalArn() {
    return renewalArn;
  }

  public String getBookedInvoiceNo() {
    return bookedInvoiceNo;
  }

  public LocalDate getBookedOn() {
    return bookedOn;
  }
}
