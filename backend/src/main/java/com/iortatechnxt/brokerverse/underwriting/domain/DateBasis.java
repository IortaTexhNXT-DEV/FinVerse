package com.iortatechnxt.brokerverse.underwriting.domain;

/** Date that selects documents into a register ("Based on" report option). */
public enum DateBasis {
  /** Document issue date. */
  ISSUE,
  /** Approval (accounting) date. */
  APPROVAL,
  /** Cover start (policy period from; endorsement effective date). */
  PERIOD_FROM
}
