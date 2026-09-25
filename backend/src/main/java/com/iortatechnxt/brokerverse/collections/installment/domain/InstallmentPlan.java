package com.iortatechnxt.brokerverse.collections.installment.domain;

import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.InstallmentStatus;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanSource;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Hibernate;

/**
 * An installment plan of a collection account (BRCLXN.053/058, CQ15): the schedule of billing
 * cycles of a multi-year account (one or more installments per policy-year invoice) or of one
 * invoice, with the payments allocated to each installment. A plan is monitoring only: it never
 * changes booking, the ledger or the GL (BRCLXN.060).
 */
@Entity
@Table(name = "clx_installment_plan")
public class InstallmentPlan extends BaseEntity {

  private static final int SCALE = 2;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "plan_no", nullable = false, length = 40, updatable = false)
  private String planNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
  private String assuredName;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, length = 40, updatable = false)
  private String frequency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private PlanSource source;

  @Column(name = "first_due", nullable = false, updatable = false)
  private LocalDate firstDue;

  @Column(name = "installment_count", nullable = false, updatable = false)
  private int installmentCount;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal total;

  @Column(name = "paid_total", nullable = false, precision = 19, scale = 2)
  private BigDecimal paidTotal = BigDecimal.ZERO.setScale(SCALE);

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PlanStatus status = PlanStatus.ACTIVE;

  @Column(length = 500, updatable = false)
  private String remarks;

  @Column(name = "cancel_reason", length = 500)
  private String cancelReason;

  @Column(name = "refreshed_at")
  private Instant refreshedAt;

  @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("seq")
  private final List<Installment> installments = new ArrayList<>();

  protected InstallmentPlan() {}

  /**
   * A new active plan.
   *
   * @param header account, frequency and source
   * @param terms installments in due-date order (at least one)
   * @return the plan
   */
  public static InstallmentPlan create(Header header, List<Installment.Terms> terms) {
    if (terms.isEmpty()) {
      throw new BusinessRuleException("CLX_PLAN_EMPTY", "A plan needs at least one installment");
    }
    InstallmentPlan plan = new InstallmentPlan();
    plan.companyId = header.companyId();
    plan.planNo = header.planNo();
    plan.arn = header.arn();
    plan.invoiceNo = header.invoiceNo();
    plan.clientCode = header.clientCode();
    plan.assuredName = header.assuredName();
    plan.currency = header.currency();
    plan.frequency = header.frequency();
    plan.source = header.source();
    plan.remarks = header.remarks();
    BigDecimal sum = BigDecimal.ZERO;
    for (Installment.Terms t : terms) {
      Installment installment = new Installment(plan, t);
      plan.installments.add(installment);
      sum = sum.add(installment.getAmount());
    }
    plan.firstDue = terms.get(0).dueDate();
    plan.installmentCount = terms.size();
    plan.total = sum;
    return plan;
  }

  /**
   * Cancels a live plan.
   *
   * @param reason why
   */
  public void cancel(String reason) {
    if (status != PlanStatus.ACTIVE) {
      throw new BusinessRuleException(
          "CLX_PLAN_NOT_ACTIVE", "Plan " + planNo + " is " + status + " and cannot be cancelled");
    }
    this.status = PlanStatus.CANCELLED;
    this.cancelReason = reason;
  }

  /**
   * Totals the allocation after the installments were allocated; a plan whose installments are all
   * paid is completed.
   *
   * @param at time of the refresh
   */
  public void refreshed(Instant at) {
    this.paidTotal =
        installments.stream()
            .map(Installment::getPaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(SCALE);
    this.refreshedAt = at;
    if (status == PlanStatus.ACTIVE
        && installments.stream().allMatch(i -> i.getStatus() == InstallmentStatus.PAID)) {
      this.status = PlanStatus.COMPLETED;
    }
  }

  /**
   * The installment of a billing cycle.
   *
   * @param seq cycle
   * @return installment
   */
  public Installment installment(int seq) {
    return installments.stream()
        .filter(i -> i.getSeq() == seq)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "CLX_CYCLE_UNKNOWN", "Plan " + planNo + " has no billing cycle " + seq));
  }

  /**
   * Whether the plan is followed up.
   *
   * @return true when active
   */
  public boolean isActive() {
    return status == PlanStatus.ACTIVE;
  }

  /** Loads the installments (reads outside the persistence context). */
  public void loadInstallments() {
    Hibernate.initialize(installments);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getPlanNo() {
    return planNo;
  }

  public String getArn() {
    return arn;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getCurrency() {
    return currency;
  }

  public String getFrequency() {
    return frequency;
  }

  public PlanSource getSource() {
    return source;
  }

  public LocalDate getFirstDue() {
    return firstDue;
  }

  public int getInstallmentCount() {
    return installmentCount;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public BigDecimal getPaidTotal() {
    return paidTotal;
  }

  public PlanStatus getStatus() {
    return status;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public Instant getRefreshedAt() {
    return refreshedAt;
  }

  public List<Installment> getInstallments() {
    return installments;
  }

  /**
   * The account facts of a plan.
   *
   * @param companyId company
   * @param planNo plan number
   * @param arn account
   * @param invoiceNo invoice (null for a policy-year plan)
   * @param clientCode client
   * @param assuredName assured
   * @param currency currency
   * @param frequency billing frequency (LOV code)
   * @param source source
   * @param remarks remarks
   */
  public record Header(
      Long companyId,
      String planNo,
      String arn,
      String invoiceNo,
      String clientCode,
      String assuredName,
      String currency,
      String frequency,
      PlanSource source,
      String remarks) {}
}
