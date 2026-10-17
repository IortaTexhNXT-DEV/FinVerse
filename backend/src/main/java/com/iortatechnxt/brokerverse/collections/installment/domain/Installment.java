package com.iortatechnxt.brokerverse.collections.installment.domain;

import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.InstallmentStatus;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * One installment of a plan = one billing cycle (BRCLXN.053/058): its due date, the coverage period
 * it bills, the amount and the payments allocated to it from the invoice ledger, oldest due first
 * (BRCLXN.054). The status is derived from the due date and the allocation.
 */
@Entity
@Table(name = "clx_installment")
public class Installment extends BaseEntity {

  private static final int SCALE = 2;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plan_id", nullable = false, updatable = false)
  private InstallmentPlan plan;

  @Column(nullable = false, updatable = false)
  private int seq;

  @Column(name = "policy_year", nullable = false, updatable = false)
  private int policyYear;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(name = "due_date", nullable = false, updatable = false)
  private LocalDate dueDate;

  @Column(name = "cycle_from", nullable = false, updatable = false)
  private LocalDate cycleFrom;

  @Column(name = "cycle_to", nullable = false, updatable = false)
  private LocalDate cycleTo;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal paidAmount = BigDecimal.ZERO.setScale(SCALE);

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InstallmentStatus status = InstallmentStatus.NOT_DUE;

  @Column(name = "overdue_since")
  private LocalDate overdueSince;

  @Column(name = "paid_on")
  private LocalDate paidOn;

  protected Installment() {}

  Installment(InstallmentPlan plan, Terms terms) {
    this.plan = plan;
    this.seq = terms.seq();
    this.policyYear = terms.policyYear();
    this.invoiceNo = terms.invoiceNo();
    this.dueDate = terms.dueDate();
    this.cycleFrom = terms.cycleFrom();
    this.cycleTo = terms.cycleTo();
    this.amount = terms.amount().setScale(SCALE, RoundingMode.HALF_UP);
  }

  /**
   * Records the payments allocated to the installment and derives its status as of a date.
   *
   * @param paid amount allocated (capped at the installment amount)
   * @param asOf business date
   */
  public void allocate(BigDecimal paid, LocalDate asOf) {
    paidAmount = paid.min(amount).max(BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
    InstallmentStatus next = statusOf(asOf);
    if (next != InstallmentStatus.PAID) {
      paidOn = null;
    } else if (status != InstallmentStatus.PAID) {
      paidOn = asOf;
    }
    status = next;
    overdueSince = status == InstallmentStatus.OVERDUE ? dueDate.plusDays(1) : null;
  }

  private InstallmentStatus statusOf(LocalDate asOf) {
    if (paidAmount.compareTo(amount) >= 0) {
      return InstallmentStatus.PAID;
    }
    if (dueDate.isBefore(asOf)) {
      return InstallmentStatus.OVERDUE;
    }
    if (paidAmount.signum() > 0) {
      return InstallmentStatus.PARTIAL;
    }
    return dueDate.isEqual(asOf) ? InstallmentStatus.DUE : InstallmentStatus.NOT_DUE;
  }

  /**
   * Links the invoice once a scheduled policy year is booked.
   *
   * @param number invoice number
   */
  public void linkInvoice(String number) {
    this.invoiceNo = number;
  }

  /**
   * What is still to pay.
   *
   * @return amount - paid
   */
  public BigDecimal balance() {
    return amount.subtract(paidAmount);
  }

  public InstallmentPlan getPlan() {
    return plan;
  }

  public int getSeq() {
    return seq;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public LocalDate getCycleFrom() {
    return cycleFrom;
  }

  public LocalDate getCycleTo() {
    return cycleTo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getPaidAmount() {
    return paidAmount;
  }

  public InstallmentStatus getStatus() {
    return status;
  }

  public LocalDate getOverdueSince() {
    return overdueSince;
  }

  public LocalDate getPaidOn() {
    return paidOn;
  }

  /**
   * The terms of an installment.
   *
   * @param seq sequence in the plan, from 1
   * @param policyYear policy year it bills
   * @param invoiceNo invoice billed; null for a policy year still scheduled in booking
   * @param dueDate due date
   * @param cycleFrom first day of the billing cycle (coverage period)
   * @param cycleTo last day of the billing cycle
   * @param amount amount
   */
  public record Terms(
      int seq,
      int policyYear,
      String invoiceNo,
      LocalDate dueDate,
      LocalDate cycleFrom,
      LocalDate cycleTo,
      BigDecimal amount) {}
}
