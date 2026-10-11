package com.iortatechnxt.brokerverse.payables.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Petty cash document of a fund (disbursement voucher or reimbursement claim): captured pending
 * approval, then approved (posted) or rejected by a checker.
 */
@MappedSuperclass
public abstract class PettyCashDocument extends ApprovableDocument {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "fund_id", nullable = false)
  private Long fundId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "document_no", nullable = false, length = 40)
  private String documentNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PettyCashStatus status = PettyCashStatus.PENDING_APPROVAL;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  /** JPA constructor. */
  protected PettyCashDocument() {}

  /**
   * Binds the document to its fund.
   *
   * @param fund fund
   * @param number document number
   */
  protected PettyCashDocument(PettyCashFund fund, String number) {
    this.companyId = fund.getCompanyId();
    this.fundId = fund.getId();
    this.branchId = fund.getBranchId();
    this.documentNo = number;
  }

  /**
   * Approves the document after posting.
   *
   * @param checker checker
   * @param when timestamp
   * @param batchNo journal
   */
  public void approve(String checker, Instant when, String batchNo) {
    requirePending();
    recordApproval(checker, when);
    journalBatchNo = batchNo;
    status = PettyCashStatus.APPROVED;
  }

  /**
   * Rejects the document (nothing is posted).
   *
   * @param checker checker
   * @param reason reason
   */
  public void reject(String checker, String reason) {
    requirePending();
    requireChecker(checker);
    recordReason(reason);
    status = PettyCashStatus.REJECTED;
  }

  private void requirePending() {
    if (status != PettyCashStatus.PENDING_APPROVAL) {
      throw new BusinessRuleException(
          "INVALID_PETTY_CASH_STATUS", "Document " + documentNo + " is " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getFundId() {
    return fundId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public PettyCashStatus getStatus() {
    return status;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }
}
