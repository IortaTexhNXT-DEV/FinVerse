package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpBillingRepository;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Insurer feedback timeline (CMRID.011): every billing still awaiting the insurer after its due
 * date ({@code CMR_FEEDBACK_WORKING_DAYS} working days after sending) raises the {@code
 * DP_FEEDBACK_OVERDUE} alert and notifies its handler and the commission team once.
 */
@Component
public class DpFeedbackSlaJob implements ManagedJob {

  /** Job name. */
  public static final String NAME = "DP_FEEDBACK_SLA";

  /** Alert code and notification event. */
  public static final String OVERDUE = "DP_FEEDBACK_OVERDUE";

  private final DpBillingRepository billings;
  private final AlertService alerts;
  private final NotificationService notifications;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param billings billings
   * @param alerts alerts
   * @param notifications notifications
   * @param txManager transaction manager
   * @param cron schedule ({@code brokerverse.jobs.dp-feedback-sla-cron})
   */
  public DpFeedbackSlaJob(
      DpBillingRepository billings,
      AlertService alerts,
      NotificationService notifications,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.dp-feedback-sla-cron:-}") String cron) {
    this.billings = billings;
    this.alerts = alerts;
    this.notifications = notifications;
    this.tx = new TransactionTemplate(txManager);
    this.cron = cron;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Flags direct payment billings whose insurer feedback is overdue (CMRID.011)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    Integer flagged =
        tx.execute(
            s -> {
              int n = 0;
              for (DpBilling b :
                  billings.findByStageAndSlaDueBeforeAndOverdueAlertedFalseOrderByIdAsc(
                      DpBilling.AWAITING, businessDate)) {
                flag(b);
                n++;
              }
              return n;
            });
    int count = flagged == null ? 0 : flagged;
    return new JobOutcome(count, count + " billing(s) with overdue insurer feedback flagged");
  }

  private void flag(DpBilling b) {
    String message =
        "Insurer "
            + b.getInsurerCode()
            + " has not answered billing "
            + b.getBillingNo()
            + " due "
            + b.getSlaDue();
    alerts.raise(
        OVERDUE,
        new AlertFacts(
            b.getCompanyId(),
            null,
            DpBillingService.ENTITY,
            String.valueOf(b.getId()),
            message,
            b.getTotalNet(),
            OVERDUE + ":" + b.getId()));
    Notice notice =
        new Notice(
            "Insurer feedback overdue: " + b.getBillingNo(),
            message,
            "/commission/billings/" + b.getId(),
            DpBillingService.ENTITY,
            String.valueOf(b.getId()));
    if (b.getHandler() != null) {
      notifications.notifyUser(b.getHandler(), notice, OVERDUE);
    }
    notifications.notifyPermission("COMMREC_APPROVE", notice, OVERDUE);
    b.overdueAlerted();
  }
}
