package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.system.service.JobOutcome;
import com.iortatechnxt.finverse.system.service.ManagedJob;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Daily job flagging post-dated cheques issued whose cheque date has been reached as DUE (status
 * only, no posting). The same refresh can be run on request from the PDC register.
 */
@Component
public class IssuedPdcDueJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "PDC_ISSUED_DUE";

  private final IssuedPdcService pdcs;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param pdcs issued PDC register
   * @param cron schedule ({@code finverse.jobs.pdc-issued-due-cron})
   */
  public IssuedPdcDueJob(
      IssuedPdcService pdcs,
      @Value("${finverse.jobs.pdc-issued-due-cron:0 15 0 * * *}") String cron) {
    this.pdcs = pdcs;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Flags post-dated cheques issued whose cheque date has been reached as due";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int due = pdcs.refreshDue(businessDate);
    return new JobOutcome(due, due + " cheque(s) issued marked due as of " + businessDate);
  }
}
