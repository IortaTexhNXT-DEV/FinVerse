package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.FundingStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Funding of the main BDOIR account through BDO Business Online Banking (DIS 2.17.0-2.17.4, AQ10):
 * source and target bank accounts, amount, purpose, the BOB reference and the maker, verifier and
 * two approvers of workflow {@code DISB_FUNDING}. Four eyes: the verifier is not the maker, the two
 * approvers differ from each other, from the maker and from the verifier.
 */
@Entity
@Table(name = "dsb_funding_request")
public class FundingRequest extends BaseEntity {

  private static final String FOUR_EYES = "FUNDING_FOUR_EYES";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "funding_no", nullable = false, length = 30, updatable = false)
  private String fundingNo;

  @Column(name = "source_bank_account_id", nullable = false)
  private Long sourceBankAccountId;

  @Column(name = "target_bank_account_id", nullable = false)
  private Long targetBankAccountId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, length = 500)
  private String purpose;

  @Column(name = "value_date", nullable = false)
  private LocalDate valueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FundingStage stage = FundingStage.CREATED;

  @Column(name = "verified_by", length = 50)
  private String verifiedBy;

  @Column(name = "first_approver", length = 50)
  private String firstApprover;

  @Column(name = "second_approver", length = 50)
  private String secondApprover;

  @Column(name = "bob_reference", length = 80)
  private String bobReference;

  @Column(name = "journal_no", length = 30)
  private String journalNo;

  protected FundingRequest() {}

  /**
   * A new request.
   *
   * @param companyId company
   * @param fundingNo number ({@code FND-<yyyy>-nnnnnn})
   * @param terms accounts, amount and purpose
   */
  public FundingRequest(Long companyId, String fundingNo, FundingTerms terms) {
    this.companyId = companyId;
    this.fundingNo = fundingNo;
    apply(terms);
  }

  /**
   * Changes the terms while the request is with the maker.
   *
   * @param terms terms
   */
  public void update(FundingTerms terms) {
    if (stage != FundingStage.CREATED) {
      throw new BusinessRuleException(
          "FUNDING_NOT_EDITABLE", "Funding request " + fundingNo + " is " + stage);
    }
    apply(terms);
  }

  private void apply(FundingTerms terms) {
    if (terms.sourceBankAccountId().equals(terms.targetBankAccountId())) {
      throw new BusinessRuleException(
          "FUNDING_SAME_ACCOUNT", "The source and target accounts must differ");
    }
    sourceBankAccountId = terms.sourceBankAccountId();
    targetBankAccountId = terms.targetBankAccountId();
    amount = terms.amount();
    currency = terms.currency();
    purpose = terms.purpose();
    valueDate = terms.valueDate();
    bobReference = terms.bobReference();
  }

  /**
   * The verifier: a team leader other than the maker (DIS 2.17.2-2.17.3).
   *
   * @param user verifier
   */
  public void verifiedBy(String user) {
    if (CurrentUser.sameUser(user, getCreatedBy())) {
      throw new BusinessRuleException(FOUR_EYES, "The maker cannot verify the funding request");
    }
    verifiedBy = user;
  }

  /**
   * One of the two approvers (DIS 2.17.4): neither the maker nor the verifier, and the second
   * approver differs from the first.
   *
   * @param user approver
   */
  public void approvedBy(String user) {
    if (CurrentUser.sameUser(user, getCreatedBy()) || CurrentUser.sameUser(user, verifiedBy)) {
      throw new BusinessRuleException(
          FOUR_EYES, "The maker or verifier cannot approve the funding request");
    }
    if (stage == FundingStage.FOR_APPROVAL_1) {
      firstApprover = user;
    } else if (CurrentUser.sameUser(user, firstApprover)) {
      throw new BusinessRuleException(
          FOUR_EYES, "The second approval must be given by another approver");
    } else {
      secondApprover = user;
    }
  }

  /**
   * Records the BOB reference of the transfer (DIS 2.17.1).
   *
   * @param reference BOB reference
   */
  public void bobReference(String reference) {
    bobReference = reference;
  }

  /**
   * Mirrors the workflow stage.
   *
   * @param next stage
   */
  public void markStage(FundingStage next) {
    stage = next;
  }

  /**
   * The transfer was posted.
   *
   * @param batchNo journal batch
   */
  public void posted(String batchNo) {
    journalNo = batchNo;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getFundingNo() {
    return fundingNo;
  }

  public Long getSourceBankAccountId() {
    return sourceBankAccountId;
  }

  public Long getTargetBankAccountId() {
    return targetBankAccountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public String getPurpose() {
    return purpose;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public FundingStage getStage() {
    return stage;
  }

  public String getVerifiedBy() {
    return verifiedBy;
  }

  public String getFirstApprover() {
    return firstApprover;
  }

  public String getSecondApprover() {
    return secondApprover;
  }

  public String getBobReference() {
    return bobReference;
  }

  public String getJournalNo() {
    return journalNo;
  }

  /**
   * Terms of a funding request.
   *
   * @param sourceBankAccountId account debited
   * @param targetBankAccountId main BDOIR account credited
   * @param amount amount
   * @param currency currency
   * @param purpose purpose
   * @param valueDate value date
   * @param bobReference BOB reference, may be null until the transfer is done
   */
  public record FundingTerms(
      Long sourceBankAccountId,
      Long targetBankAccountId,
      BigDecimal amount,
      String currency,
      String purpose,
      LocalDate valueDate,
      String bobReference) {}
}
