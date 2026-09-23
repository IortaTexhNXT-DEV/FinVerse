package com.iortatechnxt.finverse.tax.domain;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A tax return for one form and filing period.
 *
 * <p>Lifecycle: DRAFT (figures computed from the worksheet and refreshable) → FILED (figures
 * frozen, filed by a user other than the preparer, with the eFPS / eBIRForms reference) → PAID
 * (remittance posted). Only a DRAFT can be CANCELLED; at most one non-cancelled return exists per
 * form and period (database index {@code tax_uq_return_period}).
 */
@Entity
@Table(name = "tax_return")
public class TaxReturn extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "return_no", nullable = false, length = 40)
  private String returnNo;

  @Column(name = "form_code", nullable = false, length = 20)
  private String formCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorksheetKind worksheet;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "tax_base", nullable = false, precision = 19, scale = 2)
  private BigDecimal taxBase = BigDecimal.ZERO;

  @Column(name = "tax_due", nullable = false, precision = 19, scale = 2)
  private BigDecimal taxDue = BigDecimal.ZERO;

  @Column(name = "tax_credits", nullable = false, precision = 19, scale = 2)
  private BigDecimal taxCredits = BigDecimal.ZERO;

  @Column(name = "amount_payable", nullable = false, precision = 19, scale = 2)
  private BigDecimal amountPayable = BigDecimal.ZERO;

  @Column(name = "excess_credit", nullable = false, precision = 19, scale = 2)
  private BigDecimal excessCredit = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private ReturnStatus status = ReturnStatus.DRAFT;

  @Column(name = "prepared_by", nullable = false, length = 50)
  private String preparedBy;

  @Column(name = "prepared_at", nullable = false)
  private Instant preparedAt;

  @Column(name = "filed_by", length = 50)
  private String filedBy;

  @Column(name = "filed_at")
  private Instant filedAt;

  @Column(name = "filed_on")
  private LocalDate filedOn;

  @Column(name = "filing_reference", length = 60)
  private String filingReference;

  @Column(name = "status_reason", length = 200)
  private String statusReason;

  @OneToMany(mappedBy = "taxReturn", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<TaxReturnLine> lines = new ArrayList<>();

  protected TaxReturn() {}

  /**
   * Creates a draft return.
   *
   * @param header identity of the return
   * @param preparer preparing user
   * @param when timestamp
   */
  public TaxReturn(ReturnHeader header, String preparer, Instant when) {
    this.companyId = header.companyId();
    this.returnNo = header.returnNo();
    this.formCode = header.formCode();
    this.worksheet = header.worksheet();
    this.periodStart = header.period().from();
    this.periodEnd = header.period().to();
    this.dueDate = header.dueDate();
    this.preparedBy = preparer;
    this.preparedAt = when;
  }

  /**
   * Removes the lines of a draft before a recomputation. Flush afterwards, so the line numbers are
   * free again when {@link #recompute} inserts the new lines.
   */
  public void clearLines() {
    requireStatus(ReturnStatus.DRAFT, "recompute");
    lines.clear();
  }

  /**
   * Sets the computed figures and lines of a draft (after {@link #clearLines()} for a refresh).
   *
   * @param figures headline figures
   * @param values summary lines
   * @param preparer user computing the draft (becomes the preparer)
   * @param when timestamp
   */
  public void recompute(
      ReturnFigures figures, List<ReturnLineValues> values, String preparer, Instant when) {
    requireStatus(ReturnStatus.DRAFT, "recompute");
    if (!lines.isEmpty()) {
      throw new BusinessRuleException(
          "RETURN_LINES_NOT_CLEARED", "Clear the lines of return " + returnNo + " first");
    }
    this.taxBase = figures.taxBase();
    this.taxDue = figures.taxDue();
    this.taxCredits = figures.taxCredits();
    this.amountPayable = figures.amountPayable();
    this.excessCredit = figures.excessCredit();
    this.preparedBy = preparer;
    this.preparedAt = when;
    int number = 1;
    for (ReturnLineValues v : values) {
      lines.add(new TaxReturnLine(this, number++, v));
    }
  }

  /**
   * Records the filing (checker action).
   *
   * @param filer filing user, who must not be the preparer
   * @param when timestamp
   * @param date filing date
   * @param reference eFPS / eBIRForms confirmation number
   */
  public void file(String filer, Instant when, LocalDate date, String reference) {
    requireStatus(ReturnStatus.DRAFT, "file");
    if (Objects.equals(filer, preparedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A return cannot be filed by the user who prepared it");
    }
    if (date.isBefore(periodStart)) {
      throw new BusinessRuleException(
          "INVALID_FILING_DATE", "Filing date precedes the start of the period");
    }
    this.filedBy = filer;
    this.filedAt = when;
    this.filedOn = date;
    this.filingReference = reference;
    this.status = ReturnStatus.FILED;
  }

  /** Marks a filed return as paid (the remittance has been recorded). */
  public void markPaid() {
    requireStatus(ReturnStatus.FILED, "pay");
    this.status = ReturnStatus.PAID;
  }

  /**
   * Cancels a draft.
   *
   * @param reason reason
   */
  public void cancel(String reason) {
    requireStatus(ReturnStatus.DRAFT, "cancel");
    this.statusReason = reason;
    this.status = ReturnStatus.CANCELLED;
  }

  /**
   * Whether the return is past due on a date and not yet paid.
   *
   * @param date date
   * @return true when overdue
   */
  public boolean isOverdueOn(LocalDate date) {
    return status != ReturnStatus.PAID && status != ReturnStatus.CANCELLED && date.isAfter(dueDate);
  }

  /**
   * The filing period.
   *
   * @return period
   */
  public TaxPeriod period() {
    return new TaxPeriod(periodStart, periodEnd);
  }

  private void requireStatus(ReturnStatus expected, String action) {
    if (status != expected) {
      throw new BusinessRuleException(
          "INVALID_RETURN_STATUS",
          "Cannot " + action + " return " + returnNo + " in status " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getReturnNo() {
    return returnNo;
  }

  public String getFormCode() {
    return formCode;
  }

  public WorksheetKind getWorksheet() {
    return worksheet;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public BigDecimal getTaxBase() {
    return taxBase;
  }

  public BigDecimal getTaxDue() {
    return taxDue;
  }

  public BigDecimal getTaxCredits() {
    return taxCredits;
  }

  public BigDecimal getAmountPayable() {
    return amountPayable;
  }

  public BigDecimal getExcessCredit() {
    return excessCredit;
  }

  public ReturnStatus getStatus() {
    return status;
  }

  public String getPreparedBy() {
    return preparedBy;
  }

  public Instant getPreparedAt() {
    return preparedAt;
  }

  public String getFiledBy() {
    return filedBy;
  }

  public Instant getFiledAt() {
    return filedAt;
  }

  public LocalDate getFiledOn() {
    return filedOn;
  }

  public String getFilingReference() {
    return filingReference;
  }

  public String getStatusReason() {
    return statusReason;
  }

  public List<TaxReturnLine> getLines() {
    return Collections.unmodifiableList(lines);
  }
}
