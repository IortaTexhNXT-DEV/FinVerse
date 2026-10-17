package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.ProcessingTrail;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Aging of endorsement requests from request to completion (ADJID.021). */
public final class RequestAging {

  private RequestAging() {}

  /**
   * Days a request has been in process.
   *
   * @param r request
   * @param now current time
   * @return days from submission (or creation) to completion (or now), in Philippine days
   */
  public static long days(EndorsementRequest r, Instant now) {
    ProcessingTrail trail = r.trail();
    Instant from = trail.submittedAt() != null ? trail.submittedAt() : r.getCreatedAt();
    Instant to = trail.completedAt() != null ? trail.completedAt() : now;
    if (from == null) {
      return 0;
    }
    return Math.max(0, ChronoUnit.DAYS.between(DocText.date(from), DocText.date(to)));
  }
}
