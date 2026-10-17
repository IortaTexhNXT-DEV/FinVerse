package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * What a matching rule decides (BRIDSP-08, 11-15).
 *
 * @param bucket bucket (LOV SBM_BUCKET), may be null
 * @param tag renewal tag, may be null
 * @param classification INFORCED or SUBMITTED, may be null
 * @param raTemplate GENERIC or FFY, may be null
 * @param flag FALLOUT or REVIEW, may be null
 */
@Embeddable
public record SbmRuleOutcome(
    @Column(name = "outcome_bucket", length = 40) String bucket,
    @Column(name = "outcome_tag", length = 20) String tag,
    @Column(name = "outcome_classification", length = 20) String classification,
    @Column(name = "outcome_ra_template", length = 20) String raTemplate,
    @Column(name = "outcome_flag", length = 20) String flag) {

  /**
   * Whether the outcome decides anything.
   *
   * @return false when every part is empty
   */
  public boolean isEmpty() {
    return bucket == null
        && tag == null
        && classification == null
        && raTemplate == null
        && flag == null;
  }
}
