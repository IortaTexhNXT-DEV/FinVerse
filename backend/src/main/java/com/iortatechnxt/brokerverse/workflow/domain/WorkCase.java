package com.iortatechnxt.brokerverse.workflow.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The workflow position of one business record (quotation, proposal request, account...). The
 * business record owns its data; the case owns where it stands, who works it and since when.
 */
@Entity
@Table(name = "wf_case")
public class WorkCase extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "workflow_code", nullable = false, length = 40, updatable = false)
  private String workflowCode;

  @Column(name = "entity_type", nullable = false, length = 60, updatable = false)
  private String entityType;

  @Column(name = "entity_id", nullable = false, length = 60, updatable = false)
  private String entityId;

  @Column(nullable = false, length = 60)
  private String reference;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 300)
  private String link;

  @Column(name = "originating_unit", length = 60)
  private String originatingUnit;

  @Column(name = "stage_code", nullable = false, length = 40)
  private String stageCode;

  @Column(name = "stage_entered_at", nullable = false)
  private Instant stageEnteredAt;

  @Column(name = "due_at")
  private Instant dueAt;

  @Column(length = 50)
  private String assignee;

  @Column(nullable = false)
  private boolean closed;

  protected WorkCase() {}

  /**
   * Opens a case.
   *
   * @param companyId company
   * @param workflowCode workflow
   * @param record business record facts
   * @param stage first stage
   * @param now time
   */
  public WorkCase(
      Long companyId, String workflowCode, CaseRecord record, WorkflowStage stage, Instant now) {
    this.companyId = companyId;
    this.workflowCode = workflowCode;
    this.entityType = record.entityType();
    this.entityId = record.entityId();
    this.reference = record.reference();
    this.title = record.title();
    this.link = record.link();
    this.originatingUnit = record.originatingUnit();
    enter(stage, now);
  }

  /**
   * Moves the case to a stage.
   *
   * @param stage new stage
   * @param now time
   */
  public final void enter(WorkflowStage stage, Instant now) {
    this.stageCode = stage.getStageCode();
    this.stageEnteredAt = now;
    this.dueAt = stage.dueAt(now);
    this.closed = stage.isTerminal();
  }

  /**
   * Replaces the due time of the current stage (a dated SLA matrix of the business module, e.g. the
   * screening SLA matrix, SNSRP-108).
   *
   * @param newDueAt new due time; null removes the SLA of the stage
   */
  public void overrideDue(Instant newDueAt) {
    this.dueAt = newDueAt;
  }

  /**
   * Assigns the case (null releases it to the team queue).
   *
   * @param username assignee
   */
  public void assignTo(String username) {
    this.assignee = username;
  }

  /**
   * Updates the display facts when the business record changes (e.g. insured name).
   *
   * @param newReference reference
   * @param newTitle title
   */
  public void describe(String newReference, String newTitle) {
    this.reference = newReference;
    this.title = newTitle;
  }

  /**
   * Whether the case is past its due time.
   *
   * @param now time
   * @return true when overdue
   */
  public boolean isOverdue(Instant now) {
    return !closed && dueAt != null && dueAt.isBefore(now);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getWorkflowCode() {
    return workflowCode;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getReference() {
    return reference;
  }

  public String getTitle() {
    return title;
  }

  public String getLink() {
    return link;
  }

  public String getOriginatingUnit() {
    return originatingUnit;
  }

  public String getStageCode() {
    return stageCode;
  }

  public Instant getStageEnteredAt() {
    return stageEnteredAt;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public String getAssignee() {
    return assignee;
  }

  public boolean isClosed() {
    return closed;
  }
}
