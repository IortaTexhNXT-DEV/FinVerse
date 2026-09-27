package com.iortatechnxt.brokerverse.eb.tracked.service;

import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItem;
import com.iortatechnxt.brokerverse.eb.domain.EbTrackedItemRepository;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code EB_ITEM_FOLLOWUP} (BRID-030; design 8.1; cron {@code
 * brokerverse.jobs.eb-item-followup-cron}, 07:00 Manila): for every pending tracked item past its
 * due date, sends the follow-up due every {@code EB_FOLLOWUP_DAYS} working days and escalates after
 * {@code EB_FOLLOWUP_MAX} follow-ups. Each item runs in its own transaction; one failure does not
 * stop the others.
 */
@Component
public class ItemFollowUpJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "EB_ITEM_FOLLOWUP";

  private static final Logger LOG = LoggerFactory.getLogger(ItemFollowUpJob.class);

  private final EbTrackedItemRepository items;
  private final ItemFollowUpService service;
  private final EbParameters parameters;
  private final EbWorkingDays workingDays;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param items tracked items
   * @param service one follow-up step
   * @param parameters follow-up interval and maximum
   * @param workingDays working-day calendars
   * @param cron schedule ({@code brokerverse.jobs.eb-item-followup-cron})
   */
  public ItemFollowUpJob(
      EbTrackedItemRepository items,
      ItemFollowUpService service,
      EbParameters parameters,
      EbWorkingDays workingDays,
      @Value("${brokerverse.jobs.eb-item-followup-cron:-}") String cron) {
    this.items = items;
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
    return "Follows up Employee Benefits pending items past due and escalates them to the AO";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    FollowUpSchedule schedule =
        new FollowUpSchedule(parameters.followUpDays(), parameters.followUpMax());
    Map<Long, Predicate<LocalDate>> calendars = new HashMap<>();
    Map<FollowUpSchedule.Step, Integer> steps = new EnumMap<>(FollowUpSchedule.Step.class);
    int failed = 0;
    List<EbTrackedItem> due = items.findPendingPastDue(businessDate);
    for (EbTrackedItem item : due) {
      Predicate<LocalDate> working =
          calendars.computeIfAbsent(item.getCompanyId(), workingDays::calendar);
      try {
        steps.merge(
            service.process(item.getId(), businessDate, schedule, working), 1, Integer::sum);
      } catch (RuntimeException e) {
        failed++;
        LOG.warn("Follow-up of tracked item {} failed: {}", item.getId(), e.getMessage());
      }
    }
    return new JobOutcome(
        due.size(),
        steps.getOrDefault(FollowUpSchedule.Step.FOLLOW_UP, 0)
            + " follow-up(s) sent, "
            + steps.getOrDefault(FollowUpSchedule.Step.ESCALATE, 0)
            + " escalated, "
            + failed
            + " failed, of "
            + due.size()
            + " item(s) past due");
  }
}
