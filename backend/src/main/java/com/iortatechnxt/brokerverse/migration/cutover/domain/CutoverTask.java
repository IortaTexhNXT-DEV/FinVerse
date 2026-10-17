package com.iortatechnxt.brokerverse.migration.cutover.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * A task of a cutover plan: sequence, phase, task, owner role, the object it loads, dependencies,
 * planned and actual times, status and evidence. The runbook is the export of the tasks.
 */
@Entity
@Table(name = "mig_cutover_task")
public class CutoverTask {

  /** Status of a task. */
  public enum Status {
    NOT_STARTED,
    IN_PROGRESS,
    DONE,
    BLOCKED,
    SKIPPED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(name = "plan_id", nullable = false, updatable = false)
  private Long planId;

  @Column(nullable = false)
  private int seq;

  @Column(nullable = false, length = 40)
  private String phase;

  @Column(nullable = false, length = 300)
  private String task;

  @Column(name = "owner_role", nullable = false, length = 60)
  private String ownerRole;

  @Column(name = "object_code", length = 10)
  private String objectCode;

  @Column(name = "depends_on", length = 100)
  private String dependsOn;

  @Column(name = "planned_start")
  private LocalDateTime plannedStart;

  @Column(name = "planned_end")
  private LocalDateTime plannedEnd;

  @Column(name = "actual_start")
  private LocalDateTime actualStart;

  @Column(name = "actual_end")
  private LocalDateTime actualEnd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.NOT_STARTED;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "evidence_file_id")
  private Long evidenceFileId;

  @Column(name = "evidence_name", length = 255)
  private String evidenceName;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, length = 50, updatable = false)
  private String createdBy;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "updated_by", length = 50)
  private String updatedBy;

  protected CutoverTask() {}

  /**
   * A new task.
   *
   * @param planId plan
   * @param data task data
   * @param user creator
   * @param when time
   */
  public CutoverTask(Long planId, Data data, String user, Instant when) {
    this.planId = planId;
    this.createdBy = user;
    this.createdAt = when;
    assign(data);
  }

  /**
   * Replaces the task data.
   *
   * @param data task data
   */
  public void apply(Data data) {
    assign(data);
  }

  private void assign(Data data) {
    this.seq = data.seq();
    this.phase = data.phase();
    this.task = data.task();
    this.ownerRole = data.ownerRole();
    this.objectCode = data.objectCode();
    this.dependsOn = data.dependsOn();
    this.plannedStart = data.plannedStart();
    this.plannedEnd = data.plannedEnd();
  }

  /**
   * Records progress.
   *
   * @param newStatus status
   * @param at time of the change (business time)
   * @param note remarks
   * @param user user
   * @param when audit time
   */
  public void progress(Status newStatus, LocalDateTime at, String note, String user, Instant when) {
    if (newStatus == Status.IN_PROGRESS && actualStart == null) {
      this.actualStart = at;
    }
    if (newStatus == Status.DONE || newStatus == Status.SKIPPED) {
      this.actualStart = actualStart == null ? at : actualStart;
      this.actualEnd = at;
    }
    this.status = newStatus;
    this.remarks = note == null ? remarks : note;
    this.updatedBy = user;
    this.updatedAt = when;
  }

  /**
   * Keeps the evidence file.
   *
   * @param fileId stored file
   * @param name file name
   */
  public void evidence(Long fileId, String name) {
    this.evidenceFileId = fileId;
    this.evidenceName = name;
  }

  /**
   * Sequences this task depends on.
   *
   * @return sequences
   */
  public List<Integer> dependencies() {
    if (dependsOn == null || dependsOn.isBlank()) {
      return List.of();
    }
    return Arrays.stream(dependsOn.split(",")).map(String::trim).map(Integer::valueOf).toList();
  }

  public boolean finished() {
    return status == Status.DONE || status == Status.SKIPPED;
  }

  public Long getId() {
    return id;
  }

  public Long getPlanId() {
    return planId;
  }

  public int getSeq() {
    return seq;
  }

  public String getPhase() {
    return phase;
  }

  public String getTask() {
    return task;
  }

  public String getOwnerRole() {
    return ownerRole;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public String getDependsOn() {
    return dependsOn;
  }

  public LocalDateTime getPlannedStart() {
    return plannedStart;
  }

  public LocalDateTime getPlannedEnd() {
    return plannedEnd;
  }

  public LocalDateTime getActualStart() {
    return actualStart;
  }

  public LocalDateTime getActualEnd() {
    return actualEnd;
  }

  public Status getStatus() {
    return status;
  }

  public String getRemarks() {
    return remarks;
  }

  public Long getEvidenceFileId() {
    return evidenceFileId;
  }

  public String getEvidenceName() {
    return evidenceName;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  /**
   * Data of a task.
   *
   * @param seq sequence
   * @param phase phase
   * @param task task
   * @param ownerRole owner role
   * @param objectCode object it loads, if any
   * @param dependsOn sequences it depends on, comma separated
   * @param plannedStart planned start
   * @param plannedEnd planned end
   */
  public record Data(
      int seq,
      String phase,
      String task,
      String ownerRole,
      String objectCode,
      String dependsOn,
      LocalDateTime plannedStart,
      LocalDateTime plannedEnd) {}
}
