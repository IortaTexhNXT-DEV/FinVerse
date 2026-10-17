package com.iortatechnxt.brokerverse.csf.cases.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * The five statuses of a case (CSF addendum BRCSFCM.003; FRS BRD-9 section 5.3) with their label,
 * their status group (the stages of the envisioned process) and the statuses an action may move a
 * case to. Closed statuses are final: a follow-up is a new linked case.
 */
public enum CaseStatus {

  /** Submitted or re-endorsed; in the queue of the fulfilment unit. */
  WITH_FULFILMENT("Open - With Fulfillment Unit", "Open / In Progress"),
  /** Returned by the fulfilment unit with a reason. */
  RETURNED_TO_AGENT("Open - Returned to Agent", "Pending"),
  /** The fulfilment unit needs the agent to act with the client. */
  CSR_ACTION_REQUIRED("Open - CSR Action Required", "Pending"),
  /** Resolved and closed by the fulfilment unit. */
  CLOSED_BY_FULFILMENT("Closed by Fulfillment Unit", "Closed"),
  /** Resolved on the call, or closed by the agent or the supervisor. */
  CLOSED_BY_CSR("Closed by CSR", "Closed");

  private final String label;
  private final String group;

  CaseStatus(String label, String group) {
    this.label = label;
    this.group = group;
  }

  /**
   * Label of the status.
   *
   * @return label
   */
  public String label() {
    return label;
  }

  /**
   * Status group of the envisioned process.
   *
   * @return group
   */
  public String group() {
    return group;
  }

  /**
   * Whether the case is closed (read only).
   *
   * @return true for a closed status
   */
  public boolean closed() {
    return this == CLOSED_BY_FULFILMENT || this == CLOSED_BY_CSR;
  }

  /**
   * The open statuses.
   *
   * @return statuses
   */
  public static Set<CaseStatus> open() {
    return EnumSet.of(WITH_FULFILMENT, RETURNED_TO_AGENT, CSR_ACTION_REQUIRED);
  }

  /**
   * Statuses waiting for the agent (returned or CSR action required).
   *
   * @return statuses
   */
  public static Set<CaseStatus> withAgent() {
    return EnumSet.of(RETURNED_TO_AGENT, CSR_ACTION_REQUIRED);
  }
}
