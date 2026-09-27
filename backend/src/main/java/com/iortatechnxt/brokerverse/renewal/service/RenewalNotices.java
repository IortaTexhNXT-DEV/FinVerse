package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Notifications of the Renewal events (RENEWAL_DESIGN section 9): to the assigned users, to the
 * Team Leaders of a sales unit, or to the holders of a permission, with the link of the renewal.
 */
@Component
public class RenewalNotices {

  private final NotificationService notifications;
  private final RenewalScope scope;
  private final AppUserRepository users;
  private final CurrentUser currentUser;

  /**
   * Creates the notifier.
   *
   * @param notifications notifications
   * @param scope sales units of the Team Leaders
   * @param users users holding a permission
   * @param currentUser current user (never notified of his own action)
   */
  public RenewalNotices(
      NotificationService notifications,
      RenewalScope scope,
      AppUserRepository users,
      CurrentUser currentUser) {
    this.notifications = notifications;
    this.scope = scope;
    this.users = users;
    this.currentUser = currentUser;
  }

  /**
   * Notifies users of a renewal event.
   *
   * @param usernames users (null entries ignored)
   * @param event notification event
   * @param candidate renewal
   * @param text title and body
   */
  public void users(
      Collection<String> usernames, String event, RenewalCandidate candidate, Text text) {
    Set<String> distinct = new LinkedHashSet<>();
    usernames.stream()
        .filter(u -> u != null && !CurrentUser.sameUser(u, currentUser.username()))
        .forEach(distinct::add);
    distinct.forEach(u -> notifications.notifyUser(u, notice(candidate, text), event));
  }

  /**
   * Notifies the Team Leaders ({@code RNW_REVIEW}) whose units include a sales unit.
   *
   * @param companyId company
   * @param unit sales unit
   * @param event notification event
   * @param candidate renewal
   * @param text title and body
   */
  public void teamLeaders(
      Long companyId, String unit, String event, RenewalCandidate candidate, Text text) {
    users(
        users.findUsernamesWithPermission(Permission.RNW_REVIEW).stream()
            .filter(u -> scope.unitsOf(companyId, u).contains(unit))
            .toList(),
        event,
        candidate,
        text);
  }

  /**
   * Notifies every holder of a permission.
   *
   * @param permission permission
   * @param event notification event
   * @param text title and body
   * @param link route to open
   */
  public void holders(Permission permission, String event, Text text, String link) {
    notifications.notifyPermission(
        permission.name(), new Notice(text.title(), text.body(), link, null, null), event);
  }

  private static Notice notice(RenewalCandidate candidate, Text text) {
    return new Notice(
        text.title(),
        text.body(),
        RenewalCodes.LINK + candidate.getRenewalRef(),
        RenewalCodes.ENTITY,
        String.valueOf(candidate.getId()));
  }

  /**
   * Title and body of a notification.
   *
   * @param title title
   * @param body body
   */
  public record Text(String title, String body) {}
}
