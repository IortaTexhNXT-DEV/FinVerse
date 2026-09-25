package com.iortatechnxt.brokerverse.commission.domain;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Sanitation;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * A direct payment account of a DP list (CMRID.001-013, MKTID.012): what the branch submitted, the
 * commission receivable computed from the Operations ledger (commission, VAT, withholding tax, net,
 * CMRID.007), the sanitation and rule results, the tag through billing, insurer feedback,
 * collection with its OR, and the premium receivable reversal.
 */
@Entity
@Table(name = "cmr_dp_item")
public class DpItem extends BaseEntity {

  private static final int MAX_RESULTS = 2000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "list_id", updatable = false)
  private Long listId;

  @Column(name = "row_no", updatable = false)
  private Integer rowNo;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "policy_no", length = 60)
  private String policyNo;

  @Column(name = "insurer_code", length = 30)
  private String insurerCode;

  @Column(name = "client_code", length = 30)
  private String clientCode;

  @Column(name = "assured_name", length = 250)
  private String assuredName;

  @Column(name = "branch_code", length = 60)
  private String branchCode;

  @Column(name = "submitted_premium", precision = 19, scale = 2)
  private BigDecimal submittedPremium;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal premium = BigDecimal.ZERO;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal commission = BigDecimal.ZERO;

  @Column(name = "commission_vat", nullable = false, precision = 19, scale = 2)
  private BigDecimal commissionVat = BigDecimal.ZERO;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal wtax = BigDecimal.ZERO;

  @Column(name = "net_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal netCommission = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private DpTag tag;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Sanitation sanitation;

  @Column(name = "rule_results", length = MAX_RESULTS)
  private String ruleResults;

  @Column(name = "duplicate_of")
  private Long duplicateOf;

  @Column(length = 500)
  private String remarks;

  @Column(name = "billing_id")
  private Long billingId;

  @Column(name = "confirmed_by", length = 50)
  private String confirmedBy;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "feedback_reason", length = 40)
  private String feedbackReason;

  @Column(name = "feedback_comment", length = 500)
  private String feedbackComment;

  @Column(name = "responded_at")
  private Instant respondedAt;

  @Column(name = "returned_ref", length = 40)
  private String returnedRef;

  @Column(name = "or_no", length = 40)
  private String orNo;

  @Column(name = "collected_amount", precision = 19, scale = 2)
  private BigDecimal collectedAmount;

  @Column(name = "collected_on")
  private LocalDate collectedOn;

  @Column(name = "pr_reversed_at")
  private Instant prReversedAt;

  @Column(name = "reversal_count", nullable = false)
  private int reversalCount;

  @Column(name = "reinstated_count", nullable = false)
  private int reinstatedCount;

  protected DpItem() {}

  /**
   * An account as submitted.
   *
   * @param companyId company
   * @param listId list, null for a manual entry
   * @param rowNo row of the list
   * @param submission submitted fields
   */
  public DpItem(Long companyId, Long listId, Integer rowNo, Submission submission) {
    this.companyId = companyId;
    this.listId = listId;
    this.rowNo = rowNo;
    this.invoiceNo = submission.invoiceNo();
    this.policyNo = submission.policyNo();
    this.insurerCode = submission.insurerCode();
    this.submittedPremium = submission.premium();
    this.remarks = submission.remarks();
    this.branchCode = submission.branchCode();
    this.tag = DpTag.EXCLUDED;
    this.sanitation = Sanitation.INCOMPLETE;
  }

  /**
   * Takes the ledger facts and the commission receivable (CMRID.007).
   *
   * @param facts ledger facts
   */
  public void computed(LedgerFacts facts) {
    this.policyNo = facts.policyNo() == null ? policyNo : facts.policyNo();
    this.insurerCode = facts.insurerCode();
    this.clientCode = facts.clientCode();
    this.assuredName = facts.assuredName();
    this.premium = facts.premium();
    this.commission = facts.commission();
    this.commissionVat = facts.commissionVat();
    this.wtax = facts.wtax();
    this.netCommission = facts.commission().add(facts.commissionVat()).subtract(facts.wtax());
  }

  /**
   * Records the sanitation (CMRID.002/013): valid accounts wait for confirmation, others are
   * excluded.
   *
   * @param result sanitation
   * @param results rule results, one per line
   * @param duplicate the account this one duplicates, may be null
   */
  public void sanitized(Sanitation result, String results, Long duplicate) {
    this.sanitation = result;
    this.ruleResults =
        results == null || results.length() <= MAX_RESULTS
            ? results
            : results.substring(0, MAX_RESULTS);
    this.duplicateOf = duplicate;
    this.tag = result == Sanitation.VALID ? DpTag.DP_FOR_CONFIRMATION : DpTag.EXCLUDED;
  }

  /**
   * Confirms that the premium is fully paid to the insurer (CMRID.013 reviewer confirmation).
   *
   * @param by reviewer
   * @param at time
   */
  public void confirm(String by, Instant at) {
    require(Set.of(DpTag.DP_FOR_CONFIRMATION), "confirmed");
    this.tag = DpTag.DP_FOR_BILLING;
    this.confirmedBy = by;
    this.confirmedAt = at;
  }

  /**
   * Excludes an account by hand.
   *
   * @param reason reason
   */
  public void exclude(String reason) {
    require(Set.of(DpTag.DP_FOR_CONFIRMATION, DpTag.DP_FOR_BILLING), "excluded");
    if (billingId != null) {
      throw new BusinessRuleException(
          "DP_ITEM_BILLED", "Account " + invoiceNo + " is on a billing: cancel the billing first");
    }
    this.tag = DpTag.EXCLUDED;
    this.remarks = reason;
  }

  /**
   * Puts the account on a billing.
   *
   * @param billing billing
   */
  public void assign(Long billing) {
    require(Set.of(DpTag.DP_FOR_BILLING), "billed");
    this.billingId = billing;
  }

  /** Takes the account off a cancelled billing. */
  public void release() {
    this.billingId = null;
    this.tag = DpTag.DP_FOR_BILLING;
  }

  /** The billing was sent to the insurer. */
  public void billed() {
    this.tag = DpTag.BILLED;
  }

  /**
   * Records the insurer's answer (CMRID.009).
   *
   * @param approved approved or rejected
   * @param reason reason (LOV DP_FEEDBACK_REASON), may be null when approved
   * @param comment comment
   * @param at time
   */
  public void answered(boolean approved, String reason, String comment, Instant at) {
    require(Set.of(DpTag.BILLED), "answered");
    this.tag = approved ? DpTag.APPROVED : DpTag.REJECTED;
    this.feedbackReason = reason;
    this.feedbackComment = comment;
    this.respondedAt = at;
  }

  /**
   * Records the return of a rejected account to Collection (CMRID.009).
   *
   * @param ref Collection feed run
   */
  public void returned(String ref) {
    this.returnedRef = ref;
  }

  /**
   * Records the commission collected (CMRID.010).
   *
   * @param or OR number, null while the OR is handed over
   * @param amount net amount collected
   * @param on collection date
   */
  public void collected(String or, BigDecimal amount, LocalDate on) {
    require(Set.of(DpTag.APPROVED), "collected");
    this.tag = DpTag.COLLECTED;
    this.orNo = or;
    this.collectedAmount = amount;
    this.collectedOn = on;
  }

  /**
   * Records the premium receivable reversal (MKTID.012).
   *
   * @param at time
   * @return the sequence of the reversal
   */
  public int reversed(Instant at) {
    require(Set.of(DpTag.COLLECTED), "reversed");
    this.tag = DpTag.PR_REVERSED;
    this.prReversedAt = at;
    this.reversalCount++;
    return reversalCount;
  }

  /**
   * Reinstates the premium receivable (CSHID.004 b).
   *
   * @return the sequence of the reinstatement
   */
  public int reinstated() {
    require(Set.of(DpTag.PR_REVERSED), "reinstated");
    this.tag = DpTag.COLLECTED;
    this.prReversedAt = null;
    this.reinstatedCount++;
    return reinstatedCount;
  }

  private void require(Set<DpTag> tags, String action) {
    if (!tags.contains(tag)) {
      throw new BusinessRuleException(
          "DP_ITEM_STATE", "Account " + invoiceNo + " (" + tag + ") cannot be " + action);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getListId() {
    return listId;
  }

  public Integer getRowNo() {
    return rowNo;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getBranchCode() {
    return branchCode;
  }

  public BigDecimal getSubmittedPremium() {
    return submittedPremium;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public BigDecimal getCommissionVat() {
    return commissionVat;
  }

  public BigDecimal getWtax() {
    return wtax;
  }

  public BigDecimal getNetCommission() {
    return netCommission;
  }

  public DpTag getTag() {
    return tag;
  }

  public Sanitation getSanitation() {
    return sanitation;
  }

  public String getRuleResults() {
    return ruleResults;
  }

  public Long getDuplicateOf() {
    return duplicateOf;
  }

  public String getRemarks() {
    return remarks;
  }

  public Long getBillingId() {
    return billingId;
  }

  public String getConfirmedBy() {
    return confirmedBy;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public String getFeedbackReason() {
    return feedbackReason;
  }

  public String getFeedbackComment() {
    return feedbackComment;
  }

  public Instant getRespondedAt() {
    return respondedAt;
  }

  public String getReturnedRef() {
    return returnedRef;
  }

  public String getOrNo() {
    return orNo;
  }

  public BigDecimal getCollectedAmount() {
    return collectedAmount;
  }

  public LocalDate getCollectedOn() {
    return collectedOn;
  }

  public Instant getPrReversedAt() {
    return prReversedAt;
  }

  public int getReversalCount() {
    return reversalCount;
  }

  public int getReinstatedCount() {
    return reinstatedCount;
  }

  /**
   * What a branch submits for an account.
   *
   * @param invoiceNo invoice number
   * @param policyNo policy number, may be null
   * @param insurerCode insurer, may be null
   * @param premium premium paid to the insurer, may be null
   * @param remarks remarks
   * @param branchCode submitting branch
   */
  public record Submission(
      String invoiceNo,
      String policyNo,
      String insurerCode,
      BigDecimal premium,
      String remarks,
      String branchCode) {}

  /**
   * Facts of the ledger invoice.
   *
   * @param policyNo policy number
   * @param insurerCode lead insurer
   * @param clientCode client
   * @param assuredName assured
   * @param premium gross premium
   * @param commission commission
   * @param commissionVat VAT on commission
   * @param wtax withholding tax on commission
   */
  public record LedgerFacts(
      String policyNo,
      String insurerCode,
      String clientCode,
      String assuredName,
      BigDecimal premium,
      BigDecimal commission,
      BigDecimal commissionVat,
      BigDecimal wtax) {}
}
