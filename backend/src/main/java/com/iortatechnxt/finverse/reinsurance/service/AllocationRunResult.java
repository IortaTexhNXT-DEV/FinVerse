package com.iortatechnxt.finverse.reinsurance.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of an RI allocation run.
 *
 * @param jobRunId job run recorded in the job monitor
 * @param status job run status
 * @param ceded transactions ceded
 * @param failed transactions that could not be ceded
 * @param premiumCeded base currency premium ceded to treaties and facultative
 * @param messages first failure messages
 */
public record AllocationRunResult(
    Long jobRunId,
    String status,
    int ceded,
    int failed,
    BigDecimal premiumCeded,
    List<String> messages) {

  /** Canonical constructor copying the messages. */
  public AllocationRunResult {
    messages = List.copyOf(messages);
  }
}
