package com.iortatechnxt.brokerverse.collections.promise.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A client's promise to pay an amount on a collection account (an invoice), or on one of its
 * installments, by a date (BRCLXN.055, CQ16). The job {@code CLX_PROMISE_CHECK} compares it with
 * the payments applied in the ledger and marks it kept, partially kept or broken; broken promises
 * feed the escalation rules (BRCLXN.049).
 */
@Entity
@Table(name = "clx_promise")
public class PaymentPromise extends BaseEntity {

  private static final int SCALE = 2;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
  private String assuredName;

  @Column(name = "installment_id", updatable = false)
  private Long installmentId;

  @Column(name = "promised_on", nullable = false, updatable = false)
  private LocalDate promisedOn;

  @Column(name = "promised_date", nullable = false, updatable = false)
  private LocalDate promisedDate;

  @Column(name = "promised_amount", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal promisedAmount;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(length = 500, updatable = false)
  private String remarks;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PromiseStatus status = PromiseStatus.OPEN;

  @Column(name = "evaluated_at")
  private Instant evaluatedAt;

  @Column(name = "actual_paid", precision = 19, scale = 2)
  private BigDecimal actualPaid;

  @Column(name = "actual_date")
  private LocalDate actualDate;

  @Column(name = "closing_note", length = 500)
  private String closingNote;

  @Column(name = "bulk_ref", length = 40, updatable = false)
  private String bulkRef;

  protected PaymentPromise() {}

  /**
   * Records a promise.
   *
   * @param account the collection account
   * @param terms dates, amount and installment
   * @param remarks remarks
   * @param bulkRef bulk upload or bulk action reference, may be null
   */
  public PaymentPromise(Account account, Terms terms, String remarks, String bulkRef) {
    if (terms.promisedDate().isBefore(terms.promisedOn())) {
      throw new BusinessRuleException(
          "CLX_PROMISE_DATES", "The promised date cannot be before the day of the promise");
    }
    if (terms.amount().signum() <= 0) {
      throw new BusinessRuleException("CLX_PROMISE_AMOUNT", "The promised amount must be positive");
    }
    this.companyId = account.companyId();
    this.invoiceNo = account.invoiceNo();
    this.arn = account.arn();
    this.clientCode = account.clientCode();
    this.assuredName = account.assuredName();
    this.currency = account.currency();
    this.installmentId = terms.installmentId();
    this.promisedOn = terms.promisedOn();
    this.promisedDate = terms.promisedDate();
    this.promisedAmount = terms.amount().setScale(SCALE, RoundingMode.HALF_UP);
    this.remarks = remarks;
    this.bulkRef = bulkRef;
  }

  /**
   * Closes an open promise with the outcome of its evaluation.
   *
   * @param outcome kept, partially kept or broken, with the payments found
   * @param at evaluation time
   */
  public void evaluate(Outcome outcome, Instant at) {
    requireOpen();
    this.status = outcome.status();
    this.actualPaid = outcome.paid().setScale(SCALE, RoundingMode.HALF_UP);
    this.actualDate = outcome.lastPaymentDate();
    this.evaluatedAt = at;
  }

  /**
   * Cancels an open promise (withdrawn, or replaced by a new promise).
   *
   * @param note why
   * @param at time
   */
  public void cancel(String note, Instant at) {
    requireOpen();
    this.status = PromiseStatus.CANCELLED;
    this.closingNote = note;
    this.evaluatedAt = at;
  }

  private void requireOpen() {
    if (status != PromiseStatus.OPEN) {
      throw new BusinessRuleException(
          "CLX_PROMISE_CLOSED", "The promise of " + invoiceNo + " is already " + status);
    }
  }

  /**
   * The last day a payment counts for the promise.
   *
   * @param graceDays CLX_PROMISE_GRACE_DAYS
   * @return promised date plus the grace days
   */
  public LocalDate deadline(int graceDays) {
    return promisedDate.plusDays(graceDays);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getArn() {
    return arn;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public Long getInstallmentId() {
    return installmentId;
  }

  public LocalDate getPromisedOn() {
    return promisedOn;
  }

  public LocalDate getPromisedDate() {
    return promisedDate;
  }

  public BigDecimal getPromisedAmount() {
    return promisedAmount;
  }

  public String getCurrency() {
    return currency;
  }

  public String getRemarks() {
    return remarks;
  }

  public PromiseStatus getStatus() {
    return status;
  }

  public Instant getEvaluatedAt() {
    return evaluatedAt;
  }

  public BigDecimal getActualPaid() {
    return actualPaid;
  }

  public LocalDate getActualDate() {
    return actualDate;
  }

  public String getClosingNote() {
    return closingNote;
  }

  public String getBulkRef() {
    return bulkRef;
  }

  /**
   * The collection account a promise is about.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param arn account
   * @param clientCode client
   * @param assuredName assured
   * @param currency currency
   */
  public record Account(
      Long companyId,
      String invoiceNo,
      String arn,
      String clientCode,
      String assuredName,
      String currency) {}

  /**
   * What was promised.
   *
   * @param promisedOn day the client made the promise
   * @param promisedDate day the payment is promised for
   * @param amount promised amount
   * @param installmentId installment, may be null
   */
  public record Terms(
      LocalDate promisedOn, LocalDate promisedDate, BigDecimal amount, Long installmentId) {}

  /**
   * The evaluation of a promise.
   *
   * @param status KEPT, PARTIALLY_KEPT or BROKEN
   * @param paid payments applied in the window
   * @param lastPaymentDate value date of the last of them, may be null
   */
  public record Outcome(PromiseStatus status, BigDecimal paid, LocalDate lastPaymentDate) {}
}
