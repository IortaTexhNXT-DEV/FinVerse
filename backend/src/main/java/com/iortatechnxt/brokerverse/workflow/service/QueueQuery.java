package com.iortatechnxt.brokerverse.workflow.service;

/**
 * Work queue filters.
 *
 * @param companyId company
 * @param workflowCode workflow, null for all
 * @param stageCode stage, null for all
 * @param scope whose items
 * @param overdueOnly only items past their due time
 * @param text reference or title contains
 */
public record QueueQuery(
    Long companyId,
    String workflowCode,
    String stageCode,
    Scope scope,
    boolean overdueOnly,
    String text) {

  /** Whose items to show. */
  public enum Scope {
    /** Assigned to me. */
    MINE,
    /** Not assigned to anyone. */
    UNASSIGNED,
    /** Everything in the queues I work. */
    ALL
  }
}
