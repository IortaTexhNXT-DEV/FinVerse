package com.iortatechnxt.brokerverse.payables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Post-dated cheque issued (PDC register entry), created when a PDC payment voucher is approved.
 *
 * <p>Status flow: ISSUED → DUE (cheque date reached) → PRESENTED (confirmed; PDC liability cleared
 * against the bank) → CLEARED (matched with the bank statement). An outstanding cheque can be
 * CANCELLED (payment reversed) or REPLACED by a new cheque for the same payment. Dates of every
 * transition are kept so the register can be reproduced as of any date.
 */
@Entity
@Table(name = "pay_pdc_issued")
public class IssuedPdc extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "voucher_id", nullable = false)
  private Long voucherId;

  @Column(name = "bank_account_id", nullable = false)
  private Long bankAccountId;

  @Column(name = "party_id", nullable = false)
  private Long partyId;

  @Column(name = "party_code", nullable = false, length = 30)
  private String partyCode;

  @Column(name = "payee_name", nullable = false, length = 200)
  private String payeeName;

  @Column(name = "cheque_no", nullable = false, length = 20)
  private String chequeNo;

  @Column(name = "cheque_date", nullable = false)
  private LocalDate chequeDate;

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(length = 20)
  private String department;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IssuedPdcStatus status = IssuedPdcStatus.ISSUED;

  @Column(name = "presented_on")
  private LocalDate presentedOn;

  @Column(name = "presentation_batch_no", length = 40)
  private String presentationBatchNo;

  @Column(name = "cleared_on")
  private LocalDate clearedOn;

  @Column(name = "cancelled_on")
  private LocalDate cancelledOn;

  @Column(name = "cancel_batch_no", length = 40)
  private String cancelBatchNo;

  @Column(name = "replaced_on")
  private LocalDate replacedOn;

  @Column(name = "replaces_id")
  private Long replacesId;

  @Column(name = "replaced_by_id")
  private Long replacedById;

  @Column(length = 200)
  private String remarks;

  protected IssuedPdc() {}

  /**
   * Registers a cheque from an approved PDC voucher.
   *
   * @param v voucher (approved, mode PDC)
   */
  public IssuedPdc(PaymentVoucher v) {
    this.companyId = v.getCompanyId();
    this.branchId = v.getBranchId();
    this.voucherId = v.getId();
    this.bankAccountId = v.getBankAccountId();
    this.partyId = v.getPartyId();
    this.partyCode = v.getPartyCode();
    this.payeeName = v.getPayeeName();
    this.chequeNo = v.getChequeNo();
    this.chequeDate = v.getChequeDate();
    this.issueDate = v.getVoucherDate();
    this.currency = v.getCurrency();
    this.amount = v.getAmount();
    this.baseAmount = v.getBaseAmount();
    this.department = v.getDepartment();
  }

  /**
   * Creates the replacement of a cheque.
   *
   * @param original cheque being replaced
   * @param newChequeNo new cheque number
   * @param newChequeDate new cheque date
   * @param issuedOn replacement date
   * @return new register entry
   */
  public static IssuedPdc replacementOf(
      IssuedPdc original, String newChequeNo, LocalDate newChequeDate, LocalDate issuedOn) {
    IssuedPdc copy = new IssuedPdc();
    copy.companyId = original.companyId;
    copy.branchId = original.branchId;
    copy.voucherId = original.voucherId;
    copy.bankAccountId = original.bankAccountId;
    copy.partyId = original.partyId;
    copy.partyCode = original.partyCode;
    copy.payeeName = original.payeeName;
    copy.currency = original.currency;
    copy.amount = original.amount;
    copy.baseAmount = original.baseAmount;
    copy.department = original.department;
    copy.chequeNo = newChequeNo;
    copy.chequeDate = newChequeDate;
    copy.issueDate = issuedOn;
    copy.replacesId = original.getId();
    return copy;
  }

  /**
   * Marks the cheque due when its date is reached.
   *
   * @param asOf date
   * @return true when the status changed
   */
  public boolean markDueIfReached(LocalDate asOf) {
    if (status == IssuedPdcStatus.ISSUED && !chequeDate.isAfter(asOf)) {
      status = IssuedPdcStatus.DUE;
      return true;
    }
    return false;
  }

  /**
   * Confirms presentation.
   *
   * @param date presentation (bank) date, not before the cheque date
   * @param batchNo presentation journal
   */
  public void present(LocalDate date, String batchNo) {
    requireOutstanding("present");
    if (date.isBefore(chequeDate)) {
      throw new BusinessRuleException(
          "PDC_NOT_MATURED", "Cheque " + chequeNo + " is dated " + chequeDate);
    }
    status = IssuedPdcStatus.PRESENTED;
    presentedOn = date;
    presentationBatchNo = batchNo;
  }

  /**
   * Records that the presentation appears on the bank statement.
   *
   * @param date clearing date
   */
  public void clear(LocalDate date) {
    if (status != IssuedPdcStatus.PRESENTED) {
      throw new BusinessRuleException("PDC_NOT_PRESENTED", "Cheque " + chequeNo + " is " + status);
    }
    status = IssuedPdcStatus.CLEARED;
    clearedOn = date;
  }

  /**
   * Stops the cheque.
   *
   * @param date cancellation date
   * @param batchNo reversal journal
   * @param reason reason
   */
  public void cancel(LocalDate date, String batchNo, String reason) {
    requireOutstanding("cancel");
    status = IssuedPdcStatus.CANCELLED;
    cancelledOn = date;
    cancelBatchNo = batchNo;
    remarks = reason;
  }

  /**
   * Marks the cheque replaced.
   *
   * @param date replacement date
   * @param replacement new register entry
   * @param reason reason
   */
  public void replaceWith(LocalDate date, IssuedPdc replacement, String reason) {
    requireOutstanding("replace");
    status = IssuedPdcStatus.REPLACED;
    replacedOn = date;
    replacedById = replacement.getId();
    remarks = reason;
  }

  private void requireOutstanding(String action) {
    if (!status.isOutstanding()) {
      throw new BusinessRuleException(
          "PDC_NOT_OUTSTANDING", "Cannot " + action + " cheque " + chequeNo + " in " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public Long getVoucherId() {
    return voucherId;
  }

  public Long getBankAccountId() {
    return bankAccountId;
  }

  public Long getPartyId() {
    return partyId;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getChequeNo() {
    return chequeNo;
  }

  public LocalDate getChequeDate() {
    return chequeDate;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public String getDepartment() {
    return department;
  }

  public IssuedPdcStatus getStatus() {
    return status;
  }

  public LocalDate getPresentedOn() {
    return presentedOn;
  }

  public String getPresentationBatchNo() {
    return presentationBatchNo;
  }

  public LocalDate getClearedOn() {
    return clearedOn;
  }

  public LocalDate getCancelledOn() {
    return cancelledOn;
  }

  public String getCancelBatchNo() {
    return cancelBatchNo;
  }

  public LocalDate getReplacedOn() {
    return replacedOn;
  }

  public Long getReplacesId() {
    return replacesId;
  }

  public Long getReplacedById() {
    return replacedById;
  }

  public String getRemarks() {
    return remarks;
  }
}
