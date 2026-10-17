package com.iortatechnxt.brokerverse.csf.cases.service;

import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code CSF_CASE_SLA} (FR-CSF-057; cron {@code brokerverse.jobs.csf-case-sla-cron}, every 15
 * minutes by default): updates the SLA status of the open cases, sends the approaching-breach and
 * breach notices once per case and threshold, and escalates breached cases of the case types set
 * for automatic escalation. One transaction per case.
 */
@Component
public class CsfCaseSlaJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "CSF_CASE_SLA";

  private final CsfCaseStore cases;
  private final CsfCaseService service;
  private final CsfCaseViews views;
  private final CsfCaseCalendar calendar;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param cases open cases
   * @param service SLA actions
   * @param views SLA status
   * @param calendar working days
   * @param transactions transaction manager
   * @param cron schedule
   */
  public CsfCaseSlaJob(
      CsfCaseStore cases,
      CsfCaseService service,
      CsfCaseViews views,
      CsfCaseCalendar calendar,
      PlatformTransactionManager transactions,
      @Value("${brokerverse.jobs.csf-case-sla-cron:0 */15 * * * *}") String cron) {
    this.cases = cases;
    this.service = service;
    this.views = views;
    this.calendar = calendar;
    this.tx = new TransactionTemplate(transactions);
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Updates the SLA status of the open contact centre cases, sends the breach notices and"
        + " escalates breached cases";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public Workload workload() {
    return Workload.BATCH;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<CsfCase> open = tx.execute(s -> cases.allOpen());
    int changed = 0;
    for (CsfCase c : open == null ? List.<CsfCase>of() : open) {
      String status = views.sla(c, calendar.of(c.companyId())).status();
      if (Boolean.TRUE.equals(tx.execute(s -> service.applySla(c, status)))) {
        changed++;
      }
    }
    return new JobOutcome(changed, changed + " cases changed their SLA status or were notified");
  }
}
