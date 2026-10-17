package com.iortatechnxt.brokerverse.submitted.domain;

/** Classification of a submitted policy (BRIDSP-11; definition SP SQ04). */
public enum SbmClassification {
  /** PN not matched to an active LAMD loan. */
  INFORCED,
  /** PN matched to an active LAMD loan and not placed by BDOI. */
  SUBMITTED
}
