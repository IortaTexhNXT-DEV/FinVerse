package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * The placement side of a renewal account after its submission for placement (FRRN.029 to
 * FRRN.033): the placement status (For Booking, Rejected Placement, Booked, For E-Policy Sending,
 * E-Policy Sent), the For Booking Only tag and the e-policy number with the upload that set it.
 */
@Embeddable
public class CandidatePlacement {

  /** Placement status: sent to the insurer, to be booked. */
  public static final String FOR_BOOKING = "FOR_BOOKING";

  /** Placement status: rejected by the insurer. */
  public static final String REJECTED_PLACEMENT = "REJECTED_PLACEMENT";

  /** Placement status: approved by the insurer and booked. */
  public static final String BOOKED = "BOOKED";

  /** Placement status: e-policy received, to be sent to the client. */
  public static final String FOR_EPOLICY_SENDING = "FOR_EPOLICY_SENDING";

  /** Placement status: e-policy sent to the client. */
  public static final String EPOLICY_SENT = "EPOLICY_SENT";

  @Column(name = "placement_status", length = 30)
  private String status;

  @Column(name = "for_booking_only", nullable = false)
  private boolean forBookingOnly;

  @Column(name = "epolicy_no", length = 60)
  private String epolicyNo;

  @Column(name = "epolicy_batch", length = 40)
  private String epolicyBatch;

  @Column(name = "epolicy_updated_at")
  private Instant epolicyUpdatedAt;

  @Column(name = "epolicy_updated_by", length = 50)
  private String epolicyUpdatedBy;

  /**
   * Sets the placement status.
   *
   * @param newStatus status, null for Submitted for Placement
   */
  public void status(String newStatus) {
    this.status = newStatus;
  }

  /**
   * Tags or untags the account For Booking Only.
   *
   * @param tagged whether tagged
   */
  public void forBookingOnly(boolean tagged) {
    this.forBookingOnly = tagged;
  }

  /**
   * Records the e-policy number.
   *
   * @param number e-policy number
   * @param batch upload or receipt that set it
   * @param user user
   * @param at time
   */
  public void epolicy(String number, String batch, String user, Instant at) {
    this.epolicyNo = number;
    this.epolicyBatch = batch;
    this.epolicyUpdatedBy = user;
    this.epolicyUpdatedAt = at;
  }

  public String getStatus() {
    return status;
  }

  public boolean isForBookingOnly() {
    return forBookingOnly;
  }

  public String getEpolicyNo() {
    return epolicyNo;
  }

  public String getEpolicyBatch() {
    return epolicyBatch;
  }

  public Instant getEpolicyUpdatedAt() {
    return epolicyUpdatedAt;
  }

  public String getEpolicyUpdatedBy() {
    return epolicyUpdatedBy;
  }
}
