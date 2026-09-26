package com.iortatechnxt.brokerverse.workflow.domain;

/**
 * Open cases of one stage (query projection).
 *
 * @param workflowCode workflow
 * @param stageCode stage
 * @param open open cases
 * @param overdue cases past due
 * @param mine cases assigned to the current user
 */
public record StageCountRow(
    String workflowCode, String stageCode, long open, Long overdue, Long mine) {

  /**
   * Map key of the row.
   *
   * @return workflow:stage
   */
  public String key() {
    return workflowCode + ":" + stageCode;
  }
}
