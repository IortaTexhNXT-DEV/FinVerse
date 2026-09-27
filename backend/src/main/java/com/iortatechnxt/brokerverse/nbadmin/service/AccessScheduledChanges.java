package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Applies approved requests on their effective date (UAM-NFR-14; FR-UA-020): each due SCHEDULED
 * request is checked again and applied in its own transaction. A request that fails the check or
 * the application stays SCHEDULED with the reason, and raises alert {@value #APPLY_FAILED}.
 */
@Service
public class AccessScheduledChanges {

  /** Alert of a scheduled change that could not be applied. */
  public static final String APPLY_FAILED = "UAM_SCHEDULED_APPLY_FAILED";

  private final AccessRequestRepository requests;
  private final AccessRequestValidator validator;
  private final AccessChangeApplier applier;
  private final AccessRequestHistory history;
  private final AccessRequestNotifier notifier;
  private final AlertService alerts;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param validator request checks
   * @param applier applies approved changes
   * @param history request history
   * @param notifier notifications
   * @param alerts alerts
   * @param transactions transaction manager
   * @param clock clock
   */
  public AccessScheduledChanges(
      AccessRequestRepository requests,
      AccessRequestValidator validator,
      AccessChangeApplier applier,
      AccessRequestHistory history,
      AccessRequestNotifier notifier,
      AlertService alerts,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.requests = requests;
    this.validator = validator;
    this.applier = applier;
    this.history = history;
    this.notifier = notifier;
    this.alerts = alerts;
    this.tx = new TransactionTemplate(transactions);
    this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  /**
   * Applies the scheduled requests due on a date.
   *
   * @param businessDate business date
   * @return requests applied and failed
   */
  public Result applyDue(LocalDate businessDate) {
    List<Long> due =
        tx.execute(
            s ->
                requests
                    .findByStatusAndEffectiveFromLessThanEqualOrderByIdAsc(
                        AccessRequestStatus.SCHEDULED, businessDate)
                    .stream()
                    .map(AccessRequest::getId)
                    .toList());
    int applied = 0;
    int failed = 0;
    for (Long id : due == null ? List.<Long>of() : due) {
      try {
        tx.executeWithoutResult(s -> apply(id));
        applied++;
      } catch (RuntimeException e) {
        tx.executeWithoutResult(s -> failed(id, e.getMessage()));
        failed++;
      }
    }
    return new Result(applied, failed);
  }

  private void apply(Long id) {
    AccessRequest r = requests.findById(id).orElseThrow();
    validator.recheck(r.content());
    applier.apply(r);
    r.applied(clock.instant());
    history.record(r, AccessRequestAction.APPLY, AccessRequestStatus.SCHEDULED, null);
    notifier.decided(r, "applied on its effective date");
    notifier.accessChanged(r);
  }

  private void failed(Long id, String message) {
    AccessRequest r = requests.findById(id).orElseThrow();
    r.applyFailed(message);
    history.record(r, AccessRequestAction.APPLY_FAILED, AccessRequestStatus.SCHEDULED, message);
    alerts.raise(
        APPLY_FAILED,
        new AlertFacts(
            null,
            null,
            AccessRequestService.ENTITY,
            r.getRequestNo(),
            "Access request "
                + r.getRequestNo()
                + " could not be applied on "
                + r.getEffectiveFrom()
                + ": "
                + message,
            null,
            APPLY_FAILED + ":" + r.getRequestNo()));
  }

  /**
   * Outcome of a run.
   *
   * @param applied requests applied
   * @param failed requests that stay scheduled
   */
  public record Result(int applied, int failed) {}
}
