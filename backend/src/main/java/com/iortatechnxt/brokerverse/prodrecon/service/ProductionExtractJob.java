package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ExtractTrigger;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.Frequency;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSchedule;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconScheduleRepository;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService.ExtractRequest;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Scheduled production register extraction (PRCID.001): every active schedule due on the business
 * date extracts the production of the previous month (monthly) or the previous seven days (weekly),
 * sends it right away when the schedule says so, and moves to its next run date rolled to a working
 * day. Each schedule runs in its own transactions, so one insurer failing does not stop the others.
 */
@Component
public class ProductionExtractJob implements ManagedJob {

  /** Job name. */
  public static final String NAME = "PRODUCTION_EXTRACT";

  private static final Logger LOG = LoggerFactory.getLogger(ProductionExtractJob.class);
  private static final int WEEK = 7;

  private final ReconScheduleRepository schedules;
  private final ReconScheduleService scheduleService;
  private final ProductionExtractService extracts;
  private final ReconSendService sender;
  private final TransactionTemplate tx;
  private final Clock clock;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param schedules schedules
   * @param scheduleService holiday roll
   * @param extracts extraction
   * @param sender sending
   * @param txManager transaction manager
   * @param clock clock
   * @param cron schedule ({@code brokerverse.jobs.production-extract-cron})
   */
  public ProductionExtractJob(
      ReconScheduleRepository schedules,
      ReconScheduleService scheduleService,
      ProductionExtractService extracts,
      ReconSendService sender,
      PlatformTransactionManager txManager,
      Clock clock,
      @Value("${brokerverse.jobs.production-extract-cron:-}") String cron) {
    this.schedules = schedules;
    this.scheduleService = scheduleService;
    this.extracts = extracts;
    this.sender = sender;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
    this.cron = cron;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Extracts the production register of every insurer whose schedule is due (PRCID.001)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int extracted = 0;
    int failed = 0;
    for (ReconSchedule schedule :
        schedules.findByActiveTrueAndNextRunDateLessThanEqualOrderByIdAsc(businessDate)) {
      String extractNo = null;
      try {
        extractNo = tx.execute(s -> extractAndSend(schedule, businessDate));
        extracted++;
      } catch (BusinessRuleException | ResourceNotFoundException ex) {
        failed++;
        LOG.warn(
            "Production extract of {} skipped: {}", schedule.getInsurerCode(), ex.getMessage());
      }
      String done = extractNo;
      tx.executeWithoutResult(s -> advance(schedule.getId(), done, businessDate));
    }
    return new JobOutcome(
        extracted + failed, extracted + " register(s) extracted, " + failed + " skipped");
  }

  private String extractAndSend(ReconSchedule schedule, LocalDate businessDate) {
    boolean weekly = schedule.getFrequency() == Frequency.WEEKLY;
    LocalDate from =
        weekly ? businessDate.minusDays(WEEK) : businessDate.minusMonths(1).withDayOfMonth(1);
    LocalDate to = weekly ? businessDate.minusDays(1) : from.withDayOfMonth(from.lengthOfMonth());
    ReconExtract extract =
        extracts.extract(
            new ExtractRequest(schedule.getCompanyId(), schedule.getInsurerCode(), from, to),
            ExtractTrigger.SCHEDULED);
    if (schedule.isAutoSend() && schedule.getRecipients() != null) {
      List<String> recipients =
          Arrays.stream(schedule.getRecipients().split("[,;]"))
              .map(String::strip)
              .filter(a -> !a.isEmpty())
              .toList();
      sender.send(extract.getId(), recipients, List.of(), true);
    }
    return extract.getExtractNo();
  }

  private void advance(Long scheduleId, String extractNo, LocalDate businessDate) {
    schedules
        .findById(scheduleId)
        .ifPresent(
            s -> {
              s.ran(extractNo, clock.instant(), businessDate);
              scheduleService.roll(s);
            });
  }
}
