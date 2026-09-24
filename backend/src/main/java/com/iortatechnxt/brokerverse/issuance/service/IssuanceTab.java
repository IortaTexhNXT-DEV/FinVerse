package com.iortatechnxt.brokerverse.issuance.service;

/** Tabs of the Issuance Workbench. */
public enum IssuanceTab {
  /** Placed with the insurer, awaiting the e-policy (BRNB.073). */
  AWAITING_POLICY,
  /** E-policy received, extraction to review (BRNB.104). */
  REVIEW,
  /** Policy confirmed, e-policy not yet sent to the client (BRNB.077). */
  READY_TO_DISPATCH,
  /** Mortgaged accounts placed or issued without an Insurance Advice (BRNB.070). */
  IA_TO_GENERATE
}
