package com.iortatechnxt.brokerverse.migration.mapping.domain;

/**
 * What the target code of a code map entry refers to; the approval of a map version checks that
 * each MAP target exists and is active in that domain (FR-DM-011 R4).
 */
public enum TargetKind {
  /** Not checked (free codes, for example legacy statuses mapped to BIBS status names). */
  FREE,
  /** A value of the list of values named by the set's target reference. */
  LOV,
  /** An insurer (party code). */
  INSURER,
  /** A product (risk code). */
  PRODUCT,
  /** A product line. */
  LINE,
  /** A cover type. */
  COVER_TYPE,
  /** A branch code. */
  BRANCH,
  /** A sales unit. */
  SALES_UNIT,
  /** A BIBS user name. */
  USER,
  /** A chart of accounts code, or CLEARING for the legacy control accounts. */
  GL_ACCOUNT,
  /** An ISO currency. */
  CURRENCY,
  /** A package version (product code and version). */
  PACKAGE,
  /** A cost centre. */
  COST_CENTER,
  /** A BIBS status of the target record. */
  STATUS
}
