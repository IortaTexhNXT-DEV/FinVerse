package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.eb.domain.EbActivity;
import com.iortatechnxt.brokerverse.eb.domain.EbActivityRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbDocument;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the received / released stamps of the TAT annex activities (BRID-022, 024; design 4.2):
 * the source of the TAT report and of the History tab of a programme. Called inside the business
 * transaction of each step.
 */
@Service
@Transactional
public class EbActivityLog {

  private final EbActivityRepository activities;
  private final Clock clock;

  /**
   * Creates the log.
   *
   * @param activities activity stamps
   * @param clock clock
   */
  public EbActivityLog(EbActivityRepository activities, Clock clock) {
    this.activities = activities;
    this.clock = clock;
  }

  /**
   * Stamps the start of an activity of a cycle.
   *
   * @param cycle cycle
   * @param activity activity
   * @param reference business reference, e.g. the cycle number
   * @param actor who performs it (user, or the party BDOI waits for)
   * @return the stamp
   */
  public EbActivity received(EbCycle cycle, TatActivity activity, String reference, String actor) {
    return activities.save(
        new EbActivity(
            cycle.getCompanyId(),
            new EbDocument.Place(cycle.getProgrammeId(), cycle.getId()),
            activity,
            reference,
            clock.instant(),
            actor));
  }

  /**
   * Stamps an activity of a cycle done at once (received and released now).
   *
   * @param cycle cycle
   * @param activity activity
   * @param reference business reference
   * @param actor user
   * @param remarks remarks, may be null
   */
  public void done(
      EbCycle cycle, TatActivity activity, String reference, String actor, String remarks) {
    EbActivity stamp = received(cycle, activity, reference, actor);
    stamp.release(stamp.getReceivedAt(), remarks);
  }

  /**
   * Stamps the end of the open activity of a reference, when there is one.
   *
   * @param activity activity
   * @param reference business reference
   * @param remarks remarks, may be null
   * @return true when an open stamp was released
   */
  public boolean released(TatActivity activity, String reference, String remarks) {
    Instant now = clock.instant();
    return activities
        .findFirstByActivityAndReferenceAndReleasedAtIsNullOrderByIdDesc(activity, reference)
        .map(
            stamp -> {
              stamp.release(now, remarks);
              return true;
            })
        .orElse(false);
  }
}
