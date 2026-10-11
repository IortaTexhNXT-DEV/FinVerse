package com.iortatechnxt.brokerverse.report.core;

/** Input control types for report parameters (drives the generic parameter form in the UI). */
public enum ParameterType {
  DATE,
  TEXT,
  NUMBER,
  BOOLEAN,
  SELECT,
  COMPANY,
  BRANCH,
  ACCOUNT,
  CURRENCY,
  BUSINESS_LINE,
  /** An insurer, chosen by name; the value is the insurer's party code. */
  INSURER,
  /**
   * An include or exclude list of codes ("only" or "all except", BRD x.009.2): the value is a comma
   * separated list, prefixed with {@code !} for "all except"; the options come from the {@link
   * CodeSetSource} named in {@link ParameterSpec#options()}.
   */
  CODE_SET,
  /**
   * One value chosen from a list by name (a user, a group profile, a sales unit...): the value is
   * the code; the options come from the {@link CodeSetSource} named in {@link
   * ParameterSpec#options()}.
   */
  LOOKUP
}
