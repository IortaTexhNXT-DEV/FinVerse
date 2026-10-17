package com.iortatechnxt.brokerverse.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The dismissal of a refused provisioning event, with the administrator's remark: the event stays
 * as received (append-only) and leaves the list of refused events to review.
 */
@Entity
@Table(name = "idn_event_dismissal")
public class IdentityEventDismissal {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, updatable = false)
  private Long eventId;

  @Column(nullable = false, length = 500, updatable = false)
  private String remark;

  @Column(name = "dismissed_by", nullable = false, length = 50, updatable = false)
  private String dismissedBy;

  @Column(name = "dismissed_at", nullable = false, updatable = false)
  private Instant dismissedAt;

  protected IdentityEventDismissal() {}

  /**
   * Dismisses an event.
   *
   * @param eventId the refused event
   * @param remark why it needs no further action
   * @param dismissedBy administrator
   * @param dismissedAt time
   */
  public IdentityEventDismissal(
      Long eventId, String remark, String dismissedBy, Instant dismissedAt) {
    this.eventId = eventId;
    this.remark = remark;
    this.dismissedBy = dismissedBy;
    this.dismissedAt = dismissedAt;
  }

  public Long getEventId() {
    return eventId;
  }

  public String getRemark() {
    return remark;
  }

  public String getDismissedBy() {
    return dismissedBy;
  }

  public Instant getDismissedAt() {
    return dismissedAt;
  }
}
