package com.iortatechnxt.finverse.receivables.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Status change of a post-dated cheque (confirmation audit trail). */
@Entity
@Table(name = "rcv_pdc_event")
public class PdcEvent extends BaseEntity {

  @Column(name = "pdc_id", nullable = false)
  private Long pdcId;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", length = 20)
  private PdcStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, length = 20)
  private PdcStatus toStatus;

  @Column(name = "event_date", nullable = false)
  private LocalDate eventDate;

  @Column(length = 250)
  private String remarks;

  @Column(name = "receipt_no", length = 40)
  private String receiptNo;

  protected PdcEvent() {}

  /**
   * Creates an event.
   *
   * @param pdcId cheque
   * @param fromStatus previous status (null when registered)
   * @param toStatus new status
   * @param eventDate date
   * @param remarks remarks
   * @param receiptNo related receipt
   */
  public PdcEvent(
      Long pdcId,
      PdcStatus fromStatus,
      PdcStatus toStatus,
      LocalDate eventDate,
      String remarks,
      String receiptNo) {
    this.pdcId = pdcId;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.eventDate = eventDate;
    this.remarks = remarks;
    this.receiptNo = receiptNo;
  }

  public Long getPdcId() {
    return pdcId;
  }

  public PdcStatus getFromStatus() {
    return fromStatus;
  }

  public PdcStatus getToStatus() {
    return toStatus;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getReceiptNo() {
    return receiptNo;
  }
}
