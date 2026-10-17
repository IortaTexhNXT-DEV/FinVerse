package com.iortatechnxt.brokerverse.migration.cutover.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A go / no-go criterion of a plan (DATA_MIGRATION_DESIGN section 17.5; FR-DM-121) with its
 * threshold and its measured value: measured by BIBS for the reconciliation, sign-off and queue
 * criteria, entered with a note for the manual ones (smoke test, snapshot, roster, signatures).
 */
@Entity
@Table(name = "mig_gonogo_criterion")
public class GonogoCriterion extends BaseEntity {

  @Column(name = "plan_id", nullable = false, updatable = false)
  private Long planId;

  @Column(name = "criterion_no", nullable = false, updatable = false)
  private int criterionNo;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 200)
  private String threshold;

  @Column(nullable = false, length = 30)
  private String measure;

  @Column(name = "measured_value", length = 200)
  private String measuredValue;

  @Column private Boolean met;

  @Column(name = "manual_note", length = 1000)
  private String manualNote;

  @Column(name = "measured_by", length = 50)
  private String measuredBy;

  @Column(name = "measured_at")
  private Instant measuredAt;

  protected GonogoCriterion() {}

  /**
   * A criterion.
   *
   * @param planId plan
   * @param criterionNo number
   * @param name name
   * @param threshold threshold
   * @param measure measure code (MANUAL for the manual ones)
   */
  public GonogoCriterion(
      Long planId, int criterionNo, String name, String threshold, String measure) {
    this.planId = planId;
    this.criterionNo = criterionNo;
    this.name = name;
    this.threshold = threshold;
    this.measure = measure;
  }

  /**
   * Records a measurement.
   *
   * @param value measured value
   * @param isMet threshold met
   * @param note note (manual criteria)
   * @param user user
   * @param when time
   */
  public void measured(String value, boolean isMet, String note, String user, Instant when) {
    this.measuredValue = value;
    this.met = isMet;
    this.manualNote = note == null ? manualNote : note;
    this.measuredBy = user;
    this.measuredAt = when;
  }

  public boolean manual() {
    return "MANUAL".equals(measure);
  }

  public boolean isMetTrue() {
    return Boolean.TRUE.equals(met);
  }

  public Long getPlanId() {
    return planId;
  }

  public int getCriterionNo() {
    return criterionNo;
  }

  public String getName() {
    return name;
  }

  public String getThreshold() {
    return threshold;
  }

  public String getMeasure() {
    return measure;
  }

  public String getMeasuredValue() {
    return measuredValue;
  }

  public Boolean getMet() {
    return met;
  }

  public String getManualNote() {
    return manualNote;
  }

  public String getMeasuredBy() {
    return measuredBy;
  }

  public Instant getMeasuredAt() {
    return measuredAt;
  }
}
