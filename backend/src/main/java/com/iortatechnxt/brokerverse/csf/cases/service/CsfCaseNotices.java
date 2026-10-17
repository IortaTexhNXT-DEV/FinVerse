package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Notifications of the cases (FR-CSF-050 to 060): the members of the fulfilment unit when a case is
 * routed to it, the owner when the unit resolves, returns or asks for action, the escalation group
 * of the unit (default the supervisors) on an escalation, and all of them on the SLA notices. Each
 * notice names the Case ID, the policy number, the case type, the nature of transaction and the due
 * date, and links to the case.
 */
@Component
public class CsfCaseNotices {

  private final NotificationService notifications;
  private final CsfCaseSetupStore setup;
  private final LovService lovs;

  /**
   * Creates the notices.
   *
   * @param notifications in-app notices
   * @param setup members and escalation groups
   * @param lovs labels
   */
  public CsfCaseNotices(
      NotificationService notifications, CsfCaseSetupStore setup, LovService lovs) {
    this.notifications = notifications;
    this.setup = setup;
    this.lovs = lovs;
  }

  /**
   * Notifies the members of the fulfilment unit of a case.
   *
   * @param c case
   * @param what what happened
   */
  public void toUnit(CsfCase c, String what) {
    Notice n = notice(c, what);
    setup.membersOf(c.category().fulfilmentUnit()).forEach(u -> notifications.notifyUser(u, n));
  }

  /**
   * Notifies the owner of a case.
   *
   * @param c case
   * @param what what happened
   */
  public void toOwner(CsfCase c, String what) {
    notifications.notifyUser(c.owner(), notice(c, what));
  }

  /**
   * Notifies the owner and the unit of a change of category.
   *
   * @param c case
   */
  public void categoryChanged(CsfCase c) {
    toOwner(c, "has a new category");
    toUnit(c, "has a new category");
  }

  /**
   * Notifies the escalation group and the owner of an escalation.
   *
   * @param c case
   * @param reason reason in words
   */
  public void escalated(CsfCase c, String reason) {
    Notice n = notice(c, "escalated: " + reason);
    escalationGroup(c).forEach(u -> notifications.notifyUser(u, n));
    if (escalationUnit(c).isEmpty()) {
      notifications.notifyPermission(Permission.CSF_CASE_SUPERVISE.name(), n);
    }
    notifications.notifyUser(c.owner(), n);
  }

  /**
   * Notifies an SLA threshold: the owner, the supervisors, the unit and the escalation group.
   *
   * @param c case
   * @param what what happened
   */
  public void sla(CsfCase c, String what) {
    Notice n = notice(c, what);
    Set<String> users = new LinkedHashSet<>();
    users.add(c.owner());
    users.addAll(setup.membersOf(c.category().fulfilmentUnit()));
    users.addAll(escalationGroup(c));
    users.forEach(u -> notifications.notifyUser(u, n));
    notifications.notifyPermission(Permission.CSF_CASE_SUPERVISE.name(), n);
  }

  private Set<String> escalationGroup(CsfCase c) {
    Set<String> users = new LinkedHashSet<>();
    escalationUnit(c).ifPresent(u -> users.addAll(setup.membersOf(u)));
    return users;
  }

  private Optional<String> escalationUnit(CsfCase c) {
    return setup.routes().stream()
        .filter(r -> r.active() && r.fulfilmentUnit().equals(c.category().fulfilmentUnit()))
        .map(CsfCaseSetupStore.Route::escalationGroup)
        .filter(g -> g != null && !g.isBlank())
        .findFirst();
  }

  private Notice notice(CsfCase c, String what) {
    String policy = c.account().policyNo() == null ? "" : ", policy " + c.account().policyNo();
    return new Notice(
        c.caseNo() + " " + what,
        lovs.label(CsfCaseCodes.LOV_TYPE, c.category().caseType())
            + ", "
            + lovs.label(CsfCaseCodes.LOV_NATURE, c.category().nature())
            + policy
            + ", due "
            + c.dueDate(),
        CsfCaseCodes.LINK + c.id(),
        CsfCaseCodes.ENTITY,
        String.valueOf(c.id()));
  }
}
