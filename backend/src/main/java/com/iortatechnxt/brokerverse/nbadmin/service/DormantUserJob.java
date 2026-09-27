package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Job {@code UAM_DORMANT_USERS}: deactivates the users who have not signed in for {@code
 * UAM_DORMANT_DAYS} days (0 = never), counted from the last sign-in, or from the creation or the
 * last reactivation when later. Each deactivation is an access request of type Deactivate user with
 * the reason "No sign-in for the dormancy period", approved and applied by the system, so it has
 * the request history, the change log and the notice to the user like any other request. {@code
 * UAM_DORMANT_NOTICE_DAYS} days before, the user is told to sign in (in the app and by e-mail). The
 * holders of the System Administrator profile are never deactivated by the job. Daily at 00:15 PHT
 * ({@code brokerverse.jobs.uam-dormant-users-cron}); each user in a transaction of its own.
 */
@Component
public class DormantUserJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "UAM_DORMANT_USERS";

  /** Deactivation reason of the job (list UAM_DEACTIVATION_REASON). */
  public static final String REASON = "DORMANT";

  /** Group profile never deactivated by the job. */
  public static final String EXEMPT_PROFILE = "SYSADMIN";

  private static final Logger LOG = LoggerFactory.getLogger(DormantUserJob.class);

  private final AppUserRepository users;
  private final AccessRequestRepository requests;
  private final DocumentNumberService numbers;
  private final AccessRequestValidator validator;
  private final AccessChangeApplier applier;
  private final AccessRequestHistory history;
  private final AccessRequestNotifier notifier;
  private final AccessSettings settings;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param users users
   * @param requests access requests
   * @param numbers request numbers
   * @param validator request checks
   * @param applier applies the deactivation
   * @param history request history
   * @param notifier notices
   * @param settings parameters
   * @param transactions transaction manager (a transaction per user)
   * @param clock clock
   * @param cron schedule
   */
  public DormantUserJob(
      AppUserRepository users,
      AccessRequestRepository requests,
      DocumentNumberService numbers,
      AccessRequestValidator validator,
      AccessChangeApplier applier,
      AccessRequestHistory history,
      AccessRequestNotifier notifier,
      AccessSettings settings,
      PlatformTransactionManager transactions,
      Clock clock,
      @Value("${brokerverse.jobs.uam-dormant-users-cron:0 15 16 * * *}") String cron) {
    this.users = users;
    this.requests = requests;
    this.numbers = numbers;
    this.validator = validator;
    this.applier = applier;
    this.history = history;
    this.notifier = notifier;
    this.settings = settings;
    this.tx = new TransactionTemplate(transactions);
    this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Deactivates the users who have not signed in for the dormancy period, after a notice";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int days = settings.dormantDays();
    if (days <= 0) {
      return new JobOutcome(0, "Dormant-user deactivation is switched off (UAM_DORMANT_DAYS = 0)");
    }
    int noticeDays = settings.dormantNoticeDays();
    List<String> candidates = tx.execute(s -> candidates());
    List<String> deactivated = new ArrayList<>();
    int warned = 0;
    for (String username : candidates == null ? List.<String>of() : candidates) {
      try {
        Step step = tx.execute(s -> process(username, businessDate, days, noticeDays));
        if (step == Step.DEACTIVATED) {
          deactivated.add(username);
        } else if (step == Step.WARNED) {
          warned++;
        }
      } catch (RuntimeException ex) {
        LOG.warn("Dormant-user check of {} failed", username, ex);
      }
    }
    tx.executeWithoutResult(s -> notifier.dormantDeactivated(deactivated, days));
    return new JobOutcome(
        deactivated.size(),
        deactivated.size()
            + " dormant user(s) deactivated, "
            + warned
            + " user(s) told to sign in");
  }

  private List<String> candidates() {
    return users.findAll().stream()
        .filter(AppUser::isEnabled)
        .filter(u -> u.getRoles().stream().noneMatch(r -> EXEMPT_PROFILE.equals(r.getCode())))
        .map(AppUser::getUsername)
        .sorted()
        .toList();
  }

  /**
   * Checks one user: deactivates when the dormancy period has passed, warns on the notice day.
   *
   * @param username user
   * @param today business date
   * @param days dormancy period
   * @param noticeDays days of notice
   * @return what was done
   */
  Step process(String username, LocalDate today, int days, int noticeDays) {
    AppUser user = users.findByUsernameIgnoreCase(username).orElse(null);
    if (user == null || !user.isEnabled()) {
      return Step.NONE;
    }
    LocalDate deactivationDate = lastActivity(user).plusDays(days);
    if (!deactivationDate.isAfter(today)) {
      deactivate(user, days);
      return Step.DEACTIVATED;
    }
    if (noticeDays > 0 && deactivationDate.minusDays(noticeDays).equals(today)) {
      notifier.dormantWarning(user.getUsername(), deactivationDate);
      return Step.WARNED;
    }
    return Step.NONE;
  }

  private LocalDate lastActivity(AppUser user) {
    Instant last = user.getCreatedAt();
    if (user.getLastLoginAt() != null && user.getLastLoginAt().isAfter(last)) {
      last = user.getLastLoginAt();
    }
    Instant reactivated =
        requests
            .findFirstByUsernameIgnoreCaseAndRequestTypeAndAppliedAtNotNullOrderByAppliedAtDesc(
                user.getUsername(), AccessRequestType.ENABLE_USER)
            .map(AccessRequest::getAppliedAt)
            .orElse(null);
    if (reactivated != null && reactivated.isAfter(last)) {
      last = reactivated;
    }
    return BusinessClock.dateOf(last);
  }

  private void deactivate(AppUser user, int days) {
    Instant now = clock.instant();
    AccessRequestContent content =
        validator.recheck(
            new AccessRequestContent(
                    AccessRequestType.DISABLE_USER,
                    user.getUsername(),
                    null,
                    null,
                    Set.of(),
                    null,
                    "No sign-in for " + days + " days (dormant-user job)")
                .withUserData(new RequestedUserData(null, null, null, REASON, false, null)));
    AccessRequest r =
        requests.save(
            new AccessRequest(
                numbers.next("AR-" + BusinessClock.today(clock).getYear()), content, null));
    r.submit(List.of(), Set.of(), CurrentUser.SYSTEM, now);
    history.record(r, AccessRequestAction.SUBMIT, null, null);
    r.approve(AccessRequestStatus.APPROVED, CurrentUser.SYSTEM, now, null);
    applier.apply(r);
    history.record(r, AccessRequestAction.APPLY, AccessRequestStatus.PENDING, null);
    notifier.accessChanged(r);
  }

  /** What the job did for one user. */
  enum Step {
    NONE,
    WARNED,
    DEACTIVATED
  }
}
