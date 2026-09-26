package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Daily job {@code BCL_PREMIUM_RECHECK} (BRCLM.001; CLAIMS_BROKING_DESIGN 9.1): re-runs the premium
 * check of every open claim without an authorization code, as a safety net for ledger events the
 * listeners missed. Each claim is checked in its own transaction; a failing claim is logged and
 * skipped. Schedule {@code brokerverse.jobs.bcl-premium-recheck-cron} (05:30 Manila).
 */
@Component
public class PremiumRecheckJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "BCL_PREMIUM_RECHECK";

  private static final Logger LOG = LoggerFactory.getLogger(PremiumRecheckJob.class);
  private static final int BATCH = 200;

  private final ClaimOperationsSync sync;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param sync claim synchroniser
   * @param cron schedule
   */
  public PremiumRecheckJob(
      ClaimOperationsSync sync,
      @Value("${brokerverse.jobs.bcl-premium-recheck-cron:0 30 21 * * *}") String cron) {
    this.sync = sync;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Re-checks the premium of open claims without an authorization code (BRCLM.001)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int checked = 0;
    int failed = 0;
    int page = 0;
    List<Long> ids = sync.unauthorized(page, BATCH);
    while (!ids.isEmpty()) {
      for (Long id : ids) {
        try {
          checked += sync.recheck(id) ? 1 : 0;
        } catch (DataAccessException | IllegalStateException ex) {
          failed++;
          LOG.warn("Premium of claim {} not re-checked: {}", id, ex.getMessage());
        }
      }
      page++;
      ids = sync.unauthorized(page, BATCH);
    }
    return new JobOutcome(checked, checked + " claim(s) re-checked, " + failed + " failed");
  }
}
