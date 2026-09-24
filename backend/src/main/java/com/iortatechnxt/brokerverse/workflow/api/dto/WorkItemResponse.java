package com.iortatechnxt.brokerverse.workflow.api.dto;

import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import java.time.Instant;

/**
 * A work item in a queue.
 *
 * @param id case id
 * @param workflowCode workflow
 * @param stageCode stage
 * @param stageName stage name
 * @param entityType record type
 * @param entityId record id
 * @param reference business reference
 * @param title description
 * @param link route of the record
 * @param originatingUnit originating unit
 * @param assignee assignee
 * @param stageEnteredAt in stage since
 * @param dueAt due time
 * @param overdue past due
 * @param createdBy originator
 * @param createdAt opened at
 */
public record WorkItemResponse(
    Long id,
    String workflowCode,
    String stageCode,
    String stageName,
    String entityType,
    String entityId,
    String reference,
    String title,
    String link,
    String originatingUnit,
    String assignee,
    Instant stageEnteredAt,
    Instant dueAt,
    boolean overdue,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a case.
   *
   * @param c case
   * @param stageName stage name
   * @param now current time
   * @return item
   */
  public static WorkItemResponse from(WorkCase c, String stageName, Instant now) {
    return new WorkItemResponse(
        c.getId(),
        c.getWorkflowCode(),
        c.getStageCode(),
        stageName,
        c.getEntityType(),
        c.getEntityId(),
        c.getReference(),
        c.getTitle(),
        c.getLink(),
        c.getOriginatingUnit(),
        c.getAssignee(),
        c.getStageEnteredAt(),
        c.getDueAt(),
        c.isOverdue(now),
        c.getCreatedBy(),
        c.getCreatedAt());
  }
}
