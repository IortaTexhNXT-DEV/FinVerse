package com.iortatechnxt.brokerverse.screening.risk.domain;

/** Who decided a risk-profile change (SNSRP-302, 304). */
public enum RiskSource {
  /** The risk rules, after a screening run or a match decision. */
  RULE,
  /** An investigator, with justification and evidence. */
  MANUAL
}
