package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * The disposition the decision matrix proposes after initiation (BRRN.034): the default shown to
 * the AO, applied by the system only for a Clean candidate and an AUTO rule.
 *
 * @param disposition proposed disposition
 * @param automatic whether the rule is AUTO
 * @param matrixVersion version of the matrix
 * @param ruleId rule that matched
 */
@Embeddable
public record DispositionProposal(
    @Enumerated(EnumType.STRING) @Column(name = "proposed_disposition", length = 20)
        RenewalDisposition disposition,
    @Column(name = "proposed_automation", length = 10) String automatic,
    @Column(name = "proposed_matrix_version") Integer matrixVersion,
    @Column(name = "proposed_rule_id") Long ruleId) {

  /** Automation code of an automatic rule. */
  public static final String AUTO = "AUTO";

  /** Automation code of a manual rule. */
  public static final String MANUAL = "MANUAL";

  /** No proposal (no active matrix or no rule matched). */
  public static final DispositionProposal NONE = new DispositionProposal(null, null, null, null);

  /**
   * Whether the proposal is applied without user action.
   *
   * @return true for an AUTO rule
   */
  public boolean isAuto() {
    return AUTO.equals(automatic);
  }
}
