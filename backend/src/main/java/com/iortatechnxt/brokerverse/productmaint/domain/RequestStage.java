package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stage of a package request: the mirror of its PM_PACKAGE_REQUEST work case (V755; design section
 * 7), kept on the request by {@code PackageStatusListener}.
 */
public enum RequestStage {
  /** Being prepared by Marketing or TSU. */
  DRAFT("Draft request"),
  /** Waiting for the Marketing TL / TH / UH approval (BRPM.008). */
  FOR_MKT_APPROVAL("For Marketing approval"),
  /** With the TSU Team Lead for review and recommendation (BRPM.009). */
  FOR_TSU_REVIEW("For TSU review"),
  /** With the TSU Head for approval (BRPM.009). */
  FOR_TSU_APPROVAL("For TSU Head approval"),
  /** Insurer negotiation in rounds (BRPM.010/012/013, PMADD04). */
  NEGOTIATION("Insurer negotiation"),
  /** TSU Head reviews the negotiated terms (BRPM.013). */
  TERMS_REVIEW("Negotiated terms review"),
  /** Marketing reviews the terms of a client-specific package (BRPM.013). */
  FOR_MKT_REVIEW("Marketing review of terms"),
  /** TSU compiles the requirements pack (BRPM.015). */
  REQUIREMENTS_PREP("Requirements preparation"),
  /** Waiting for the ManCom sign-off (BRPM.015). */
  FOR_MANCOM("For ManCom sign-off"),
  /** With MBS for the package set-up (BRPM.015). */
  WITH_MBS("With MBS for set-up"),
  /** The catalog version waits for the validation checkpoint (PMADD06). */
  FOR_VALIDATION("Package version for validation"),
  /** The version was released (terminal). */
  RELEASED("Released"),
  /** The package was retired (terminal). */
  RETIRED("Package retired"),
  /** Closed without a package (terminal). */
  NOT_PROCEEDED("Not proceeded"),
  /** Voided (terminal). */
  VOIDED("Voided");

  private final String label;

  RequestStage(String label) {
    this.label = label;
  }

  /**
   * The stage as users read it (the stage name of the PM_PACKAGE_REQUEST workflow, V755).
   *
   * @return stage name
   */
  public String label() {
    return label;
  }

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
