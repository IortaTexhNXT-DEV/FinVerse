package com.iortatechnxt.brokerverse.workflow.api.dto;

import com.iortatechnxt.brokerverse.workflow.service.QueueCount;

/**
 * My Work tile.
 *
 * @param workflowCode workflow
 * @param stageCode stage
 * @param stageName stage name
 * @param open open items
 * @param overdue overdue items
 * @param mine assigned to me
 */
public record QueueCountResponse(
    String workflowCode, String stageCode, String stageName, long open, long overdue, long mine) {

  /**
   * Maps a count.
   *
   * @param c count
   * @return response
   */
  public static QueueCountResponse from(QueueCount c) {
    return new QueueCountResponse(
        c.workflowCode(), c.stageCode(), c.stageName(), c.open(), c.overdue(), c.mine());
  }
}
