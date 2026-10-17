package com.iortatechnxt.brokerverse.eb.franchise.service;

import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequestRepository;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code EB_FRANCHISE_EXPIRY} (BRID-027; FR-EB-032; cron {@code
 * brokerverse.jobs.eb-franchise-expiry-cron}, 06:30 Manila): a franchise request still without a
 * decision {@code EB_FRANCHISE_GRACE_DAYS} working days after its decision date expires; the alert
 * {@code EB_FRANCHISE_OVERDUE} is raised by the daily alert check from the decision date on. Each
 * request runs in its own transaction.
 */
@Component
public class FranchiseExpiryJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "EB_FRANCHISE_EXPIRY";

  private static final Logger LOG = LoggerFactory.getLogger(FranchiseExpiryJob.class);

  private final EbFranchiseRequestRepository requests;
  private final FranchiseService service;
  private final EbParameters parameters;
  private final EbWorkingDays workingDays;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param requests franchise requests
   * @param service expiry of one request
   * @param parameters grace days
   * @param workingDays working-day calendars
   * @param cron schedule ({@code brokerverse.jobs.eb-franchise-expiry-cron})
   */
  public FranchiseExpiryJob(
      EbFranchiseRequestRepository requests,
      FranchiseService service,
      EbParameters parameters,
      EbWorkingDays workingDays,
      @Value("${brokerverse.jobs.eb-franchise-expiry-cron:-}") String cron) {
    this.requests = requests;
    this.service = service;
    this.parameters = parameters;
    this.workingDays = workingDays;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Expires Employee Benefits franchise requests left without an insurer decision";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int grace = parameters.franchiseGraceDays();
    List<EbFranchiseRequest> due =
        requests.findByStatusAndDueDateBeforeOrderByDueDateAscIdAsc(
            EbFranchiseRequest.Status.SUBMITTED, businessDate);
    int expired = 0;
    int failed = 0;
    for (EbFranchiseRequest request : due) {
      LocalDate end = workingDays.plus(request.getCompanyId(), request.getDueDate(), grace);
      if (end.isBefore(businessDate)) {
        try {
          service.expireById(request.getId());
          expired++;
        } catch (RuntimeException e) {
          failed++;
          LOG.warn("Expiry of franchise request {} failed: {}", request.getId(), e.getMessage());
        }
      }
    }
    return new JobOutcome(
        due.size(),
        expired + " expired, " + failed + " failed, of " + due.size() + " request(s) past due");
  }
}
