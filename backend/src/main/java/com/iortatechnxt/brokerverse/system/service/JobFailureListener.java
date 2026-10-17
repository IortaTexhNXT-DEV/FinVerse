package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.system.domain.JobRun;

/**
 * Port notified when a background job fails (implemented by the alert engine to raise a JOB_FAILURE
 * exception). Called in its own transaction after the failed run is recorded.
 */
public interface JobFailureListener {

  /**
   * Handles a failed run.
   *
   * @param run the failed run
   */
  void onFailure(JobRun run);
}
