package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A renewal candidate (RENEWAL_DESIGN section 4.1): one expiring root invoice and policy year
 * (BRRN.005), or one migrated or submitted policy, from extraction to closure. It carries the
 * renewal reference (BRRN.022), a listing snapshot, the stage of its {@code RNW_CASE} work case,
 * the bucket (BRRN.023), the current disposition, the assignment, the flag chips, and the links to
 * the renewal account, the New Business path and the booked renewal invoice.
 */
@Entity
@Table(name = "rnw_candidate")
public class RenewalCandidate extends BaseEntity {

  private static final int SEARCH_LENGTH = 2000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "renewal_ref", nullable = false, length = 30, updatable = false)
  private String renewalRef;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private CandidateSource source;

  @Column(name = "source_ref", length = 60, updatable = false)
  private String sourceRef;

  @Column(name = "extraction_run_id", updatable = false)
  private Long extractionRunId;

  @Column(name = "expiring_invoice_no", length = 40, updatable = false)
  private String expiringInvoiceNo;

  @Column(name = "expiring_arn", length = 40)
  private String expiringArn;

  @Column(name = "policy_year")
  private Integer policyYear;

  @Embedded private CandidateSnapshot snapshot;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RenewalStage stage = RenewalStage.EXTRACTED;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Bucket bucket;

  @Column(name = "bucket_rule_version")
  private Integer bucketRuleVersion;

  @Column(name = "evaluated_at")
  private Instant evaluatedAt;

  @Column(name = "last_check_run_id")
  private Long lastCheckRunId;

  @Embedded private CurrentDisposition disposition;

  @Embedded private DispositionProposal proposal;

  @Column(name = "initiated_by", length = 50)
  private String initiatedBy;

  @Column(name = "initiated_at")
  private Instant initiatedAt;

  @Column(name = "assigned_ao", length = 50)
  private String assignedAo;

  @Column(name = "assigned_po", length = 50)
  private String assignedPo;

  @Column(name = "owner_unit", length = 20)
  private String ownerUnit;

  @Column(name = "marketing_locked_at")
  private Instant marketingLockedAt;

  @Column(name = "history_viewed", nullable = false)
  private boolean historyViewed;

  @Embedded private final CandidateFlags flags = new CandidateFlags();

  @Embedded private CandidateExpiry expiry = new CandidateExpiry();

  @Embedded private CandidateAttention attention = new CandidateAttention();

  @Enumerated(EnumType.STRING)
  @Column(name = "ra_notice", nullable = false, length = 10)
  private RaNotice raNotice = RaNotice.NONE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private RenewalPath path = RenewalPath.STANDARD;

  @Column(name = "resolved_product_code", length = 20)
  private String resolvedProductCode;

  @Column(name = "resolved_version_no")
  private Integer resolvedVersionNo;

  @Column(name = "renewal_arn", length = 40)
  private String renewalArn;

  @Column(name = "quotation_ref", length = 30)
  private String quotationRef;

  @Column(name = "proposal_ref", length = 30)
  private String proposalRef;

  @Column(name = "renewed_invoice_no", length = 40)
  private String renewedInvoiceNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "closed_as", length = 30)
  private ClosedAs closedAs;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "search_text", length = SEARCH_LENGTH)
  private String searchText;

  protected RenewalCandidate() {}

  /**
   * Creates a candidate in stage EXTRACTED (not yet initiated, BRRN.021).
   *
   * @param companyId company
   * @param renewalRef renewal reference (RNW-yyyy-nnnnnn)
   * @param origin source, source reference, expiring invoice, ARN and policy year
   * @param snapshot listing snapshot
   * @param extractionRunId extraction run, null when handed over
   */
  public RenewalCandidate(
      Long companyId,
      String renewalRef,
      Origin origin,
      CandidateSnapshot snapshot,
      Long extractionRunId) {
    this.companyId = companyId;
    this.renewalRef = renewalRef;
    this.source = origin.source();
    this.sourceRef = origin.sourceRef();
    this.expiringInvoiceNo = origin.invoiceNo();
    this.expiringArn = origin.arn();
    this.policyYear = origin.policyYear();
    this.extractionRunId = extractionRunId;
    this.disposition = CurrentDisposition.NONE;
    this.proposal = DispositionProposal.NONE;
    refresh(snapshot);
  }

  /**
   * Refreshes the listing snapshot (each evaluation) and the owning unit when it is not yet set.
   *
   * @param newSnapshot snapshot
   */
  public final void refresh(CandidateSnapshot newSnapshot) {
    this.snapshot = newSnapshot;
    if (ownerUnit == null && newSnapshot.sales() != null) {
      this.ownerUnit = newSnapshot.sales().salesUnit();
    }
    this.searchText = searchTextOf();
  }

  private String searchTextOf() {
    String text =
        Stream.of(
                renewalRef,
                expiringInvoiceNo,
                expiringArn,
                sourceRef,
                snapshot.policyNo(),
                snapshot.pnNos(),
                snapshot.client() == null ? null : snapshot.client().clientCode(),
                snapshot.clientName(),
                snapshot.client() == null ? null : snapshot.client().assuredName(),
                quotationRef,
                proposalRef,
                renewalArn)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "))
            .toLowerCase(Locale.ROOT);
    return text.length() > SEARCH_LENGTH ? text.substring(0, SEARCH_LENGTH) : text;
  }

  /**
   * Records the explicit initiation (BRRN.021 AC3: a separate event from the extraction).
   *
   * @param user initiating user
   * @param at time
   */
  public void initiate(String user, Instant at) {
    this.initiatedBy = user;
    this.initiatedAt = at;
  }

  /**
   * Mirrors the stage of the work case.
   *
   * @param newStage stage
   */
  public void enter(RenewalStage newStage) {
    this.stage = newStage;
  }

  /**
   * Stores the result of an evaluation.
   *
   * @param newBucket bucket
   * @param ruleSetVersion version of the bucket rule set, null for the built-in rules
   * @param checkRunId check run
   * @param at time
   */
  public void evaluated(Bucket newBucket, Integer ruleSetVersion, Long checkRunId, Instant at) {
    this.bucket = newBucket;
    this.bucketRuleVersion = ruleSetVersion;
    this.lastCheckRunId = checkRunId;
    this.evaluatedAt = at;
  }

  /**
   * Stores the decision-matrix proposal.
   *
   * @param newProposal proposal
   */
  public void propose(DispositionProposal newProposal) {
    this.proposal = newProposal == null ? DispositionProposal.NONE : newProposal;
  }

  /**
   * Takes a new current disposition (the row is kept in the disposition history).
   *
   * @param newDisposition disposition
   */
  public void dispose(CurrentDisposition newDisposition) {
    this.disposition = newDisposition == null ? CurrentDisposition.NONE : newDisposition;
  }

  /**
   * Assigns the Marketing AO.
   *
   * @param ao user name, null to clear
   */
  public void assignAo(String ao) {
    this.assignedAo = ao;
  }

  /**
   * Assigns the Processing Officer.
   *
   * @param po user name, null to clear
   */
  public void assignPo(String po) {
    this.assignedPo = po;
  }

  /**
   * Moves the candidate to another Marketing unit (accepted transfer).
   *
   * @param unit sales unit
   */
  public void moveToUnit(String unit) {
    this.ownerUnit = unit;
  }

  /**
   * Locks or unlocks the Marketing side (Renewal Advice generated or cancelled, BRD 2.004.9).
   *
   * @param at lock time, null to unlock
   */
  public void lockMarketing(Instant at) {
    this.marketingLockedAt = at;
  }

  /** Records that the account history was opened (BRRN.027). */
  public void markHistoryViewed() {
    this.historyViewed = true;
  }

  /**
   * Records the Renewal Advice notice.
   *
   * @param notice notice
   */
  public void noticed(RaNotice notice) {
    this.raNotice = notice;
  }

  /**
   * Sets the path of the renewal.
   *
   * @param newPath path
   */
  public void takePath(RenewalPath newPath) {
    this.path = newPath;
  }

  /**
   * Records the BIBS package version a migrated policy renews on (DMQ36).
   *
   * @param productCode risk code
   * @param versionNo package version, null for a non-package product
   */
  public void resolvePackage(String productCode, Integer versionNo) {
    this.resolvedProductCode = productCode;
    this.resolvedVersionNo = versionNo;
  }

  /**
   * Links the renewal account (created at processing).
   *
   * @param arn renewal account
   */
  public void linkRenewalAccount(String arn) {
    this.renewalArn = arn;
    this.searchText = searchTextOf();
  }

  /**
   * Links the quotation or PRF of the New Business path.
   *
   * @param quotationNo quotation number, null when a PRF
   * @param prfNo PRF number, null when a quotation
   */
  public void linkNewBusiness(String quotationNo, String prfNo) {
    this.quotationRef = quotationNo;
    this.proposalRef = prfNo;
    this.searchText = searchTextOf();
  }

  /**
   * Closes the candidate.
   *
   * @param how closure kind
   * @param invoiceNo renewal invoice when renewed, else null
   * @param at time
   */
  public void close(ClosedAs how, String invoiceNo, Instant at) {
    this.closedAs = how;
    this.renewedInvoiceNo = invoiceNo;
    this.closedAt = at;
    getAttention().set(null, null, null);
  }

  /** Re-opens a closed candidate (BRD 2.004.10). */
  public void reopen() {
    this.closedAs = null;
    this.closedAt = null;
  }

  /**
   * Days from a date to the expiry (negative once expired).
   *
   * @param today business date
   * @return days to expiry
   */
  public long daysToExpiry(LocalDate today) {
    return ChronoUnit.DAYS.between(today, getExpiryDate());
  }

  /**
   * What a renewal account of this renewal renews (shared work item BT0): the expiring ARN of a
   * booked policy, the legacy reference of a migrated one, the SBM number of a submitted one.
   *
   * @return reference for {@code renewal_of_ref}
   */
  public String renewsRef() {
    return source == CandidateSource.BIBS_INVOICE ? expiringArn : sourceRef;
  }

  public LocalDate getExpiryDate() {
    return snapshot.expiryDate();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRenewalRef() {
    return renewalRef;
  }

  public CandidateSource getSource() {
    return source;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public Long getExtractionRunId() {
    return extractionRunId;
  }

  public String getExpiringInvoiceNo() {
    return expiringInvoiceNo;
  }

  public String getExpiringArn() {
    return expiringArn;
  }

  public Integer getPolicyYear() {
    return policyYear;
  }

  public CandidateSnapshot getSnapshot() {
    return snapshot;
  }

  public RenewalStage getStage() {
    return stage;
  }

  public Bucket getBucket() {
    return bucket;
  }

  public Integer getBucketRuleVersion() {
    return bucketRuleVersion;
  }

  public Instant getEvaluatedAt() {
    return evaluatedAt;
  }

  public Long getLastCheckRunId() {
    return lastCheckRunId;
  }

  public CurrentDisposition getDisposition() {
    return disposition == null ? CurrentDisposition.NONE : disposition;
  }

  public DispositionProposal getProposal() {
    return proposal == null ? DispositionProposal.NONE : proposal;
  }

  public String getInitiatedBy() {
    return initiatedBy;
  }

  public Instant getInitiatedAt() {
    return initiatedAt;
  }

  public String getAssignedAo() {
    return assignedAo;
  }

  public String getAssignedPo() {
    return assignedPo;
  }

  public String getOwnerUnit() {
    return ownerUnit;
  }

  public Instant getMarketingLockedAt() {
    return marketingLockedAt;
  }

  public boolean isHistoryViewed() {
    return historyViewed;
  }

  public CandidateFlags getFlags() {
    return flags;
  }

  /**
   * The hold cover and the closing letter (FR-RN-082, 083, 086).
   *
   * @return expiry facts, never null
   */
  public CandidateExpiry getExpiry() {
    if (expiry == null) {
      expiry = new CandidateExpiry();
    }
    return expiry;
  }

  /**
   * The attention flag of the listing (FR-RN-102).
   *
   * @return attention, never null
   */
  public CandidateAttention getAttention() {
    if (attention == null) {
      attention = new CandidateAttention();
    }
    return attention;
  }

  /**
   * The effective expiry date: the end of a confirmed hold cover, otherwise the policy expiry.
   *
   * @return date
   */
  public LocalDate effectiveExpiry() {
    return getExpiry().effectiveExpiry(getExpiryDate());
  }

  public RaNotice getRaNotice() {
    return raNotice;
  }

  public RenewalPath getPath() {
    return path;
  }

  public String getResolvedProductCode() {
    return resolvedProductCode;
  }

  public Integer getResolvedVersionNo() {
    return resolvedVersionNo;
  }

  public String getRenewalArn() {
    return renewalArn;
  }

  public String getQuotationRef() {
    return quotationRef;
  }

  public String getProposalRef() {
    return proposalRef;
  }

  public String getRenewedInvoiceNo() {
    return renewedInvoiceNo;
  }

  public ClosedAs getClosedAs() {
    return closedAs;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  /**
   * Where a candidate comes from.
   *
   * @param source source
   * @param sourceRef SBM number or legacy reference, null for a BIBS invoice
   * @param invoiceNo expiring root invoice, null for a legacy or submitted policy
   * @param arn expiring ARN, null when none
   * @param policyYear policy year of the expiring invoice
   */
  public record Origin(
      CandidateSource source, String sourceRef, String invoiceNo, String arn, Integer policyYear) {}
}
