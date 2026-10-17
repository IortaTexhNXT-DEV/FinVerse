package com.iortatechnxt.brokerverse.screening.cases.domain;

/** Whether a screening case is open or closed (retention rule SCREENING_CASE selects CLOSED). */
public enum CaseStatus {
  /** In one of the working stages. */
  OPEN,
  /** In stage CLOSED. */
  CLOSED
}
