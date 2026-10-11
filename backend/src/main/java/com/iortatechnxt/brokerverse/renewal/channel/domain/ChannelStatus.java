package com.iortatechnxt.brokerverse.renewal.channel.domain;

/** Delivery status of a channel message (FRRN.017.02, FRRN.022.02). */
public enum ChannelStatus {
  /** Created, not yet transmitted. */
  PENDING_TRANSMISSION("Pending Transmission"),
  /** Accepted by the channel (CCM transaction reference received). */
  SUBMITTED("Submitted to CCM"),
  /** Sent by the channel to the recipient. */
  SENT("Sent"),
  /** Delivered to the recipient. */
  DELIVERED("Delivered"),
  /** Refused or not delivered. */
  FAILED("Failed"),
  /** Withdrawn before transmission. */
  CANCELLED("Cancelled"),
  /** An inbound file received from the channel. */
  RECEIVED("Received"),
  /** An inbound file processed. */
  PROCESSED("Processed");

  private final String label;

  ChannelStatus(String label) {
    this.label = label;
  }

  /**
   * The status as users read it.
   *
   * @return label
   */
  public String label() {
    return label;
  }

  /**
   * Whether the status may still change by the channel.
   *
   * @return true for a message on its way
   */
  public boolean inFlight() {
    return this == SUBMITTED || this == SENT;
  }
}
