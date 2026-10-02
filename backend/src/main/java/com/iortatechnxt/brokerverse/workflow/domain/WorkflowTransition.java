package com.iortatechnxt.brokerverse.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/** An allowed move between two stages (seeded by migrations, read-only at run time). */
@Entity
@Table(name = "wf_transition")
public class WorkflowTransition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "workflow_code", nullable = false, length = 40)
  private String workflowCode;

  @Column(name = "from_stage", nullable = false, length = 40)
  private String fromStage;

  @Column(nullable = false, length = 40)
  private String action;

  @Column(name = "to_stage", nullable = false, length = 40)
  private String toStage;

  @Column(nullable = false, length = 80)
  private String label;

  @Column(nullable = false, length = 200)
  private String permission;

  @Column(nullable = false)
  private boolean generic;

  @Column(name = "reason_lov", length = 40)
  private String reasonLov;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected WorkflowTransition() {}

  /**
   * Permissions of which any one allows the action.
   *
   * @return permissions
   */
  public List<String> permissions() {
    return Arrays.stream(permission.split(",")).map(String::trim).toList();
  }

  /**
   * Whether a user with the given authority test may run the action.
   *
   * @param hasAuthority authority test
   * @return true when any permission is held
   */
  public boolean allowedFor(Predicate<String> hasAuthority) {
    return permissions().stream().anyMatch(hasAuthority);
  }

  public String getWorkflowCode() {
    return workflowCode;
  }

  public String getFromStage() {
    return fromStage;
  }

  public String getAction() {
    return action;
  }

  public String getToStage() {
    return toStage;
  }

  public String getLabel() {
    return label;
  }

  public String getPermission() {
    return permission;
  }

  public boolean isGeneric() {
    return generic;
  }

  public String getReasonLov() {
    return reasonLov;
  }

  public int getSortOrder() {
    return sortOrder;
  }
}
