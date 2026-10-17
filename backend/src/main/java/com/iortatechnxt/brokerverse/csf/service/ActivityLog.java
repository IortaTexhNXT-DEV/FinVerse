package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.csf.domain.ActivityAction;
import com.iortatechnxt.brokerverse.csf.domain.CsfActivity;
import com.iortatechnxt.brokerverse.csf.domain.CsfActivityRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The agent activity log (FR-CSF-042): one row per action, written in its own transaction so that
 * the reads of the Servicing View (read-only transactions) and the refused actions are logged too.
 * Append-only; it cannot be switched off.
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ActivityLog {

  private final CsfActivityRepository activities;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the log.
   *
   * @param activities activity rows
   * @param currentUser current user
   * @param clock clock
   */
  public ActivityLog(CsfActivityRepository activities, CurrentUser currentUser, Clock clock) {
    this.activities = activities;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records an action of the current user.
   *
   * @param companyId company
   * @param action action
   * @param subject client, reference and detail
   * @return the row
   */
  public CsfActivity record(Long companyId, ActivityAction action, CsfActivity.Subject subject) {
    return activities.save(
        new CsfActivity(companyId, currentUser.username(), action, subject, clock.instant()));
  }
}
