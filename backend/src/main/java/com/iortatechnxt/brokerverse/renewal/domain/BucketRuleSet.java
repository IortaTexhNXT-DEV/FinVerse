package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
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
 * A versioned bucket rule set (BRRN.023): rules by priority turn the check results into Clean,
 * Review or Exception. A rule that maps a failed check to Clean cannot be saved (database check and
 * this guard).
 */
@Entity
@Table(name = "rnw_bucket_rule_set")
public class BucketRuleSet extends VersionedRules {

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "rule_set_id", nullable = false)
  @OrderBy("priority")
  private final List<BucketRule> rules = new ArrayList<>();

  protected BucketRuleSet() {}

  /**
   * Starts a draft.
   *
   * @param companyId company
   * @param versionNo version
   * @param effectiveFrom effective date
   * @param description description
   */
  public BucketRuleSet(Long companyId, int versionNo, LocalDate effectiveFrom, String description) {
    super(companyId, versionNo, effectiveFrom, description);
  }

  /**
   * Replaces the rules of the draft.
   *
   * @param newRules rules
   */
  public void replaceRules(List<BucketRule.Data> newRules) {
    requireDraft();
    for (BucketRule.Data d : newRules) {
      if (d.outcome() == CheckOutcome.FAIL && d.bucket() == Bucket.CLEAN) {
        throw new BusinessRuleException(
            "RNW_BUCKET_RULE_FAIL_CLEAN", "A failed check cannot give the Clean bucket");
      }
    }
    rules.clear();
    newRules.forEach(d -> rules.add(new BucketRule(d)));
  }

  public List<BucketRule> getRules() {
    return rules;
  }
}
