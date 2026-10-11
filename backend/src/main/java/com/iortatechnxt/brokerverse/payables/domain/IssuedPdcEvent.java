package com.iortatechnxt.brokerverse.payables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Status history (confirmation audit trail) of a post-dated cheque issued. Insert only. */
@Entity
@Table(name = "pay_pdc_event")
public class IssuedPdcEvent extends BaseEntity {

  @Column(name = "pdc_id", nullable = false)
  private Long pdcId;

  @Column(name = "event_date", nullable = false)
  private LocalDate eventDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", length = 20)
  private IssuedPdcStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, length = 20)
  private IssuedPdcStatus toStatus;

  @Column(name = "batch_no", length = 40)
  private String batchNo;

  @Column(length = 200)
  private String remarks;

  protected IssuedPdcEvent() {}

  /**
   * Records a transition.
   *
   * @param pdcId cheque
   * @param eventDate business date of the transition
   * @param fromStatus previous status (null on issue)
   * @param toStatus new status
   * @param batchNo journal posted by the transition, if any
   * @param remarks remarks
   */
  public IssuedPdcEvent(
      Long pdcId,
      LocalDate eventDate,
      IssuedPdcStatus fromStatus,
      IssuedPdcStatus toStatus,
      String batchNo,
      String remarks) {
    this.pdcId = pdcId;
    this.eventDate = eventDate;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.batchNo = batchNo;
    this.remarks = remarks;
  }

  public Long getPdcId() {
    return pdcId;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public IssuedPdcStatus getFromStatus() {
    return fromStatus;
  }

  public IssuedPdcStatus getToStatus() {
    return toStatus;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getRemarks() {
    return remarks;
  }
}
