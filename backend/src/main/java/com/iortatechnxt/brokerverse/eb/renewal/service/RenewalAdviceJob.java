package com.iortatechnxt.brokerverse.eb.renewal.service;

import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdvice;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdviceRepository;
import com.iortatechnxt.brokerverse.eb.renewal.service.RenewalAdviceService.JobStep;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code EB_RENEWAL_ADVICE} (BRID-001, 002; design 8.1; cron {@code
 * brokerverse.jobs.eb-renewal-advice-cron}, 06:00 Manila): sends the renewal advice of every
 * programme flagged for renewal whose line expires within {@code EB_RA_LEAD_DAYS} (raising {@code
 * EB_RA_NOT_SENT} for those that cannot receive it), then the reminders at {@code
 * EB_RA_REMINDER_DAYS} before expiry while no feedback is recorded. Each programme and each
 * reminder runs in its own transaction; one failure does not stop the others.
 */
@Component
public class RenewalAdviceJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "EB_RENEWAL_ADVICE";

  private static final Logger LOG = LoggerFactory.getLogger(RenewalAdviceJob.class);

  private final EbProgrammeRepository programmes;
  private final EbRenewalAdviceRepository advices;
  private final RenewalAdviceService service;
  private final EbParameters parameters;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param programmes programmes
   * @param advices renewal advices
   * @param service sending
   * @param parameters lead time and reminder days
   * @param cron schedule ({@code brokerverse.jobs.eb-renewal-advice-cron})
   */
  public RenewalAdviceJob(
      EbProgrammeRepository programmes,
      EbRenewalAdviceRepository advices,
      RenewalAdviceService service,
      EbParameters parameters,
      @Value("${brokerverse.jobs.eb-renewal-advice-cron:-}") String cron) {
    this.programmes = programmes;
    this.advices = advices;
    this.service = service;
    this.parameters = parameters;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Sends the renewal advice of Employee Benefits programmes before expiry and the reminders";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    LocalDate until = businessDate.plusDays(parameters.raLeadDays());
    Map<JobStep, Integer> steps = new EnumMap<>(JobStep.class);
    int failed = 0;
    List<EbProgramme> due = programmes.findWithLinesExpiring(businessDate, until);
    for (EbProgramme programme : due) {
      try {
        steps.merge(service.sendDue(programme.getId(), businessDate, until), 1, Integer::sum);
      } catch (RuntimeException e) {
        failed++;
        LOG.warn("Renewal advice of {} not sent: {}", programme.getProgrammeNo(), e.getMessage());
      }
    }
    List<Integer> reminderDays = parameters.raReminderDays();
    int reminded = 0;
    List<EbRenewalAdvice> waiting = advices.findAwaitingFeedback(null);
    for (EbRenewalAdvice advice : waiting) {
      try {
        reminded += service.remindIfDue(advice.getId(), businessDate, reminderDays) ? 1 : 0;
      } catch (RuntimeException e) {
        failed++;
        LOG.warn("Reminder of renewal advice {} not sent: {}", advice.getId(), e.getMessage());
      }
    }
    int sent = steps.getOrDefault(JobStep.SENT, 0);
    int alerted = steps.getOrDefault(JobStep.ALERTED, 0);
    return new JobOutcome(
        due.size() + waiting.size(),
        sent
            + " renewal advice(s) sent, "
            + reminded
            + " reminder(s), "
            + alerted
            + " programme(s) flagged, "
            + failed
            + " failed");
  }
}
