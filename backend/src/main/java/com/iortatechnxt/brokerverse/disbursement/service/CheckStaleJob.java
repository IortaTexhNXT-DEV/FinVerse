package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EventSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService.Change;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code DISB_CHECK_STALE} (DIS 3.26.2, 3.27.1; cron {@code
 * brokerverse.jobs.disb-check-stale-cron}, daily 00:20 PHT): a check still printed or released
 * {@code DISB_STALE_DAYS} (180) days after its print date becomes stale, its amount moves from
 * checks outstanding to Miscellaneous Liability - stale checks of the payee, and the alert {@code
 * DISB_CHECK_STALE} is raised. Each check is processed in its own transaction.
 */
@Component
public class CheckStaleJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "DISB_CHECK_STALE";

  private static final Logger LOG = LoggerFactory.getLogger(CheckStaleJob.class);

  private final InstrumentRepository instruments;
  private final InstrumentService service;
  private final SystemParameterService parameters;
  private final AlertService alerts;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param instruments instruments
   * @param service instrument life cycle
   * @param parameters business parameters
   * @param alerts alerts
   * @param txManager transactions
   * @param cron schedule
   */
  public CheckStaleJob(
      InstrumentRepository instruments,
      InstrumentService service,
      SystemParameterService parameters,
      AlertService alerts,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.disb-check-stale-cron:-}") String cron) {
    this.instruments = instruments;
    this.service = service;
    this.parameters = parameters;
    this.alerts = alerts;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Tags as stale the checks not negotiated DISB_STALE_DAYS after their print date";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int days =
        parameters.intValue(
            DisbursementSettings.STALE_DAYS, DisbursementSettings.DEFAULT_STALE_DAYS);
    List<Instrument> due =
        instruments.findByModeAndStatusInAndPrintedOnLessThanEqualOrderByIdAsc(
            DisbursementMode.CHECK,
            EnumSet.of(InstrumentStatus.PRINTED, InstrumentStatus.RELEASED),
            businessDate.minusDays(days));
    int staled = 0;
    for (Instrument check : due) {
      try {
        tx.executeWithoutResult(s -> stale(check.getId(), days));
        staled++;
      } catch (BusinessRuleException ex) {
        LOG.warn("Check {} not staled: {}", check.label(), ex.getMessage());
      }
    }
    return new JobOutcome(staled, staled + " check(s) staled after " + days + " days");
  }

  private void stale(Long instrumentId, int days) {
    Instrument i = service.get(instrumentId);
    service.stale(i, new Change(EventSource.JOB, days + " days after the print date", null));
    Voucher v = service.voucherOf(i);
    alerts.raise(
        JOB_NAME,
        new AlertFacts(
            v.getCompanyId(),
            v.getBranchId(),
            DisbursementSettings.VOUCHER,
            v.getId().toString(),
            "Check "
                + i.label()
                + " of DV "
                + v.getDvNo()
                + " to "
                + v.getPayeeName()
                + " is stale",
            i.getAmount(),
            JOB_NAME + ":" + i.getId()));
  }
}
