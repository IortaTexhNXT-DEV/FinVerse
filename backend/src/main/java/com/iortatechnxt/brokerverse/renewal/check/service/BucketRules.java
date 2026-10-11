package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRule;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRuleSet;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRuleSetRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSeverity;
import com.iortatechnxt.brokerverse.renewal.domain.RuleSetStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Turns check results into a bucket (BRRN.023; RENEWAL_DESIGN section 8.2) with the active bucket
 * rule set, or with the default rules while none is active: any FAIL of severity FAIL_EXCEPTION
 * gives Exception; any FAIL_REVIEW or WARN gives Review; otherwise Clean. The worst bucket of the
 * findings wins, and a failed check never gives Clean (service guard next to the database check).
 */
@Component
public class BucketRules {

  private final BucketRuleSetRepository ruleSets;

  /**
   * Creates the rules.
   *
   * @param ruleSets bucket rule sets
   */
  public BucketRules(BucketRuleSetRepository ruleSets) {
    this.ruleSets = ruleSets;
  }

  /**
   * The bucket of a set of findings.
   *
   * @param companyId company
   * @param findings findings of a check run
   * @param today business date
   * @return bucket with the rule set version and the rule that decided it
   */
  public Decision bucketOf(Long companyId, List<Finding> findings, LocalDate today) {
    return bucketOf(companyId, findings, today, null);
  }

  /**
   * The bucket of a set of findings for a renewal already initiated: the rule set version that
   * classified it at initiation keeps applying, so that a new version applies only to the renewals
   * not yet initiated (Annex BRRN.020; FR-RN-112 R3). Without a pinned version, or when that
   * version no longer exists, the active rule set applies.
   *
   * @param companyId company
   * @param findings findings of a check run
   * @param today business date
   * @param pinnedVersion rule set version of the renewal, null when not initiated
   * @return bucket with the rule set version and the rule that decided it
   */
  public Decision bucketOf(
      Long companyId, List<Finding> findings, LocalDate today, Integer pinnedVersion) {
    Optional<BucketRuleSet> active =
        pinned(companyId, pinnedVersion)
            .or(
                () ->
                    ruleSets.findByCompanyIdAndStatus(companyId, RuleSetStatus.ACTIVE).stream()
                        .filter(r -> !r.getEffectiveFrom().isAfter(today))
                        .findFirst());
    Decision decision =
        new Decision(Bucket.CLEAN, active.map(r -> r.getVersionNo()).orElse(null), null);
    for (Finding f : findings) {
      if (!f.countsForBucket()) {
        continue;
      }
      Decision one =
          active
              .map(set -> byRules(set, f))
              .orElseGet(() -> new Decision(defaultBucket(f), null, null));
      decision = worst(decision, one);
    }
    boolean failed = findings.stream().anyMatch(f -> f.countsForBucket() && f.failed());
    if (failed && decision.bucket() == Bucket.CLEAN) {
      decision = new Decision(Bucket.REVIEW, decision.ruleSetVersion(), decision.ruleId());
    }
    return decision;
  }

  private Optional<BucketRuleSet> pinned(Long companyId, Integer version) {
    if (version == null) {
      return Optional.empty();
    }
    return ruleSets.findByCompanyIdOrderByVersionNoDesc(companyId).stream()
        .filter(
            r ->
                version.equals(r.getVersionNo())
                    && (r.getStatus() == RuleSetStatus.ACTIVE
                        || r.getStatus() == RuleSetStatus.RETIRED))
        .findFirst();
  }

  private static Decision byRules(BucketRuleSet set, Finding f) {
    for (BucketRule rule : set.getRules()) {
      if (rule.matches(f.code(), f.outcome(), f.severity())) {
        return new Decision(rule.getResultBucket(), set.getVersionNo(), rule.getId());
      }
    }
    return new Decision(defaultBucket(f), set.getVersionNo(), null);
  }

  private static Bucket defaultBucket(Finding f) {
    if (f.outcome() == CheckOutcome.FAIL && f.severity() == CheckSeverity.FAIL_EXCEPTION) {
      return Bucket.EXCEPTION;
    }
    return Bucket.REVIEW;
  }

  private static Decision worst(Decision a, Decision b) {
    return b.bucket().ordinal() > a.bucket().ordinal() ? b : a;
  }

  /**
   * The bucket decided and why.
   *
   * @param bucket bucket
   * @param ruleSetVersion active rule set version, null for the default rules
   * @param ruleId rule that decided, null when none
   */
  public record Decision(Bucket bucket, Integer ruleSetVersion, Long ruleId) {}
}
