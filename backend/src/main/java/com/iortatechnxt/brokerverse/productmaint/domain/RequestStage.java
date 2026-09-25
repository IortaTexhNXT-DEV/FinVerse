package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stage of a package request: the mirror of its PM_PACKAGE_REQUEST work case (V755; design section
 * 7), kept on the request by {@code PackageStatusListener}.
 */
public enum RequestStage {
  /** Being prepared by Marketing or TSU. */
  DRAFT,
  /** Waiting for the Marketing TL / TH / UH approval (BRPM.008). */
  FOR_MKT_APPROVAL,
  /** With the TSU Team Lead for review and recommendation (BRPM.009). */
  FOR_TSU_REVIEW,
  /** With the TSU Head for approval (BRPM.009). */
  FOR_TSU_APPROVAL,
  /** Insurer negotiation in rounds (BRPM.010/012/013, PMADD04). */
  NEGOTIATION,
  /** TSU Head reviews the negotiated terms (BRPM.013). */
  TERMS_REVIEW,
  /** Marketing reviews the terms of a client-specific package (BRPM.013). */
  FOR_MKT_REVIEW,
  /** TSU compiles the requirements pack (BRPM.015). */
  REQUIREMENTS_PREP,
  /** Waiting for the ManCom sign-off (BRPM.015). */
  FOR_MANCOM,
  /** With MBS for the package set-up (BRPM.015). */
  WITH_MBS,
  /** The catalog version waits for the validation checkpoint (PMADD06). */
  FOR_VALIDATION,
  /** The version was released (terminal). */
  RELEASED,
  /** The package was retired (terminal). */
  RETIRED,
  /** Closed without a package (terminal). */
  NOT_PROCEEDED,
  /** Voided (terminal). */
  VOIDED;

  /** Terminal stages. */
  public static final Set<RequestStage> CLOSED =
      EnumSet.of(RELEASED, RETIRED, NOT_PROCEEDED, VOIDED);

  /** Stages in which negotiation rounds and responses may change (until ManCom sign-off). */
  public static final Set<RequestStage> NEGOTIATION_OPEN =
      EnumSet.of(NEGOTIATION, TERMS_REVIEW, FOR_MKT_REVIEW, REQUIREMENTS_PREP, FOR_MANCOM);

  /**
   * Whether the stage is terminal.
   *
   * @return true when closed
   */
  public boolean isClosed() {
    return CLOSED.contains(this);
  }
}
