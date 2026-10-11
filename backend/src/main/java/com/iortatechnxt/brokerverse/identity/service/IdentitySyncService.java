package com.iortatechnxt.brokerverse.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryProfile;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryProfileRepository;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryStatus;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventRepository;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventSource;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventStatus;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventType;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Receives and applies the provisioning events of UIDM-ISC and the status changes of the Enterprise
 * SSO platform (BDOI FRS FRUM.002.02, FRUM.003.01 to FRUM.003.03): every event is recorded first,
 * then applied in its own transaction; a refused or failed event stays recorded with its reason, is
 * alerted to the System Administrators and can be reprocessed. Also the on-demand synchronisation
 * of a user and the creation of a user from an active Enterprise SSO account (FRUM.002.01).
 */
@Service
public class IdentitySyncService {

  /** Setting: users from the Enterprise SSO platform / UIDM-ISC (conflict C01 option a). */
  public static final String SETTING = "UAM_SSO_PROVISIONING";

  /** Record type of the events in the audit trail. */
  public static final String ENTITY = "IdentityEvent";

  /** The generic answer to an Enterprise SSO account that cannot be used (no clues). */
  public static final String CANNOT_CREATE =
      "The user cannot be created from this Enterprise SSO account";

  private static final Logger LOG = LoggerFactory.getLogger(IdentitySyncService.class);

  private final IdentityEventRepository events;
  private final DirectoryProfileRepository profiles;
  private final AppUserRepository users;
  private final IdentityUserChanges changes;
  private final IdentityAlerts alerts;
  private final EnterpriseDirectory directory;
  private final SystemParameterService parameters;
  private final AuditTrailService audit;
  private final ObjectMapper json;
  private final TransactionTemplate tx;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param events events received
   * @param profiles directory details of the users
   * @param users users
   * @param changes changes to the users
   * @param alerts alerts of refused events
   * @param directory the Enterprise SSO directory
   * @param parameters business parameters
   * @param audit audit trail
   * @param json object mapper (the event as received)
   * @param transactions transaction manager
   * @param currentUser the user who reprocesses or synchronises
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the synchronisation
  public IdentitySyncService(
      IdentityEventRepository events,
      DirectoryProfileRepository profiles,
      AppUserRepository users,
      IdentityUserChanges changes,
      IdentityAlerts alerts,
      EnterpriseDirectory directory,
      SystemParameterService parameters,
      AuditTrailService audit,
      ObjectMapper json,
      PlatformTransactionManager transactions,
      CurrentUser currentUser,
      Clock clock) {
    this.events = events;
    this.profiles = profiles;
    this.users = users;
    this.changes = changes;
    this.alerts = alerts;
    this.directory = directory;
    this.parameters = parameters;
    this.audit = audit;
    this.json = json;
    this.tx = new TransactionTemplate(transactions);
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Whether users come from the Enterprise SSO platform / UIDM-ISC.
   *
   * @return the setting, true when it is missing
   */
  public boolean enabled() {
    return Boolean.parseBoolean(parameters.text(SETTING, "true").trim());
  }

  /**
   * Receives an event: records it and applies it.
   *
   * @param account the account it carries
   * @param type what it asks for
   * @param source where it comes from
   * @return the event with its outcome
   */
  public IdentityEvent receive(
      DirectoryAccount account, IdentityEventType type, IdentityEventSource source) {
    Long id =
        tx.execute(
            s ->
                events
                    .save(
                        new IdentityEvent(clock.instant(), source, type, account, payload(account)))
                    .getId());
    return process(id);
  }

  /**
   * Processes a refused or failed event again (after the data was corrected).
   *
   * @param id event
   * @return the event with its new outcome
   */
  public IdentityEvent reprocess(Long id) {
    tx.executeWithoutResult(
        s -> {
          IdentityEvent event = find(id);
          if (!event.reprocessable()) {
            throw new IdentityRefused(
                "NOT_REPROCESSABLE", "Only a refused or failed event can be processed again");
          }
          event.retried();
        });
    return process(id);
  }

  /**
   * Synchronises a user now from the Enterprise SSO directory (FRUM.002.02: on-demand
   * synchronisation).
   *
   * @param username user
   * @return the event of the synchronisation
   */
  public IdentityEvent synchronise(String username) {
    AppUser user =
        users
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
    if (user.getWindowsId() == null || user.getWindowsId().isBlank()) {
      throw new IdentityRefused(
          "NO_WINDOWS_ID", "User " + user.getUsername() + " has no Windows ID to synchronise");
    }
    Optional<DirectoryAccount> account = directory.find(user.getWindowsId());
    if (account.isEmpty()) {
      return notFound(user);
    }
    return receive(account.get(), IdentityEventType.MOVER, IdentityEventSource.ON_DEMAND);
  }

  /**
   * The account of a Windows ID, when it exists and is active (FRUM.002.01: validate the account
   * before creating the user). Any other case gets the same answer, which gives no clue.
   *
   * @param windowsId Windows ID
   * @return the account
   */
  public DirectoryAccount activeAccount(String windowsId) {
    if (windowsId == null || windowsId.isBlank()) {
      throw new IdentityRefused("SSO_ACCOUNT_INVALID", CANNOT_CREATE);
    }
    return directory
        .find(windowsId.trim())
        .filter(a -> a.status() == DirectoryStatus.ACTIVE)
        .orElseThrow(() -> new IdentityRefused("SSO_ACCOUNT_INVALID", CANNOT_CREATE));
  }

  /**
   * Creates a user from an existing and active Enterprise SSO account, with the details retrieved
   * from the platform (FRUM.002.01). The group profiles are given through a request.
   *
   * @param windowsId Windows ID
   * @return the event of the creation
   */
  public IdentityEvent createFromDirectory(String windowsId) {
    DirectoryAccount account = activeAccount(windowsId);
    IdentityEvent event = receive(account, IdentityEventType.JOINER, IdentityEventSource.ON_DEMAND);
    if (event.getStatus() != IdentityEventStatus.APPLIED) {
      throw new IdentityRefused("USER_NOT_CREATED", event.getMessage());
    }
    return event;
  }

  /**
   * The directory details of a user.
   *
   * @param username user
   * @return profile, empty when the user was never synchronised
   */
  public Optional<DirectoryProfile> profile(String username) {
    return tx.execute(s -> profiles.findByUsernameIgnoreCase(username));
  }

  /**
   * Name of the directory in use.
   *
   * @return name
   */
  public String directoryName() {
    return directory.name();
  }

  private IdentityEvent process(Long id) {
    String actor = currentUser.username();
    try {
      return tx.execute(s -> apply(find(id), actor));
    } catch (IdentityRefused ex) {
      return closed(id, IdentityEventStatus.REFUSED, ex.getMessage(), actor);
    } catch (RuntimeException ex) {
      LOG.warn("Identity event {} failed", id, ex);
      return closed(id, IdentityEventStatus.FAILED, "The event could not be applied", actor);
    }
  }

  private IdentityEvent closed(Long id, IdentityEventStatus status, String message, String actor) {
    return tx.execute(
        s -> {
          IdentityEvent event = find(id);
          event.processed(status, event.getUsername(), message, clock.instant(), actor);
          users
              .findByWindowsIdIgnoreCase(Optional.ofNullable(event.getWindowsId()).orElse(""))
              .flatMap(u -> profiles.findByUsernameIgnoreCase(u.getUsername()))
              .ifPresent(p -> p.failed(message, clock.instant(), actor));
          audit.recordIndependently(
              event.getSource().label(),
              ENTITY,
              event.getId(),
              AuditAction.REJECT,
              event.getEventType() + " of " + event.getWindowsId() + " " + status + ": " + message);
          alerts.refused(event);
          return event;
        });
  }

  private IdentityEvent apply(IdentityEvent event, String actor) {
    if (!enabled()) {
      throw new IdentityRefused(
          "PROVISIONING_OFF",
          "Users are maintained through access requests; the events of the Enterprise SSO"
              + " platform are not applied (setting "
              + SETTING
              + ")");
    }
    DirectoryAccount account = account(event);
    IdentityUserChanges.Source source =
        new IdentityUserChanges.Source(
            event.getSource() == IdentityEventSource.ON_DEMAND ? actor : event.getSource().label(),
            account.uidmRequestNo() == null ? "IDE-" + event.getId() : account.uidmRequestNo());
    Outcome outcome = outcome(event.getEventType(), account, source);
    DirectoryProfile profile =
        profiles
            .findByUsernameIgnoreCase(outcome.user().getUsername())
            .orElseGet(
                () ->
                    new DirectoryProfile(
                        outcome.user().getUsername(),
                        account.windowsId(),
                        clock.instant(),
                        source.actor()));
    profile.synchronise(account, event, clock.instant(), source.actor());
    profiles.save(profile);
    event.processed(
        outcome.changed() ? IdentityEventStatus.APPLIED : IdentityEventStatus.NO_CHANGE,
        outcome.user().getUsername(),
        outcome.text(),
        clock.instant(),
        actor);
    audit.recordIndependently(
        source.actor(), ENTITY, event.getId(), AuditAction.UPDATE, outcome.text());
    return event;
  }

  private Outcome outcome(
      IdentityEventType type, DirectoryAccount account, IdentityUserChanges.Source source) {
    if (type == IdentityEventType.JOINER) {
      AppUser created = changes.create(account, source);
      return new Outcome(created, true, "Created user " + created.getUsername());
    }
    AppUser user = existing(account);
    String name = user.getUsername();
    return switch (type) {
      case LEAVER -> {
        int n =
            changes.applyStatus(
                user,
                account.status().grantsAccess() ? DirectoryStatus.DEACTIVATED : account.status(),
                source);
        yield new Outcome(user, n > 0, "Deactivated user " + name + " and ended its open sessions");
      }
      case REHIRE -> {
        int n = changes.update(user, account.withStatus(DirectoryStatus.ACTIVE), source);
        yield new Outcome(
            user, n > 0, "Reactivated user " + name + " with the group profiles held before");
      }
      case STATUS -> {
        int n = changes.applyStatus(user, account.status(), source);
        yield new Outcome(
            user,
            n > 0,
            (account.status().grantsAccess() ? "Reactivated user " : "Deactivated user ")
                + name
                + " for the Enterprise SSO status "
                + DisplayFormat.label(account.status()));
      }
      default -> {
        int n = changes.update(user, account, source);
        yield new Outcome(user, n > 0, "Updated " + n + " detail(s) of user " + name);
      }
    };
  }

  private AppUser existing(DirectoryAccount account) {
    Optional<AppUser> byWindows =
        account.windowsId() == null
            ? Optional.empty()
            : users.findByWindowsIdIgnoreCase(account.windowsId());
    return byWindows
        .or(
            () ->
                account.userId() == null
                    ? Optional.empty()
                    : users.findByUsernameIgnoreCase(account.userId()))
        .orElseThrow(
            () ->
                new IdentityRefused(
                    "NO_SUCH_USER", "No user holds Windows ID " + account.windowsId()));
  }

  private IdentityEvent notFound(AppUser user) {
    DirectoryAccount missing =
        new DirectoryAccount(
            user.getWindowsId(),
            user.getUsername(),
            user.getEmail(),
            null,
            null,
            user.getFullName(),
            null,
            DirectoryStatus.INACTIVE,
            null,
            null,
            null,
            null);
    Long id =
        tx.execute(
            s ->
                events
                    .save(
                        new IdentityEvent(
                            clock.instant(),
                            IdentityEventSource.ON_DEMAND,
                            IdentityEventType.MOVER,
                            missing,
                            payload(missing)))
                    .getId());
    return closed(
        id,
        IdentityEventStatus.FAILED,
        "The Enterprise SSO account of " + user.getWindowsId() + " was not found",
        currentUser.username());
  }

  private IdentityEvent find(Long id) {
    return events
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Identity event", id));
  }

  private DirectoryAccount account(IdentityEvent event) {
    try {
      return json.readValue(event.getPayload(), DirectoryAccount.class);
    } catch (JsonProcessingException ex) {
      throw new IdentityRefused("UNREADABLE_EVENT", "The event as received cannot be read", ex);
    }
  }

  private String payload(DirectoryAccount account) {
    try {
      return json.writeValueAsString(account);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Account not serialisable", ex);
    }
  }

  /** What an event did. */
  private record Outcome(AppUser user, boolean changed, String text) {}
}
