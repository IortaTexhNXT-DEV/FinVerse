package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PdcStatus;
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
 * A post-dated check in the warehouse (CSHID.008 item 4): stored with a traceable {@code PDCW-}
 * number, it becomes a payment with an AR at maturity ({@code PDC_MATURITY} job) or is returned,
 * replaced or pulled out before.
 */
@Entity
@Table(name = "csh_pdc_item")
public class PdcItem extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(name = "warehouse_no", nullable = false, length = 30, updatable = false)
  private String warehouseNo;

  @Column(name = "batch_ref", length = 40, updatable = false)
  private String batchRef;

  @Column(name = "client_code", length = 30, updatable = false)
  private String clientCode;

  @Column(name = "payor_name", nullable = false, length = 250, updatable = false)
  private String payorName;

  @Column(nullable = false, length = 80, updatable = false)
  private String reference;

  @Column(name = "check_no", nullable = false, length = 40, updatable = false)
  private String checkNo;

  @Column(name = "bank_code", nullable = false, length = 30, updatable = false)
  private String bankCode;

  @Column(name = "check_branch", length = 60, updatable = false)
  private String checkBranch;

  @Column(name = "maturity_date", nullable = false, updatable = false)
  private LocalDate maturityDate;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(length = 40, updatable = false)
  private String segment;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PdcStatus status = PdcStatus.WAREHOUSED;

  @Column(name = "status_reason", length = 250)
  private String statusReason;

  @Column(name = "payment_id")
  private Long paymentId;

  @Column(name = "receipt_no", length = 40)
  private String receiptNo;

  protected PdcItem() {}

  /**
   * Warehouses a check.
   *
   * @param companyId company
   * @param branchId branch
   * @param warehouseNo PDCW- number
   * @param check check details
   */
  public PdcItem(Long companyId, Long branchId, String warehouseNo, PdcCheck check) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.warehouseNo = warehouseNo;
    this.batchRef = check.batchRef();
    this.clientCode = check.clientCode();
    this.payorName = check.payorName();
    this.reference = check.reference();
    this.checkNo = check.checkNo();
    this.bankCode = check.bankCode();
    this.checkBranch = check.checkBranch();
    this.maturityDate = check.maturityDate();
    this.amount = check.amount();
    this.currency = check.currency();
    this.segment = check.segment();
  }

  /**
   * Records the payment and AR created at maturity.
   *
   * @param payment payment id
   * @param receipt AR number
   * @param applied whether the payment was applied to an invoice
   */
  public void matured(Long payment, String receipt, boolean applied) {
    requireWarehoused();
    this.paymentId = payment;
    this.receiptNo = receipt;
    this.status = applied ? PdcStatus.APPLIED : PdcStatus.MATURED;
  }

  /**
   * Takes the check out of the warehouse before maturity.
   *
   * @param outcome RETURNED, REPLACED or PULLED_OUT
   * @param reason reason
   */
  public void release(PdcStatus outcome, String reason) {
    requireWarehoused();
    if (outcome == PdcStatus.WAREHOUSED
        || outcome == PdcStatus.MATURED
        || outcome == PdcStatus.APPLIED) {
      throw new BusinessRuleException(
          "PDC_RELEASE_STATUS", "A check leaves the warehouse as returned, replaced or pulled out");
    }
    this.status = outcome;
    this.statusReason = reason;
  }

  private void requireWarehoused() {
    if (status != PdcStatus.WAREHOUSED) {
      throw new BusinessRuleException(
          "PDC_NOT_WAREHOUSED",
          "Check " + warehouseNo + " is " + status + ", not in the warehouse");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getWarehouseNo() {
    return warehouseNo;
  }

  public String getBatchRef() {
    return batchRef;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getPayorName() {
    return payorName;
  }

  public String getReference() {
    return reference;
  }

  public String getCheckNo() {
    return checkNo;
  }

  public String getBankCode() {
    return bankCode;
  }

  public String getCheckBranch() {
    return checkBranch;
  }

  public LocalDate getMaturityDate() {
    return maturityDate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public String getSegment() {
    return segment;
  }

  public PdcStatus getStatus() {
    return status;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public Long getPaymentId() {
    return paymentId;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  /**
   * A post-dated check.
   *
   * @param batchRef upload job, may be null
   * @param clientCode client, may be null
   * @param payorName payor
   * @param reference invoice, ARN, policy or PN number to match at maturity
   * @param checkNo check number
   * @param bankCode bank code
   * @param checkBranch bank branch, may be null
   * @param maturityDate maturity date
   * @param amount amount
   * @param currency currency
   * @param segment market segment, may be null
   */
  public record PdcCheck(
      String batchRef,
      String clientCode,
      String payorName,
      String reference,
      String checkNo,
      String bankCode,
      String checkBranch,
      LocalDate maturityDate,
      BigDecimal amount,
      String currency,
      String segment) {}
}
