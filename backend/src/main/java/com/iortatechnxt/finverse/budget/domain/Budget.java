package com.iortatechnxt.finverse.budget.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Budget version of a company for one fiscal year (header of the budget grid).
 *
 * <p>Enforces the {@link BudgetStatus} lifecycle, including maker-checker segregation: the user who
 * submitted a version can never approve it.
 */
@Entity
@Table(name = "bud_budget")
public class Budget extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "fiscal_year", nullable = false)
  private int fiscalYear;

  @Column(name = "version_no", nullable = false)
  private int versionNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "version_type", nullable = false, length = 10)
  private BudgetVersionType versionType;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BudgetStatus status = BudgetStatus.DRAFT;

  @Column(name = "based_on_id")
  private Long basedOnId;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "rejection_reason", length = 200)
  private String rejectionReason;

  @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("accountCode, costCenter")
  private final List<BudgetLine> lines = new ArrayList<>();

  protected Budget() {}

  /**
   * Creates a draft budget version.
   *
   * @param header header values
   */
  public Budget(BudgetHeader header) {
    this.companyId = header.companyId();
    this.fiscalYear = header.fiscalYear();
    this.versionNo = header.versionNo();
    this.versionType = header.versionType();
    this.name = header.name();
    this.currency = header.currency();
    this.basedOnId = header.basedOnId();
  }

  /**
   * Replaces all lines (grid save, import, copy from actuals). Only DRAFT or REJECTED versions are
   * editable; editing a rejected version returns it to DRAFT.
   *
   * @param values new lines
   */
  public void replaceLines(List<BudgetLineValues> values) {
    requireEditable();
    // Update matching lines in place: the (account, cost centre) key is unique and Hibernate
    // flushes inserts before orphan deletes.
    Map<String, BudgetLineValues> byKey = new LinkedHashMap<>();
    values.forEach(v -> byKey.put(BudgetLine.key(v.accountCode(), v.costCenter()), v));
    lines.removeIf(l -> !byKey.containsKey(l.key()));
    for (BudgetLine line : lines) {
      line.update(byKey.remove(line.key()));
    }
    byKey.values().forEach(v -> lines.add(new BudgetLine(this, v)));
    status = BudgetStatus.DRAFT;
    rejectionReason = null;
  }

  /**
   * Submits the version for approval.
   *
   * @param user maker
   * @param when timestamp
   */
  public void submit(String user, Instant when) {
    requireEditable();
    if (lines.isEmpty()) {
      throw new BusinessRuleException("BUDGET_EMPTY", "A budget needs at least one line");
    }
    status = BudgetStatus.SUBMITTED;
    submittedBy = user;
    submittedAt = when;
    rejectionReason = null;
  }

  /**
   * Approves the version (checker).
   *
   * @param user checker
   * @param when timestamp
   */
  public void approve(String user, Instant when) {
    requireSubmitted();
    if (Objects.equals(submittedBy, user)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A budget cannot be approved by the user who submitted it");
    }
    status = BudgetStatus.APPROVED;
    approvedBy = user;
    approvedAt = when;
  }

  /**
   * Rejects the version back to its maker.
   *
   * @param reason reason
   */
  public void reject(String reason) {
    requireSubmitted();
    status = BudgetStatus.REJECTED;
    rejectionReason = reason;
  }

  /** Marks an approved version as replaced by a newer approved revision. */
  public void supersede() {
    if (status == BudgetStatus.APPROVED) {
      status = BudgetStatus.SUPERSEDED;
    }
  }

  /**
   * Annual total of all lines.
   *
   * @return total
   */
  public BigDecimal total() {
    return lines.stream().map(BudgetLine::annual).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void requireEditable() {
    if (status != BudgetStatus.DRAFT && status != BudgetStatus.REJECTED) {
      throw new BusinessRuleException(
          "BUDGET_NOT_EDITABLE", "Budget version " + versionNo + " is " + status);
    }
  }

  private void requireSubmitted() {
    if (status != BudgetStatus.SUBMITTED) {
      throw new BusinessRuleException(
          "BUDGET_NOT_SUBMITTED", "Budget version " + versionNo + " is " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public int getFiscalYear() {
    return fiscalYear;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public BudgetVersionType getVersionType() {
    return versionType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCurrency() {
    return currency;
  }

  public BudgetStatus getStatus() {
    return status;
  }

  public Long getBasedOnId() {
    return basedOnId;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public List<BudgetLine> getLines() {
    return lines;
  }
}
