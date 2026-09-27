package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EventSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * One status change of an instrument (DIS 2.8.0, 3.26.0): from and to status, what changed it
 * (user, system, uploaded file, job), a note and the file reference; shown as the status timeline.
 */
@Entity
@Table(name = "dsb_instrument_event")
public class InstrumentEvent extends BaseEntity {

  private static final int MAX_NOTE = 500;

  @Column(name = "instrument_id", nullable = false, updatable = false)
  private Long instrumentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", length = 20, updatable = false)
  private InstrumentStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, length = 20, updatable = false)
  private InstrumentStatus toStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private EventSource source;

  @Column(length = MAX_NOTE, updatable = false)
  private String note;

  @Column(name = "file_ref", length = 80, updatable = false)
  private String fileRef;

  protected InstrumentEvent() {}

  /**
   * A status change.
   *
   * @param instrumentId instrument
   * @param fromStatus previous status, null for the first one
   * @param toStatus new status
   * @param source what changed it
   * @param note note, may be null
   * @param fileRef upload or file reference, may be null
   */
  public InstrumentEvent(
      Long instrumentId,
      InstrumentStatus fromStatus,
      InstrumentStatus toStatus,
      EventSource source,
      String note,
      String fileRef) {
    this.instrumentId = instrumentId;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.source = source;
    this.note = note == null || note.length() <= MAX_NOTE ? note : note.substring(0, MAX_NOTE);
    this.fileRef = fileRef;
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

  public EventSource getSource() {
    return source;
  }

  public String getNote() {
    return note;
  }

  public String getFileRef() {
    return fileRef;
  }
}
