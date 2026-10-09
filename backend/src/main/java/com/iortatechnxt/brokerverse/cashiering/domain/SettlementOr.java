package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * The commission or incentive OR of an insurer settlement kept until Disbursement approves the
 * payment request (FRS.CSH.07.01.01; Appendix R, C12): the request of the source module, the
 * payment request it waits for, and the OR issued.
 */
@Entity
@Table(name = "csh_settlement_or")
public class SettlementOr extends BaseEntity {

  /** Waiting for the approval of Disbursement. */
  public static final String PENDING = "PENDING";

  /** OR issued. */
  public static final String ISSUED = "ISSUED";

  /** Payment request cancelled by Disbursement. */
  public static final String CANCELLED = "CANCELLED";

  /** The OR could not be issued; issued again from the list. */
  public static final String FAILED = "FAILED";

  private static final int MESSAGE_MAX = 500;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "source_module", nullable = false, length = 30, updatable = false)
  private String sourceModule;

  @Column(name = "source_ref", nullable = false, length = 80, updatable = false)
  private String sourceRef;

  @Column(name = "await_ref", nullable = false, length = 80)
  private String awaitRef;

  @Column(name = "or_type", nullable = false, length = 30, updatable = false)
  private String orType;

  @Column(name = "payee_code", length = 40)
  private String payeeCode;

  @Column(name = "payee_name", length = 200)
  private String payeeName;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(name = "request_json", nullable = false, columnDefinition = "text")
  private String requestJson;

  @Column(nullable = false, length = 15)
  private String status = PENDING;

  @Column(name = "receipt_no", length = 40)
  private String receiptNo;

  @Column(length = MESSAGE_MAX)
  private String message;

  @Column(name = "issued_at")
  private Instant issuedAt;

  /** JPA. */
  protected SettlementOr() {}

  /**
   * A request kept.
   *
   * @param companyId company
   * @param sourceModule source module
   * @param sourceRef source reference
   * @param orType OR type
   * @param currency currency
   */
  public SettlementOr(
      Long companyId, String sourceModule, String sourceRef, String orType, String currency) {
    this.companyId = companyId;
    this.sourceModule = sourceModule;
    this.sourceRef = sourceRef;
    this.orType = orType;
    this.currency = currency;
  }

  /**
   * Keeps (again) the request until the approval of a payment request.
   *
   * @param waitFor reference of the payment request
   * @param payee payee code and name
   * @param total gross of the OR
   * @param request request as JSON
   */
  public void await(String waitFor, String[] payee, BigDecimal total, String request) {
    this.awaitRef = waitFor;
    this.payeeCode = payee[0];
    this.payeeName = payee[1];
    this.amount = total;
    this.requestJson = request;
    this.status = PENDING;
    this.message = "Waiting for the approval of Disbursement";
  }

  /**
   * The OR was issued.
   *
   * @param number OR number
   * @param at time
   */
  public void issued(String number, Instant at) {
    this.status = ISSUED;
    this.receiptNo = number;
    this.issuedAt = at;
    this.message = "Issued at Disbursement approval";
  }

  /**
   * The payment request was cancelled.
   *
   * @param reason reason
   */
  public void cancelled(String reason) {
    this.status = CANCELLED;
    this.message = cut("Payment request cancelled by Disbursement" + suffix(reason));
  }

  /**
   * The OR could not be issued.
   *
   * @param reason reason
   */
  public void failed(String reason) {
    this.status = FAILED;
    this.message = cut(reason);
  }

  private static String suffix(String reason) {
    return reason == null || reason.isBlank() ? "" : ": " + reason;
  }

  private static String cut(String text) {
    return text == null || text.length() <= MESSAGE_MAX ? text : text.substring(0, MESSAGE_MAX);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getAwaitRef() {
    return awaitRef;
  }

  public String getOrType() {
    return orType;
  }

  public String getPayeeCode() {
    return payeeCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getRequestJson() {
    return requestJson;
  }

  public String getStatus() {
    return status;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public String getMessage() {
    return message;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }
}
