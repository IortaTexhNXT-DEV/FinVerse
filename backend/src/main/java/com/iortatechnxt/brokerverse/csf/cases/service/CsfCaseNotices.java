package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.catalog.domain.SalesOfficer;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseSetupStore;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Notifications of the cases (FR-CSF-050 to 060): the members of the fulfilment unit when a case is
 * routed to it, in the system and by e-mail, with a copy to the account officer and to the unit of
 * the account of the case (FRS BRD-9 v1.2, BRCSFCM.012, FR-CSF-050 and FR-CSF-060), the owner when
 * the unit resolves, returns or asks for action, the escalation group of the unit (default the
 * supervisors) on an escalation, and all of them on the SLA notices. Each notice names the Case ID,
 * the policy number, the case type, the nature of transaction and the due date, and links to the
 * case.
 */
@Component
public class CsfCaseNotices {

  /** Notification event of the routing notice (in the system and by e-mail by default). */
  public static final String EVENT_ROUTED = "CSF_CASE_ROUTED";

  private final NotificationService notifications;
  private final NoticeDelivery delivery;
  private final SalesOrganisationService salesOrganisation;
  private final CsfCaseSetupStore setup;
  private final LovService lovs;
  private final AccountQueryService accounts;

  /**
   * Creates the notices.
   *
   * @param notifications in-app notices
   * @param setup members and escalation groups
   * @param lovs labels
   * @param accounts accounts (the account officer and the sales unit of the account of a case)
   * @param delivery notices in the system and by e-mail, as each user chose for the event
   * @param salesOrganisation the officers and the head of the sales unit of an account
   */
  public CsfCaseNotices(
      NotificationService notifications,
      CsfCaseSetupStore setup,
      LovService lovs,
      AccountQueryService accounts,
      NoticeDelivery delivery,
      SalesOrganisationService salesOrganisation) {
    this.notifications = notifications;
    this.delivery = delivery;
    this.salesOrganisation = salesOrganisation;
    this.setup = setup;
    this.lovs = lovs;
    this.accounts = accounts;
  }

  /**
   * Notifies the routing of a case to a fulfilment unit (FR-CSF-050, FR-CSF-060): its members, with
   * a copy to the account officer of the account of the case and to the unit of the account - the
   * account officers and the head of the account's sales unit - so that the BU/AO ownership of the
   * account is kept (FRS BRD-9 v1.2). Each is told in the system and by e-mail, as each chose for
   * the event {@value #EVENT_ROUTED}; a user in several of these groups is told once, as a member
   * of the fulfilment unit first. A case without an account sends no copy.
   *
   * @param c case
   * @param what what happened, e.g. "routed to your unit"
   */
  public void routed(CsfCase c, String what) {
    List<String> told = new ArrayList<>();
    Notice toMembers = notice(c, what);
    setup.membersOf(c.category().fulfilmentUnit()).forEach(m -> tell(told, m, toMembers));
    String copy = what.replace("your unit", "the fulfilment unit");
    Optional<Account> account = account(c);
    account
        .map(a -> a.getSales().accountOfficer())
        .filter(ao -> !ao.isBlank())
        .ifPresent(ao -> tell(told, ao, notice(c, copy + " (copy for the account officer)")));
    account.ifPresent(
        a -> {
          Notice toUnit = notice(c, copy + " (copy for the unit of the account)");
          unitOf(a).forEach(u -> tell(told, u, toUnit));
        });
  }

  private void tell(List<String> told, String username, Notice notice) {
    if (username != null && told.stream().noneMatch(t -> CurrentUser.sameUser(t, username))) {
      told.add(username);
      delivery.toUser(username, notice, EVENT_ROUTED, true);
    }
  }

  /** The account of a case, when the case names one. */
  private Optional<Account> account(CsfCase c) {
    Long accountId = c.account() == null ? null : c.account().accountId();
    return accountId == null ? Optional.empty() : Optional.of(accounts.get(accountId));
  }

  /**
   * The unit of an account: the account officers of its sales unit (team) and the head of the unit
   * (else of its department or region).
   */
  private List<String> unitOf(Account a) {
    String team = a.getSales().team();
    List<String> users = new ArrayList<>();
    if (team == null || team.isBlank()) {
      return users;
    }
    salesOrganisation.officers(a.getCompanyId()).stream()
        .filter(o -> o.isActive() && team.equals(o.getTeamCode()))
        .map(SalesOfficer::getUsername)
        .forEach(users::add);
    salesOrganisation.unitHead(a.getCompanyId(), team).ifPresent(users::add);
    return users;
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
