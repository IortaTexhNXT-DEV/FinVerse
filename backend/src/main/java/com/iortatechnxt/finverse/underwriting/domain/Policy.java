package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import java.util.List;

/**
 * Insurance policy (or marine certificate under an open cover): header terms, insured risks and the
 * premium of the original issue. Changes after approval are made by {@link Endorsement}s.
 *
 * <p>Lifecycle and maker-checker rules are held in {@link ApprovalWorkflow}; premium figures in
 * {@link PremiumBreakdown}; accounting references in {@link PostingRefs}.
 */
@Entity
@Table(name = "uw_policy")
public class Policy extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "policy_no", nullable = false, length = 40)
  private String policyNo;

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

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(name = "period_from", nullable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false)
  private LocalDate periodTo;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "uw_year", nullable = false)
  private int uwYear;

  @Enumerated(EnumType.STRING)
  @Column(name = "business_type", nullable = false, length = 30)
  private BusinessType businessType;

  @Column(name = "share_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal sharePct;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coinsurer_party_id")
  private Party coinsurer;

  @Column(name = "coinsurance_leader", nullable = false)
  private boolean coinsuranceLeader;

  @Column(name = "discount_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal discountRate;

  @Column(name = "loading_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal loadingRate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "open_cover_id")
  private OpenCover openCover;

  @Column(name = "quotation_id")
  private Long quotationId;

  @Column(name = "cancelled_on")
  private LocalDate cancelledOn;

  @Embedded private final ApprovalWorkflow workflow = new ApprovalWorkflow();

  @Embedded private PremiumBreakdown premium = new PremiumBreakdown();

  @Embedded private PostingRefs refs = new PostingRefs();

  @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<PolicyRisk> risks = new ArrayList<>();

  protected Policy() {}

  /**
   * Creates a draft policy.
   *
   * @param policyNo allocated policy (or certificate) number
   * @param terms header terms
   * @param openCover open cover for marine certificates, else null
   * @param quotationId quotation converted into this policy, else null
   */
  public Policy(String policyNo, PolicyTerms terms, OpenCover openCover, Long quotationId) {
    this.companyId = terms.product().getCompanyId();
    this.policyNo = policyNo;
    this.openCover = openCover;
    this.quotationId = quotationId;
    applyTerms(terms);
  }

  /**
   * Changes the header terms of a draft (the product is kept: callers pass the policy's product).
   *
   * @param terms new terms
   */
  public void updateTerms(PolicyTerms terms) {
    workflow.requireEditable(label());
    applyTerms(terms);
  }

  private void applyTerms(PolicyTerms t) {
    UnderwritingRules.requirePeriod(t.periodFrom(), t.periodTo());
    UnderwritingRules.requireClient(t.customer());
    UnderwritingRules.requireIntermediary(t.sourceType(), t.intermediary());
    UnderwritingRules.requireCoinsurance(t.businessType(), t.sharePct(), t.coinsurer());
    this.branchId = t.branchId();
    this.product = t.product();
    this.customer = t.customer();
    this.insuredName = t.insuredName();
    this.sourceType = t.sourceType();
    this.intermediary = t.intermediary();
    this.issueDate = t.issueDate();
    this.periodFrom = t.periodFrom();
    this.periodTo = t.periodTo();
    this.uwYear = UnderwritingRules.underwritingYear(t.periodFrom());
    this.currency = t.currency();
    this.businessType = t.businessType();
    this.sharePct = t.sharePct();
    this.coinsurer = t.coinsurer();
    this.coinsuranceLeader =
        t.businessType() == BusinessType.DIRECT_WITH_COINSURANCE && t.coinsuranceLeader();
    this.discountRate = Money.nz(t.discountRate());
    this.loadingRate = Money.nz(t.loadingRate());
  }

  /**
   * Replaces the risks of a draft.
   *
   * @param values risk values (at least one)
   */
  public void replaceRisks(List<RiskValues> values) {
    workflow.requireEditable(label());
    UnderwritingRules.requireRisks(values);
    UnderwritingRules.requirePositiveSumsInsured(values);
    risks.clear();
    int line = 1;
    for (RiskValues v : values) {
      risks.add(new PolicyRisk(this, line++, v));
    }
  }

  /**
   * Stores the computed premium of the original issue.
   *
   * @param breakdown premium figures
   */
  public void applyPremium(PremiumBreakdown breakdown) {
    workflow.requireEditable(label());
    this.premium = breakdown;
  }

  /**
   * Maker submits the policy.
   *
   * @param user maker
   * @param when timestamp
   */
  public void submit(String user, Instant when) {
    UnderwritingRules.requireRisks(risks);
    workflow.submit(label(), user, when);
  }

  /**
   * Checker approves the policy (accounting is performed by the service in the same transaction).
   *
   * @param checker approver
   * @param when timestamp
   * @param accountingDate approval (accounting) date
   */
  public void approve(String checker, Instant when, LocalDate accountingDate) {
    workflow.approve(label(), getCreatedBy(), checker, when, accountingDate);
  }

  /**
   * Checker returns the policy to the maker.
   *
   * @param reason reason
   */
  public void reject(String reason) {
    workflow.reject(label(), reason);
  }

  /** Discards a draft policy. */
  public void discard() {
    workflow.discard(label());
  }

  /**
   * Records the accounting references produced on approval.
   *
   * @param postingRefs references
   */
  public void recordPosting(PostingRefs postingRefs) {
    this.refs = postingRefs;
  }

  /**
   * Cancels an approved policy (cancellation endorsement approved).
   *
   * @param effectiveDate cancellation date
   */
  public void cancel(LocalDate effectiveDate) {
    workflow.cancelApproved(label());
    this.cancelledOn = effectiveDate;
  }

  /**
   * Extends the policy to a renewed period (renewal endorsement approved).
   *
   * @param from new period start
   * @param to new period end
   */
  public void renew(LocalDate from, LocalDate to) {
    workflow.requireApproved(label(), "renew");
    UnderwritingRules.requirePeriod(from, to);
    this.periodFrom = from;
    this.periodTo = to;
  }

  /**
   * Whether the policy covers a date (approved, not cancelled before it).
   *
   * @param date date
   * @return true when in force
   */
  public boolean isInForce(LocalDate date) {
    return UnderwritingRules.inForce(workflow.getStatus(), cancelledOn, periodFrom, periodTo, date);
  }

  /**
   * Document label used in messages.
   *
   * @return "Policy &lt;no&gt;"
   */
  public String label() {
    return "Policy " + policyNo;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getPolicyNo() {
    return policyNo;
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

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public String getCurrency() {
    return currency;
  }

  public int getUwYear() {
    return uwYear;
  }

  public BusinessType getBusinessType() {
    return businessType;
  }

  public BigDecimal getSharePct() {
    return sharePct;
  }

  public Party getCoinsurer() {
    return coinsurer;
  }

  public boolean isCoinsuranceLeader() {
    return coinsuranceLeader;
  }

  public BigDecimal getDiscountRate() {
    return discountRate;
  }

  public BigDecimal getLoadingRate() {
    return loadingRate;
  }

  public OpenCover getOpenCover() {
    return openCover;
  }

  public Long getQuotationId() {
    return quotationId;
  }

  public LocalDate getCancelledOn() {
    return cancelledOn;
  }

  public ApprovalWorkflow getWorkflow() {
    return workflow;
  }

  public PolicyStatus getStatus() {
    return workflow.getStatus();
  }

  public PremiumBreakdown getPremium() {
    return premium;
  }

  /**
   * Accounting references (empty until approved; Hibernate loads an all-null embeddable as null).
   *
   * @return references, never null
   */
  public PostingRefs getRefs() {
    return refs == null ? new PostingRefs() : refs;
  }

  public List<PolicyRisk> getRisks() {
    return List.copyOf(risks);
  }
}
