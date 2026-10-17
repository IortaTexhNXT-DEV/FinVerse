package com.iortatechnxt.brokerverse.screening.config.domain;

/** Check a validation rule runs before a case is routed (SNSRP-701, 802). */
public enum ValidationKind {
  TEMPLATE_COMPLETE,
  DOCUMENT_TYPES_PRESENT,
  DISPOSITION_ALLOWED,
  RECOMMENDATION_PRESENT,
  STR_FLAG_CONSISTENT
}
