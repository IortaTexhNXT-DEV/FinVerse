package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Change to an approved policy: additional or return premium, renewal, cancellation or a
 * non-financial (NIL) amendment. Carries its own premium figures (the change, negative for
 * returns), approval workflow and debit/credit note, and the underwriting year of the policy period
 * it belongs to: a renewal the year its new period starts, any other endorsement the year of the
 * period in force when it is made (the original issue's, or the latest renewal's).
 */
@Entity
@Table(name = "uw_endorsement")
public class Endorsement extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "policy_id")
  private Policy policy;

  @Column(name = "endorsement_no", nullable = false)
  private int endorsementNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "endorsement_type", nullable = false, length = 20)
  private EndorsementType endorsementType;

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(name = "new_period_from")
  private LocalDate newPeriodFrom;

  @Column(name = "new_period_to")
  private LocalDate newPeriodTo;

  @Column(name = "uw_year", nullable = false)
  private int uwYear;

  @Column(nullable = false, length = 500)
  private String description;

  @Embedded private final ApprovalWorkflow workflow = new ApprovalWorkflow();

  @Embedded private PremiumBreakdown premium = new PremiumBreakdown();

  @Embedded private PostingRefs refs = new PostingRefs();

  protected Endorsement() {}

  /**
   * Creates a draft endorsement.
   *
   * @param policy approved policy
   * @param endorsementNo sequence within the policy (1, 2...)
   * @param type endorsement type
   * @param issueDate issue date
   * @param effectiveDate effective date of the change
   * @param description description of the change
   */
  public Endorsement(
      Policy policy,
      int endorsementNo,
      EndorsementType type,
      LocalDate issueDate,
      LocalDate effectiveDate,
      String description) {
    policy.getWorkflow().requireApproved(policy.label(), "endorse");
    if (effectiveDate.isBefore(policy.getPeriodFrom())
        || type != EndorsementType.RENEWAL && effectiveDate.isAfter(policy.getPeriodTo())) {
      throw new BusinessRuleException(
          "INVALID_EFFECTIVE_DATE", "Effective date must fall within the policy period");
    }
    this.policy = policy;
    this.endorsementNo = endorsementNo;
    this.endorsementType = type;
    this.issueDate = issueDate;
    this.effectiveDate = effectiveDate;
    this.description = description;
    // Only one endorsement may be open, so the policy header holds the period in force.
    this.uwYear = UnderwritingRules.underwritingYear(policy.getPeriodFrom());
  }

  /**
   * Sets the renewed period (renewal endorsements).
   *
   * @param from new period start
   * @param to new period end
   */
  public void renewalPeriod(LocalDate from, LocalDate to) {
    if (endorsementType != EndorsementType.RENEWAL) {
      throw new BusinessRuleException(
          "NOT_A_RENEWAL", "Only renewal endorsements carry a new period");
    }
    UnderwritingRules.requirePeriod(from, to);
    if (!from.isAfter(policy.getPeriodTo())) {
      throw new BusinessRuleException(
          "INVALID_RENEWAL_PERIOD", "The renewed period must start after the current period");
    }
    this.newPeriodFrom = from;
    this.newPeriodTo = to;
    this.uwYear = UnderwritingRules.underwritingYear(from);
  }

  /**
   * Stores the computed premium change.
   *
   * @param breakdown premium figures
   */
  public void applyPremium(PremiumBreakdown breakdown) {
    workflow.requireEditable(label());
    this.premium = breakdown;
  }

  /**
   * Maker submits the endorsement.
   *
   * @param user maker
   * @param when timestamp
   */
  public void submit(String user, Instant when) {
    workflow.submit(label(), user, when);
  }

  /**
   * Checker approves the endorsement and applies its effect on the policy (cancellation, renewal).
   *
   * @param checker approver
   * @param when timestamp
   * @param accountingDate approval (accounting) date
   */
  public void approve(String checker, Instant when, LocalDate accountingDate) {
    policy.getWorkflow().requireApproved(policy.label(), "endorse");
    workflow.approve(label(), getCreatedBy(), checker, when, accountingDate);
    if (endorsementType == EndorsementType.CANCELLATION) {
      policy.cancel(effectiveDate);
    } else if (endorsementType == EndorsementType.RENEWAL) {
      policy.renew(newPeriodFrom, newPeriodTo);
    }
  }

  /**
   * Checker returns the endorsement to the maker.
   *
   * @param reason reason
   */
  public void reject(String reason) {
    workflow.reject(label(), reason);
  }

  /** Discards a draft endorsement. */
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
   * Printable endorsement number.
   *
   * @return e.g. "P-FIRE-HO-2026-000001/E01"
   */
  public String documentNo() {
    return String.format("%s/E%02d", policy.getPolicyNo(), endorsementNo);
  }

  /**
   * Document label used in messages.
   *
   * @return "Endorsement &lt;no&gt;"
   */
  public String label() {
    return "Endorsement " + documentNo();
  }

  public Policy getPolicy() {
    return policy;
  }

  public int getEndorsementNo() {
    return endorsementNo;
  }

  public EndorsementType getEndorsementType() {
    return endorsementType;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  public LocalDate getNewPeriodFrom() {
    return newPeriodFrom;
  }

  public LocalDate getNewPeriodTo() {
    return newPeriodTo;
  }

  /**
   * Underwriting year of the policy period this endorsement belongs to (for a renewal: the year its
   * new period starts).
   *
   * @return underwriting year
   */
  public int getUwYear() {
    return uwYear;
  }

  public String getDescription() {
    return description;
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
}
