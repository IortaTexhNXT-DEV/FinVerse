package com.iortatechnxt.brokerverse.screening.cases.domain;

import java.util.Set;

/**
 * The case types of LOV {@code SCR_CASE_TYPE} (design 4.4; FRS section 2.3) and the review template
 * each one uses (FR-SS-050 R1).
 */
public final class CaseTypes {

  /** A potential match reached the case threshold. */
  public static final String NAME_MATCH = "NAME_MATCH";

  /** The client was tagged PEP by a risk rule. */
  public static final String PEP = "PEP";

  /** The client was tagged high risk by a risk rule. */
  public static final String HIGH_RISK = "HIGH_RISK";

  /** Enhanced due diligence. */
  public static final String EDD = "EDD";

  /** High-risk or PEP client without an active policy (SNSRP-303 AC2). */
  public static final String MONITOR = "MONITOR";

  /** The client applied for an account (SNSRP-303 AC3). */
  public static final String ACCOUNT_APPLICATION = "ACCOUNT_APPLICATION";

  /** Every case type. */
  public static final Set<String> ALL =
      Set.of(NAME_MATCH, PEP, HIGH_RISK, EDD, MONITOR, ACCOUNT_APPLICATION);

  /** The types that need a KYC review or EDD only for a client with an active policy. */
  public static final Set<String> NEED_ACTIVE_POLICY = Set.of(PEP, HIGH_RISK, EDD);

  private CaseTypes() {}
}
