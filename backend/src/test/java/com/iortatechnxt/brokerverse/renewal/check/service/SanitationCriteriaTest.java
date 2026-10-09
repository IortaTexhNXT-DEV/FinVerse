package com.iortatechnxt.brokerverse.renewal.check.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRule;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRuleSet;
import com.iortatechnxt.brokerverse.renewal.domain.BucketRuleSetRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckSeverity;
import com.iortatechnxt.brokerverse.renewal.domain.RuleSetStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Sanitation criteria of the Walkthrough addendum (FR-RN-020, 112). */
class SanitationCriteriaTest {

  @Test
  void theTsiThresholdIsReadFromTheCheckSetting() {
    assertThat(TsiThresholdCheck.parse(null)).isEqualByComparingTo("250000000");
    assertThat(TsiThresholdCheck.parse("300,000,000")).isEqualByComparingTo("300000000");
    assertThat(TsiThresholdCheck.parse("amount=1000"))
        .isEqualByComparingTo(BigDecimal.valueOf(1000));
    assertThat(TsiThresholdCheck.parse("not a number")).isEqualByComparingTo("250000000");
  }

  @Test
  void aNewRuleSetVersionDoesNotReclassifyAnInitiatedRenewal() {
    BucketRuleSetRepository repository = mock(BucketRuleSetRepository.class);
    BucketRuleSet retired = mock(BucketRuleSet.class);
    when(retired.getVersionNo()).thenReturn(1);
    when(retired.getStatus()).thenReturn(RuleSetStatus.RETIRED);
    when(retired.getRules()).thenReturn(List.of());
    BucketRuleSet active = mock(BucketRuleSet.class);
    when(active.getVersionNo()).thenReturn(2);
    when(active.getStatus()).thenReturn(RuleSetStatus.ACTIVE);
    when(active.getEffectiveFrom()).thenReturn(LocalDate.of(2020, 1, 1));
    BucketRule exception = mock(BucketRule.class);
    when(exception.matches("CLAIMS", CheckOutcome.FAIL, CheckSeverity.FAIL_REVIEW))
        .thenReturn(true);
    when(exception.getResultBucket()).thenReturn(Bucket.EXCEPTION);
    when(active.getRules()).thenReturn(List.of(exception));
    when(repository.findByCompanyIdOrderByVersionNoDesc(anyLong()))
        .thenReturn(List.of(active, retired));
    when(repository.findByCompanyIdAndStatus(1L, RuleSetStatus.ACTIVE)).thenReturn(List.of(active));
    BucketRules rules = new BucketRules(repository);
    List<Finding> claims =
        List.of(
            new Finding(
                "CLAIMS", CheckOutcome.FAIL, CheckSeverity.FAIL_REVIEW, "1 open claim", null));
    LocalDate today = LocalDate.of(2027, 3, 1);

    BucketRules.Decision fresh = rules.bucketOf(1L, claims, today);
    assertThat(fresh.bucket()).isEqualTo(Bucket.EXCEPTION);
    assertThat(fresh.ruleSetVersion()).isEqualTo(2);

    BucketRules.Decision pinned = rules.bucketOf(1L, claims, today, 1);
    assertThat(pinned.bucket()).isEqualTo(Bucket.REVIEW);
    assertThat(pinned.ruleSetVersion()).isEqualTo(1);
  }
}
