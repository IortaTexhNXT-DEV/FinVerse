package com.iortatechnxt.brokerverse.payables.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Petty cash disbursement voucher: an expense paid in cash from a fund. Approved vouchers are
 * posted (Dr expense / Cr petty cash) and wait for reimbursement.
 */
@Entity
@Table(name = "pay_petty_cash_disbursement")
public class PettyCashDisbursement extends PettyCashDocument {

  @Column(name = "disbursement_date", nullable = false)
  private LocalDate disbursementDate;

  @Column(nullable = false, length = 120)
  private String payee;

  @Column(name = "expense_account_code", nullable = false, length = 30)
  private String expenseAccountCode;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(name = "receipt_ref", length = 40)
  private String receiptRef;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "reimbursement_id")
  private Long reimbursementId;

  protected PettyCashDisbursement() {}

  /**
   * Captures a disbursement voucher (pending approval).
   *
   * @param fund fund paying the cash
   * @param documentNo document number
   * @param values voucher values
   */
  public PettyCashDisbursement(
      PettyCashFund fund, String documentNo, PettyCashDisbursementValues values) {
    super(fund, documentNo);
    this.disbursementDate = values.date();
    this.payee = values.payee();
    this.expenseAccountCode = values.expenseAccountCode();
    this.costCenter = values.costCenter();
    this.description = values.description();
    this.receiptRef = values.receiptRef();
    this.amount = values.amount();
  }

  /**
   * Links the voucher to a reimbursement claim.
   *
   * @param claimId claim
   */
  public void claim(Long claimId) {
    if (getStatus() != PettyCashStatus.APPROVED || reimbursementId != null) {
      throw new BusinessRuleException(
          "NOT_REIMBURSABLE",
          "Voucher " + getDocumentNo() + " is not an unclaimed approved voucher");
    }
    reimbursementId = claimId;
  }

  /** Releases the voucher from a rejected reimbursement claim. */
  public void unclaim() {
    reimbursementId = null;
  }

  public LocalDate getDisbursementDate() {
    return disbursementDate;
  }

  public String getPayee() {
    return payee;
  }

  public String getExpenseAccountCode() {
    return expenseAccountCode;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getDescription() {
    return description;
  }

  public String getReceiptRef() {
    return receiptRef;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Long getReimbursementId() {
    return reimbursementId;
  }
}
