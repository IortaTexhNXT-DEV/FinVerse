package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.service.AccessBatchService.BatchDecision;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessBatchService.LineOutcome;
import java.util.List;

/**
 * Result of a batch approval: every line with its status, the temporary password of a created user
 * (shown once) or the reason it failed (BRD 1.009; FR-UA-019).
 *
 * @param batch the batch
 * @param lines line outcomes
 */
public record AccessBatchDecisionResponse(AccessBatchResponse batch, List<LineOutcome> lines) {

  /**
   * Maps a decision.
   *
   * @param d decision
   * @return response
   */
  public static AccessBatchDecisionResponse from(BatchDecision d) {
    return new AccessBatchDecisionResponse(AccessBatchResponse.from(d.batch()), d.lines());
  }

  @Override
  public String toString() {
    return "AccessBatchDecisionResponse[" + batch.batchNo() + ", ***]";
  }
}
