package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A versioned decision matrix (BRRN.031/034): rules by priority propose a disposition and whether
 * it is applied automatically. Every system decision stores the matrix version and the rule id.
 */
@Entity
@Table(name = "rnw_decision_matrix")
public class DecisionMatrix extends VersionedRules {

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "matrix_id", nullable = false)
  @OrderBy("priority")
  private final List<DecisionRule> rules = new ArrayList<>();

  protected DecisionMatrix() {}

  /**
   * Starts a draft.
   *
   * @param companyId company
   * @param versionNo version
   * @param effectiveFrom effective date
   * @param description description
   */
  public DecisionMatrix(
      Long companyId, int versionNo, LocalDate effectiveFrom, String description) {
    super(companyId, versionNo, effectiveFrom, description);
  }

  /**
   * Replaces the rules of the draft.
   *
   * @param newRules rules
   */
  public void replaceRules(List<DecisionRule.Data> newRules) {
    requireDraft();
    rules.clear();
    newRules.forEach(d -> rules.add(new DecisionRule(d)));
  }

  public List<DecisionRule> getRules() {
    return rules;
  }
}
