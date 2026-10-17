package com.iortatechnxt.brokerverse.migration.cutover.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A cutover plan (DATA_MIGRATION_DESIGN section 17.1; FR-DM-120): a mock, the dress rehearsal or
 * the production cut-over, with its environment, go-live date, freeze window and status; its tasks
 * and go / no-go criteria are separate records.
 */
@Entity
@Table(name = "mig_cutover_plan")
public class CutoverPlan extends BaseEntity {

  /** Kind of plan. */
  public enum Kind {
    MOCK,
    DRESS_REHEARSAL,
    PRODUCTION
  }

  /** Status of a plan. */
  public enum Status {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    GO,
    NO_GO
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "plan_no", nullable = false, length = 20, updatable = false)
  private String planNo;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Kind kind;

  @Column(name = "mock_no")
  private Integer mockNo;

  @Column(nullable = false, length = 20)
  private String environment;

  @Column(name = "go_live_date", nullable = false)
  private LocalDate goLiveDate;

  @Column(name = "freeze_start")
  private LocalDateTime freezeStart;

  @Column(name = "freeze_end")
  private LocalDateTime freezeEnd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private Status status = Status.PLANNED;

  protected CutoverPlan() {}

  /**
   * A new plan.
   *
   * @param companyId company
   * @param planNo number
   * @param data plan data
   */
  public CutoverPlan(Long companyId, String planNo, Data data) {
    this.companyId = companyId;
    this.planNo = planNo;
    assign(data);
  }

  /**
   * Replaces the plan data.
   *
   * @param data plan data
   */
  public void apply(Data data) {
    assign(data);
  }

  private void assign(Data data) {
    this.name = data.name();
    this.kind = data.kind();
    this.mockNo = data.mockNo();
    this.environment = data.environment();
    this.goLiveDate = data.goLiveDate();
    this.freezeStart = data.freezeStart();
    this.freezeEnd = data.freezeEnd();
  }

  /**
   * Moves the plan.
   *
   * @param newStatus status
   */
  public void mark(Status newStatus) {
    this.status = newStatus;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getPlanNo() {
    return planNo;
  }

  public String getName() {
    return name;
  }

  public Kind getKind() {
    return kind;
  }

  public Integer getMockNo() {
    return mockNo;
  }

  public String getEnvironment() {
    return environment;
  }

  public LocalDate getGoLiveDate() {
    return goLiveDate;
  }

  public LocalDateTime getFreezeStart() {
    return freezeStart;
  }

  public LocalDateTime getFreezeEnd() {
    return freezeEnd;
  }

  public Status getStatus() {
    return status;
  }

  /**
   * Data of a plan.
   *
   * @param name name
   * @param kind kind
   * @param mockNo mock number
   * @param environment SIT, UAT, PERF or PROD
   * @param goLiveDate go-live date
   * @param freezeStart start of the business freeze
   * @param freezeEnd end of the freeze
   */
  public record Data(
      String name,
      Kind kind,
      Integer mockNo,
      String environment,
      LocalDate goLiveDate,
      LocalDateTime freezeStart,
      LocalDateTime freezeEnd) {}
}
