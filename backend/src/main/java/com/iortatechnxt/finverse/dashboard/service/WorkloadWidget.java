package com.iortatechnxt.finverse.dashboard.service;

import java.util.Map;

/**
 * Work waiting for people: open alerts of the company and the viewer's approval inbox.
 *
 * @param openAlerts open or acknowledged alerts of the company
 * @param pendingApprovals items in the viewer's approval inbox for the company
 * @param approvalsByModule inbox items per module code
 */
public record WorkloadWidget(
    long openAlerts, long pendingApprovals, Map<String, Long> approvalsByModule) {

  /** Canonical constructor copying the map. */
  public WorkloadWidget {
    approvalsByModule = Map.copyOf(approvalsByModule);
  }
}
