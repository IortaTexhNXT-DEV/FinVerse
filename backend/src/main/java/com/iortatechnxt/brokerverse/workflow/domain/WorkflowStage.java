package com.iortatechnxt.brokerverse.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/** A stage of a workflow (seeded by migrations, read-only at run time). */
@Entity
@Table(name = "wf_stage")
public class WorkflowStage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "workflow_code", nullable = false, length = 40)
  private String workflowCode;

  @Column(name = "stage_code", nullable = false, length = 40)
  private String stageCode;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "owner_permission", length = 50)
  private String ownerPermission;

  @Column(name = "sla_hours")
  private Integer slaHours;

  @Column(nullable = false)
  private boolean initial;

  @Column(nullable = false)
  private boolean terminal;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected WorkflowStage() {}

  /**
   * When a case entering the stage at a time is due (null without SLA).
   *
   * @param enteredAt stage entry time
   * @return due time
   */
  public Instant dueAt(Instant enteredAt) {
    return slaHours == null ? null : enteredAt.plus(Duration.ofHours(slaHours));
  }

  /**
   * Whether the stage is worked from a team queue.
   *
   * @return true when an owner permission is set and the stage is not terminal
   */
  public boolean isQueueStage() {
    return ownerPermission != null && !terminal;
  }

  public Long getId() {
    return id;
  }

  public String getWorkflowCode() {
    return workflowCode;
  }

  public String getStageCode() {
    return stageCode;
  }

  public String getName() {
    return name;
  }

  public String getOwnerPermission() {
    return ownerPermission;
  }

  public Integer getSlaHours() {
    return slaHours;
  }

  public boolean isInitial() {
    return initial;
  }

  public boolean isTerminal() {
    return terminal;
  }

  public int getSortOrder() {
    return sortOrder;
  }
}
