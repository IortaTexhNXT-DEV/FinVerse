package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.List;

/**
 * Result of a dry run: the difference per dataset in load order, the blockers and the warnings.
 *
 * @param diffs differences in load order
 * @param blockers findings that prevent the approval
 * @param warnings other findings
 */
public record Analysis(List<DatasetDiff> diffs, List<Issue> blockers, List<Issue> warnings) {

  /** Defensive copies. */
  public Analysis {
    diffs = List.copyOf(diffs);
    blockers = List.copyOf(blockers);
    warnings = List.copyOf(warnings);
  }
}
