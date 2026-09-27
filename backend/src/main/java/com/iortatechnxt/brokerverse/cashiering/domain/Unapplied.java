package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * An unapplied payment or excess waiting for a disposition (CSHID.024/025). The money stays in
 * unapplied collections (OPERATIONS_DESIGN section 5, row 5). The stage mirrors the workflow {@code
 * OPS_DISPOSITION}, whose stage groups are the tabs Unapplied / Monitoring / For Approval / For
 * Reversal.
 */
@Entity
@Table(name = "csh_unapplied")
public class Unapplied extends BaseEntity {

  /** Initial stage. */
  public static final String STAGE_INITIAL = "UNAPPLIED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(nullable = false, length = 30, updatable = false)
  private String reference;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private UnappliedOrigin origin;

  @Column(name = "receipt_id", updatable = false)
  private Long receiptId;

  @Column(name = "payment_id", updatable = false)
  private Long paymentId;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "client_code", length = 30)
  private String clientCode;

  @Column(name = "payor_name", length = 250)
  private String payorName;

  @Column(name = "sales_unit", length = 40)
  private String salesUnit;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  @Column(nullable = false, length = 20)
  private String stage = STAGE_INITIAL;

  @Column(name = "disposition_hint", length = 40)
  private String dispositionHint;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(length = 250)
  private String remarks;

  protected Unapplied() {}

  /**
   * Creates an item in the Unapplied tab.
   *
   * @param companyId company
   * @param branchId branch
   * @param reference UNP- reference
   * @param spec origin, links, party, money and source
   */
  public Unapplied(Long companyId, Long branchId, String reference, UnappliedSpec spec) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.reference = reference;
    this.origin = spec.origin();
    this.receiptId = spec.receiptId();
    this.paymentId = spec.paymentId();
    this.invoiceNo = spec.invoiceNo();
    this.clientCode = spec.clientCode();
    this.payorName = spec.payorName();
    this.salesUnit = spec.salesUnit();
    this.currency = spec.currency();
    this.amount = spec.amount();
    this.balance = spec.amount();
    this.dispositionHint = spec.dispositionHint();
    this.sourceModule = spec.sourceModule();
    this.sourceRef = spec.sourceRef();
    this.remarks = spec.remarks();
  }

  /**
   * Takes money off the balance (disposition executed, automatch, overages).
   *
   * @param value amount used
   */
  public void consume(BigDecimal value) {
    if (value.signum() <= 0 || value.compareTo(balance) > 0) {
      throw new BusinessRuleException(
          "UNAPPLIED_BALANCE_EXCEEDED",
          "Amount " + value + " is above the unapplied balance " + balance + " of " + reference);
    }
    balance = balance.subtract(value);
  }

  /**
   * Puts money back on the balance (disposition reversed).
   *
   * @param value amount restored
   */
  public void restore(BigDecimal value) {
    balance = balance.add(value).min(amount);
  }

  /**
   * Moves the money to another client or unit (reclass, transfer).
   *
   * @param client new client, null keeps it
   * @param unit new unit, null keeps it
   */
  public void reassign(String client, String unit) {
    if (client != null) {
      this.clientCode = client;
    }
    if (unit != null) {
      this.salesUnit = unit;
    }
  }

  /**
   * Mirrors the workflow stage.
   *
   * @param stageCode stage
   */
  public void markStage(String stageCode) {
    this.stage = stageCode;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getReference() {
    return reference;
  }

  public UnappliedOrigin getOrigin() {
    return origin;
  }

  public Long getReceiptId() {
    return receiptId;
  }

  public Long getPaymentId() {
    return paymentId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getPayorName() {
    return payorName;
  }

  public String getSalesUnit() {
    return salesUnit;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public String getStage() {
    return stage;
  }

  public String getDispositionHint() {
    return dispositionHint;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * What an unapplied item holds.
   *
   * @param origin origin
   * @param receiptId receipt, may be null
   * @param paymentId payment, may be null
   * @param invoiceNo invoice it came from, may be null
   * @param clientCode client, may be null
   * @param payorName payor, may be null
   * @param salesUnit marketing unit, may be null
   * @param currency currency
   * @param amount amount
   * @param dispositionHint suggested disposition, may be null
   * @param sourceModule source module (idempotency)
   * @param sourceRef source reference (idempotency)
   * @param remarks remarks
   */
  public record UnappliedSpec(
      UnappliedOrigin origin,
      Long receiptId,
      Long paymentId,
      String invoiceNo,
      String clientCode,
      String payorName,
      String salesUnit,
      String currency,
      BigDecimal amount,
      String dispositionHint,
      String sourceModule,
      String sourceRef,
      String remarks) {}
}
