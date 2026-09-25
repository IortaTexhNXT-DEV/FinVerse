package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.StatusEditStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A request to correct the status tag of an instrument (DIS 2.8.5, AQ15), approved or rejected by
 * the team leader under workflow {@code DISB_STATUS_EDIT}.
 */
@Entity
@Table(name = "dsb_status_edit")
public class StatusEdit extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "instrument_id", nullable = false, updatable = false)
  private Long instrumentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", nullable = false, length = 20, updatable = false)
  private InstrumentStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, length = 20, updatable = false)
  private InstrumentStatus toStatus;

  @Column(nullable = false, length = 500, updatable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusEditStage stage = StatusEditStage.REQUESTED;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  protected StatusEdit() {}

  /**
   * A requested status change.
   *
   * @param companyId company
   * @param instrumentId instrument
   * @param fromStatus current status
   * @param toStatus requested status
   * @param reason reason
   */
  public StatusEdit(
      Long companyId,
      Long instrumentId,
      InstrumentStatus fromStatus,
      InstrumentStatus toStatus,
      String reason) {
    this.companyId = companyId;
    this.instrumentId = instrumentId;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.reason = reason;
  }

  /**
   * Mirrors the workflow stage with the deciding user.
   *
   * @param next stage
   * @param user user
   * @param at time
   */
  public void decided(StatusEditStage next, String user, Instant at) {
    stage = next;
    decidedBy = user;
    decidedAt = at;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getInstrumentId() {
    return instrumentId;
  }

  public InstrumentStatus getFromStatus() {
    return fromStatus;
  }

  public InstrumentStatus getToStatus() {
    return toStatus;
  }

  public String getReason() {
    return reason;
  }

  public StatusEditStage getStage() {
    return stage;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }
}
