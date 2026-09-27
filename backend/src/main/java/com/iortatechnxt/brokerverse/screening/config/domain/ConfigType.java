package com.iortatechnxt.brokerverse.screening.config.domain;

/**
 * Type of a versioned screening configuration set (SNSRP-101-108). {@link #TEMPLATE} versions are
 * kept per template type (the version scope); every other type has one active set at a time.
 */
public enum ConfigType {
  MATCH_CRITERIA,
  RISK_RULES,
  APPROVAL_MATRIX,
  ASSIGNMENT_MATRIX,
  SLA_MATRIX,
  VALIDATION_RULES,
  TEMPLATE,
  STR_LAYOUT
}
