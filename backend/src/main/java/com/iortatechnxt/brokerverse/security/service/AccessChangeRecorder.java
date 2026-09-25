package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.domain.AccessChange;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeActivity;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLog;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLogRepository;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeSource;
import com.iortatechnxt.brokerverse.security.domain.AccessSubjectType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the structured access change log (BRD 4.003.1; UAM-NFR-41): one row per changed attribute
 * of a user or role, with the request number, the actor and the approver. Called by {@link
 * UserAdminService} inside the transaction of the change, so a change and its evidence commit or
 * roll back together.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class AccessChangeRecorder {

  private final AccessChangeLogRepository log;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the recorder.
   *
   * @param log change log
   * @param currentUser current user (the actor)
   * @param clock clock
   */
  public AccessChangeRecorder(AccessChangeLogRepository log, CurrentUser currentUser, Clock clock) {
    this.log = log;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records one change.
   *
   * @param change what changed
   * @param authority request and approver
   */
  public void record(AccessChange change, ChangeAuthority authority) {
    Instant now = clock.instant();
    log.save(new AccessChangeLog(change, source(authority), now));
  }

  /**
   * Records every attribute whose value differs between two snapshots.
   *
   * @param subject subject type and name, and the activity of the changed attributes
   * @param before attribute values before (empty for a creation)
   * @param after attribute values after
   * @param authority request and approver
   * @return number of rows written
   */
  public int recordDifferences(
      Subject subject,
      Map<String, String> before,
      Map<String, String> after,
      ChangeAuthority authority) {
    List<String> changed =
        after.keySet().stream()
            .filter(k -> !Objects.equals(before.get(k), after.get(k)))
            .sorted()
            .toList();
    changed.forEach(
        k ->
            record(
                new AccessChange(
                    subject.type(),
                    subject.name(),
                    subject.activityOf(k),
                    k,
                    before.get(k),
                    after.get(k)),
                authority));
    return changed.size();
  }

  private AccessChangeSource source(ChangeAuthority authority) {
    return new AccessChangeSource(
        authority.requestNo(), currentUser.username(), authority.approvedBy());
  }

  /**
   * The subject of a set of attribute changes and the activity each attribute stands for.
   *
   * @param type user or role
   * @param name user name or role code
   * @param activity activity of ordinary attributes
   * @param special activity of particular attributes (e.g. roles -> ROLES_CHANGED)
   */
  public record Subject(
      AccessSubjectType type,
      String name,
      AccessChangeActivity activity,
      Map<String, AccessChangeActivity> special) {

    /** Defensive copy. */
    public Subject {
      special = special == null ? Map.of() : Map.copyOf(special);
    }

    /**
     * Activity of one attribute.
     *
     * @param attribute attribute
     * @return activity
     */
    public AccessChangeActivity activityOf(String attribute) {
      return special.getOrDefault(attribute, activity);
    }
  }
}
