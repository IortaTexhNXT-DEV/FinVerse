package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Quotation with negotiation iterations. Lifecycle: DRAFT → PENDING_APPROVAL → APPROVED (or
 * REJECTED) → CONVERTED into a policy; open quotations EXPIRE after their validity. A new iteration
 * re-opens the quotation as DRAFT.
 */
@Entity
@Table(name = "uw_quotation")
public class Quotation extends BaseEntity {

  private static final Set<QuotationStatus> ITERABLE =
      EnumSet.of(QuotationStatus.DRAFT, QuotationStatus.APPROVED, QuotationStatus.REJECTED);
  private static final Set<QuotationStatus> OPEN =
      EnumSet.of(QuotationStatus.DRAFT, QuotationStatus.PENDING_APPROVAL, QuotationStatus.APPROVED);

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "quotation_no", nullable = false, length = 40)
  private String quotationNo;

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(name = "validity_days", nullable = false)
  private int validityDays;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_party_id")
  private Party customer;

  @Column(name = "insured_name", nullable = false, length = 200)
  private String insuredName;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 10)
  private SourceType sourceType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "intermediary_party_id")
  private Party intermediary;

  @Column(name = "period_from", nullable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false)
  private LocalDate periodTo;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "share_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal sharePct;

  @Column(name = "commission_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal commissionRate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private QuotationStatus status = QuotationStatus.DRAFT;

  @Column(name = "current_iteration", nullable = false)
  private int currentIteration;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_reason", length = 200)
  private String decisionReason;

  @Column(name = "converted_policy_id")
  private Long convertedPolicyId;

  @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("iterationNo")
  private final List<QuotationIteration> iterations = new ArrayList<>();

  protected Quotation() {}

  /**
   * Creates a draft quotation; its first iteration is added with {@link #addIteration}.
   *
   * @param quotationNo allocated number
   * @param terms header terms
   */
  public Quotation(String quotationNo, QuotationTerms terms) {
    this.companyId = terms.product().getCompanyId();
    this.quotationNo = quotationNo;
    applyTerms(terms);
  }

  /**
   * Changes the terms of a draft.
   *
   * @param terms new terms
   */
  public void updateTerms(QuotationTerms terms) {
    require(EnumSet.of(QuotationStatus.DRAFT), "change");
    applyTerms(terms);
  }

  private void applyTerms(QuotationTerms t) {
    UnderwritingRules.requirePeriod(t.periodFrom(), t.periodTo());
    UnderwritingRules.requireClient(t.customer());
    UnderwritingRules.requireIntermediary(t.sourceType(), t.intermediary());
    if (t.sharePct() == null
        || t.sharePct().signum() <= 0
        || t.sharePct().compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new BusinessRuleException("INVALID_SHARE", "Share % must be above 0 and at most 100");
    }
    this.branchId = t.branchId();
    this.product = t.product();
    this.customer = t.customer();
    this.insuredName = t.insuredName();
    this.sourceType = t.sourceType();
    this.intermediary = t.intermediary();
    this.issueDate = t.issueDate();
    this.validityDays = t.validityDays();
    this.periodFrom = t.periodFrom();
    this.periodTo = t.periodTo();
    this.currency = t.currency();
    this.sharePct = t.sharePct();
    this.commissionRate =
        t.sourceType().isIntermediated() ? Money.nz(t.commissionRate()) : BigDecimal.ZERO;
  }

  /**
   * Adds a negotiation iteration; the quotation returns to DRAFT.
   *
   * @param values iteration figures
   * @param user maker
   * @param when timestamp
   * @return the new iteration
   */
  public QuotationIteration addIteration(IterationValues values, String user, Instant when) {
    require(ITERABLE, "iterate");
    this.status = QuotationStatus.DRAFT;
    this.decidedBy = null;
    this.decidedAt = null;
    this.decisionReason = null;
    if (Money.nz(values.discount()).compareTo(Money.nz(values.grossPremium())) > 0) {
      throw new BusinessRuleException("INVALID_DISCOUNT", "Discount cannot exceed gross premium");
    }
    QuotationIteration it = new QuotationIteration(this, iterations.size() + 1, values, user, when);
    iterations.add(it);
    this.currentIteration = it.getIterationNo();
    return it;
  }

  /**
   * Maker submits for approval.
   *
   * @param user maker
   */
  public void submit(String user) {
    require(EnumSet.of(QuotationStatus.DRAFT), "submit");
    if (iterations.isEmpty()) {
      throw new BusinessRuleException("ITERATION_REQUIRED", "The quotation has no figures");
    }
    this.status = QuotationStatus.PENDING_APPROVAL;
    this.submittedBy = user;
  }

  /**
   * Checker approves (must differ from the maker and submitter).
   *
   * @param checker approver
   * @param when timestamp
   */
  public void approve(String checker, Instant when) {
    decide(checker, when, null);
    this.status = QuotationStatus.APPROVED;
  }

  /**
   * Checker rejects.
   *
   * @param checker checker
   * @param when timestamp
   * @param reason reason
   */
  public void reject(String checker, Instant when, String reason) {
    decide(checker, when, reason);
    this.status = QuotationStatus.REJECTED;
  }

  private void decide(String checker, Instant when, String reason) {
    require(EnumSet.of(QuotationStatus.PENDING_APPROVAL), "decide on");
    if (Objects.equals(getCreatedBy(), checker) || Objects.equals(submittedBy, checker)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION",
          "Quotation " + quotationNo + " cannot be decided by the user who prepared it");
    }
    this.decidedBy = checker;
    this.decidedAt = when;
    this.decisionReason = reason;
  }

  /**
   * Records conversion into a policy.
   *
   * @param policyId policy
   * @param asOf conversion date (the quotation must still be valid)
   */
  public void markConverted(Long policyId, LocalDate asOf) {
    requireConvertible(asOf);
    this.status = QuotationStatus.CONVERTED;
    this.convertedPolicyId = policyId;
  }

  /**
   * Fails unless the quotation is approved and still valid.
   *
   * @param asOf conversion date
   */
  public void requireConvertible(LocalDate asOf) {
    require(EnumSet.of(QuotationStatus.APPROVED), "convert");
    if (asOf.isAfter(expiryDate())) {
      throw new BusinessRuleException(
          "QUOTATION_EXPIRED", "Quotation " + quotationNo + " expired on " + expiryDate());
    }
  }

  /**
   * Expires the quotation when its validity has lapsed.
   *
   * @param asOf date
   * @return true when it expired now
   */
  public boolean expireIfLapsed(LocalDate asOf) {
    if (OPEN.contains(status) && asOf.isAfter(expiryDate())) {
      this.status = QuotationStatus.EXPIRED;
      return true;
    }
    return false;
  }

  /**
   * Last day of validity.
   *
   * @return issue date + validity days
   */
  public LocalDate expiryDate() {
    return issueDate.plusDays(validityDays);
  }

  /**
   * The iteration currently offered.
   *
   * @return latest iteration
   */
  public QuotationIteration current() {
    return iterations.get(iterations.size() - 1);
  }

  private void require(Set<QuotationStatus> allowed, String action) {
    if (!allowed.contains(status)) {
      throw new BusinessRuleException(
          "INVALID_QUOTATION_STATUS",
          "Cannot " + action + " quotation " + quotationNo + " in status " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getQuotationNo() {
    return quotationNo;
  }

  public Product getProduct() {
    return product;
  }

  public Party getCustomer() {
    return customer;
  }

  public String getInsuredName() {
    return insuredName;
  }

  public SourceType getSourceType() {
    return sourceType;
  }

  public Party getIntermediary() {
    return intermediary;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public int getValidityDays() {
    return validityDays;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getSharePct() {
    return sharePct;
  }

  public BigDecimal getCommissionRate() {
    return commissionRate;
  }

  public QuotationStatus getStatus() {
    return status;
  }

  public int getCurrentIteration() {
    return currentIteration;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionReason() {
    return decisionReason;
  }

  public Long getConvertedPolicyId() {
    return convertedPolicyId;
  }

  public List<QuotationIteration> getIterations() {
    return List.copyOf(iterations);
  }
}
