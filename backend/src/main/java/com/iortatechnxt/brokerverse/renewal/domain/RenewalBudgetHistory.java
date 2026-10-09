package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One change of a budget amount (FRRN.042.06): the field, the previous and the updated value, the
 * user and the time. Insert-only.
 */
@Entity
@Table(name = "rnw_budget_history")
public class RenewalBudgetHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "budget_id", nullable = false, updatable = false)
  private Long budgetId;

  @Column(nullable = false, length = 60, updatable = false)
  private String field;

  @Column(updatable = false)
  private BigDecimal previous;

  @Column(updatable = false)
  private BigDecimal updated;

  @Column(name = "modified_by", nullable = false, length = 50, updatable = false)
  private String modifiedBy;

  @Column(name = "modified_at", nullable = false, updatable = false)
  private Instant modifiedAt;

  protected RenewalBudgetHistory() {}

  /**
   * Records a change.
   *
   * @param budgetId budget record
   * @param field the amount changed, e.g. "Renewal budget - March"
   * @param values previous and updated value
   * @param by user
   * @param at time
   */
  public RenewalBudgetHistory(
      Long budgetId, String field, BigDecimal[] values, String by, Instant at) {
    this.budgetId = budgetId;
    this.field = field;
    this.previous = values[0];
    this.updated = values[1];
    this.modifiedBy = by;
    this.modifiedAt = at;
  }

  public Long getId() {
    return id;
  }

  public Long getBudgetId() {
    return budgetId;
  }

  public String getField() {
    return field;
  }

  public BigDecimal getPrevious() {
    return previous;
  }

  public BigDecimal getUpdated() {
    return updated;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public Instant getModifiedAt() {
    return modifiedAt;
  }
}
