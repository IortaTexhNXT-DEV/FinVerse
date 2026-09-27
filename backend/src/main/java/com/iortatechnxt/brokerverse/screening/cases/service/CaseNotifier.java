package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * In-app notices of screening cases (SNSRP-303, 405, 702-704, 801, 802): each notice opens the case
 * page and carries its notification event, so the users' preferences apply and the event is logged
 * (FR-SS-080). The user who caused the notice is never notified.
 */
@Component
@Transactional
public class CaseNotifier {

  private final NotificationService notifications;
  private final RoleMembers members;
  private final CurrentUser currentUser;

  /**
   * Creates the notifier.
   *
   * @param notifications notifications
   * @param members role and permission holders
   * @param currentUser current user
   */
  public CaseNotifier(
      NotificationService notifications, RoleMembers members, CurrentUser currentUser) {
    this.notifications = notifications;
    this.members = members;
    this.currentUser = currentUser;
  }

  /**
   * Notifies users.
   *
   * @param users recipients (duplicates and the actor are skipped)
   * @param c the case
   * @param eventCode notification event
   * @param what the headline after the case number
   * @return users notified
   */
  public List<String> users(
      Collection<String> users, ScreeningCase c, String eventCode, String what) {
    Set<String> sent = new LinkedHashSet<>();
    String actor = currentUser.username();
    for (String user : users) {
      if (user != null
          && !CurrentUser.sameUser(user, actor)
          && sent.stream().noneMatch(u -> CurrentUser.sameUser(u, user))
          && notifications.notifyUser(user, notice(c, what), eventCode)) {
        sent.add(user);
      }
    }
    return List.copyOf(sent);
  }

  /**
   * Notifies one user.
   *
   * @param user recipient, may be null
   * @param c the case
   * @param eventCode notification event
   * @param what the headline
   * @return users notified
   */
  public List<String> user(String user, ScreeningCase c, String eventCode, String what) {
    return user == null ? List.of() : users(List.of(user), c, eventCode, what);
  }

  /**
   * Notifies the holders of a role.
   *
   * @param roleCode role
   * @param c the case
   * @param eventCode notification event
   * @param what the headline
   * @return users notified
   */
  public List<String> role(String roleCode, ScreeningCase c, String eventCode, String what) {
    return users(members.ofRole(roleCode), c, eventCode, what);
  }

  /**
   * Notifies the assignee, or the holders of the stage permission when the case waits in the stage
   * queue.
   *
   * @param c the case
   * @param permission the stage permission
   * @param eventCode notification event
   * @param what the headline
   * @return users notified
   */
  public List<String> owner(ScreeningCase c, String permission, String eventCode, String what) {
    return c.getAssignee() != null
        ? user(c.getAssignee(), c, eventCode, what)
        : users(members.withPermission(permission), c, eventCode, what);
  }

  private static Notice notice(ScreeningCase c, String what) {
    return new Notice(
        c.getCaseNo() + ": " + what,
        c.getClientName() + " (" + c.getClientCode() + ") - " + c.getCaseType(),
        CaseCodes.link(c.getId()),
        CaseCodes.ENTITY,
        String.valueOf(c.getId()));
  }
}
