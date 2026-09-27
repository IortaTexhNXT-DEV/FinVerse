package com.iortatechnxt.brokerverse.submitted.processing.service;

import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import java.time.LocalDate;
import java.util.Map;

/**
 * What every record of a run shares: the run, the active rules, the business date and the renewal
 * action of each bucket (parent code of LOV SBM_BUCKET: RENEW, MANUAL or EXCLUDE).
 *
 * @param run run
 * @param rules active rules
 * @param today business date
 * @param bucketActions renewal action by bucket
 */
public record RunContext(
    SbmRun run, ActiveRules rules, LocalDate today, Map<String, String> bucketActions) {

  /** Renewal action RENEW. */
  public static final String RENEW = "RENEW";

  /** Renewal action EXCLUDE. */
  public static final String EXCLUDE = "EXCLUDE";


  /** Defensive copy. */
  public RunContext {
    bucketActions = Map.copyOf(bucketActions);
  }

  /**
   * The renewal action of a bucket.
   *
   * @param bucket bucket
   * @return RENEW, MANUAL or EXCLUDE; MANUAL when unknown
   */
  public String actionOf(String bucket) {
    return bucketActions.getOrDefault(bucket, "MANUAL");
  }
}
