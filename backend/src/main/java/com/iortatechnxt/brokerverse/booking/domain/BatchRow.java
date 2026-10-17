package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Result of one account in a booking batch run: booked with its invoice, or failed with the reason
 * (partial success, BRNB.036).
 *
 * @param arn account
 * @param outcome BOOKED or FAILED
 * @param invoiceNo invoice booked, null on failure
 * @param message failure reason, null on success
 */
@Embeddable
public record BatchRow(
    @Column(name = "arn", nullable = false, length = 30) String arn,
    @Column(name = "outcome", nullable = false, length = 10) String outcome,
    @Column(name = "invoice_no", length = 40) String invoiceNo,
    @Column(name = "message", length = 1000) String message) {

  /** Outcome of a booked row. */
  public static final String BOOKED = "BOOKED";

  /** Outcome of a failed row. */
  public static final String FAILED = "FAILED";

  private static final int MAX_MESSAGE = 1000;

  /**
   * A booked row.
   *
   * @param arn account
   * @param invoiceNo invoice
   * @return row
   */
  public static BatchRow booked(String arn, String invoiceNo) {
    return new BatchRow(arn, BOOKED, invoiceNo, null);
  }

  /**
   * A failed row.
   *
   * @param arn account
   * @param message reason
   * @return row
   */
  public static BatchRow failed(String arn, String message) {
    String text =
        message == null || message.length() <= MAX_MESSAGE
            ? message
            : message.substring(0, MAX_MESSAGE);
    return new BatchRow(arn, FAILED, null, text);
  }

  /**
   * Whether the row was booked.
   *
   * @return true when booked
   */
  public boolean isBooked() {
    return BOOKED.equals(outcome);
  }
}
