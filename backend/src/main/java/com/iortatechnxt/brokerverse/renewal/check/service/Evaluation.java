package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import java.util.List;
import java.util.Optional;

/**
 * The result of a check run of a renewal: the bucket, the findings and the system action the checks
 * call for (a Not for Renewal tag by a non-renewable risk code or a LAMD report, or the transfer of
 * an RMU account).
 *
 * @param bucket bucket after the run
 * @param runId check run
 * @param findings findings
 * @param systemTag system Not for Renewal, empty when none
 */
public record Evaluation(
    Bucket bucket, Long runId, List<Finding> findings, Optional<SystemTag> systemTag) {

  /** Defensive copy. */
  public Evaluation {
    findings = List.copyOf(findings);
  }

  /**
   * Whether a check failed with a bucket severity.
   *
   * @return true when a FAIL counts for the bucket
   */
  public boolean anyFailure() {
    return findings.stream().anyMatch(f -> f.countsForBucket() && f.failed());
  }

  /**
   * The finding of a check.
   *
   * @param code check code
   * @return finding, empty when the check did not run
   */
  public Optional<Finding> finding(String code) {
    return findings.stream().filter(f -> f.code().equals(code)).findFirst();
  }

  /**
   * A system Not for Renewal or RMU routing (BRRN.009, 029).
   *
   * @param reasonCode reason (list RNW_NONRENEWAL_REASON)
   * @param source SYSTEM_CHECK or LAMD
   * @param remarks remarks of the disposition
   * @param transferUnit unit that receives an RMU account instead of the tag, null to tag
   */
  public record SystemTag(
      String reasonCode, DispositionSource source, String remarks, String transferUnit) {}
}
