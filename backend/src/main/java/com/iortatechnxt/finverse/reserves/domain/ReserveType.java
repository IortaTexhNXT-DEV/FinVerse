package com.iortatechnxt.finverse.reserves.domain;

/**
 * Technical reserve held on a valuation run line. Every line carries a gross amount and the
 * reinsurers' share; net = gross − reinsurers' share.
 */
public enum ReserveType {
  /** Unearned premium reserve; reinsurers' share = treaty + FAC UPR. */
  UPR("Unearned premium reserve"),
  /** Deferred acquisition cost (unearned commission); RI column = unearned RI commission (UCR). */
  DAC("Deferred acquisition cost / UCR"),
  /** Outstanding loss reserve of reported claims. */
  OSLR("Outstanding loss reserve"),
  /** Incurred but not reported claims. */
  IBNR("IBNR"),
  /** Unallocated loss adjustment expenses (gross only). */
  ULAE("ULAE provision"),
  /** Margin for adverse deviation on OSLR + IBNR. */
  MFAD("Margin for adverse deviation"),
  /** Premium deficiency reserve from the liability adequacy test (net only). */
  PDR("Premium deficiency reserve");

  private final String label;

  ReserveType(String label) {
    this.label = label;
  }

  /**
   * Display label.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
