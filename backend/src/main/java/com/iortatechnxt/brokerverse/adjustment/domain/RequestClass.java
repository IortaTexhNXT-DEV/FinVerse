package com.iortatechnxt.brokerverse.adjustment.domain;

/**
 * Class of an endorsement request, the parent of its {@code ENDORSEMENT_TYPE} value
 * (ADJID.001-004): financial ({@code FIN_}), non-financial ({@code NF_}) or internal ({@code
 * INT_}).
 */
public enum RequestClass {
  /** Financial endorsement: premium, charges or commission change, cancellation, write-off. */
  FINANCIAL,
  /** Non-financial endorsement: descriptive or period change without financial effect. */
  NON_FINANCIAL,
  /** Internal adjustment: no insurer endorsement and no endorsement slip. */
  INTERNAL
}
