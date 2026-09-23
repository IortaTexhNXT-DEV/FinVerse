package com.iortatechnxt.finverse.consolidation.domain;

/**
 * Consolidation run status: DRAFT (calculated, translated and eliminated) → FINAL (locked). A newer
 * run for the same group and date cancels an earlier DRAFT; a FINAL run blocks re-runs.
 */
public enum ConsolidationRunStatus {
  DRAFT,
  FINAL,
  CANCELLED
}
