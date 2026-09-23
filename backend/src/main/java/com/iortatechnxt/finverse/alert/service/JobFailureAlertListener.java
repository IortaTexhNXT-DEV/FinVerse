package com.iortatechnxt.finverse.alert.service;

import com.iortatechnxt.finverse.alert.domain.AlertFacts;
import com.iortatechnxt.finverse.system.domain.JobRun;
import com.iortatechnxt.finverse.system.service.JobFailureListener;
import org.springframework.stereotype.Component;

/** Raises JOB_FAILURE when a background job run fails (one live alert per job). */
@Component
public class JobFailureAlertListener implements JobFailureListener {

  /** Exception code. */
  public static final String CODE = "JOB_FAILURE";

  private final AlertService alerts;

  /**
   * Creates the listener.
   *
   * @param alerts alert service
   */
  public JobFailureAlertListener(AlertService alerts) {
    this.alerts = alerts;
  }

  @Override
  public void onFailure(JobRun run) {
    alerts.raise(
        CODE,
        new AlertFacts(
            null,
            null,
            "Job",
            run.getJobName(),
            "Job " + run.getJobName() + " failed (run " + run.getId() + "): " + run.getMessage(),
            null,
            CODE + ":" + run.getJobName()));
  }
}
