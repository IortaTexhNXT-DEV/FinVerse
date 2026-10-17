package com.iortatechnxt.brokerverse.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Free First Year tag of an account (BRNB.113): the first year's premium is not paid by the client
 * (e.g. a dealer or bank promotion). The end date is computed as one year from the start; a
 * cancelled tag keeps its dates and the cancellation for audit.
 *
 * @param active whether the account is currently tagged FFY
 * @param start FFY start
 * @param end FFY end (start + 1 year - 1 day)
 * @param cancelledAt cancellation time
 * @param cancelledBy user who cancelled
 * @param cancelReason cancellation reason
 */
@Embeddable
public record FreeFirstYear(
    @Column(name = "ffy", nullable = false) boolean active,
    @Column(name = "ffy_start") LocalDate start,
    @Column(name = "ffy_end") LocalDate end,
    @Column(name = "ffy_cancelled_at") Instant cancelledAt,
    @Column(name = "ffy_cancelled_by", length = 50) String cancelledBy,
    @Column(name = "ffy_cancel_reason", length = 300) String cancelReason) {

  /** Never tagged. */
  public static final FreeFirstYear NONE = new FreeFirstYear(false, null, null, null, null, null);

  /**
   * An active tag starting on a date; the end is one year later less one day.
   *
   * @param start start
   * @return tag
   */
  public static FreeFirstYear startingOn(LocalDate start) {
    return new FreeFirstYear(true, start, start.plusYears(1).minusDays(1), null, null, null);
  }

  /**
   * The tag cancelled (dates kept).
   *
   * @param when time
   * @param user user
   * @param reason reason
   * @return cancelled tag
   */
  public FreeFirstYear cancelled(Instant when, String user, String reason) {
    return new FreeFirstYear(false, start, end, when, user, reason);
  }
}
