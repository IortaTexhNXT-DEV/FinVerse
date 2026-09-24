package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.BatchRun;
import com.iortatechnxt.brokerverse.booking.domain.BatchTrigger;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntry;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntryRepository;
import com.iortatechnxt.brokerverse.booking.domain.QueueStatus;
import com.iortatechnxt.brokerverse.booking.service.BatchBookingRunner.BatchItem;
import com.iortatechnxt.brokerverse.booking.service.BatchBookingRunner.DueYear;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * End-of-day booking batch (BRNB.036/076/112): books every queued account (queued by users,
 * placement or the auto-book rules) and the policy years of multi-year accounts that have started,
 * one batch run per company with a result per account.
 */
@Component
public class BookingBatchJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "BOOKING_BATCH";

  private final QueueEntryRepository queue;
  private final BookedInvoiceRepository invoices;
  private final BatchBookingRunner runner;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param queue booking queue
   * @param invoices scheduled policy years
   * @param runner batch runner
   * @param cron schedule ({@code brokerverse.jobs.booking-batch-cron})
   */
  public BookingBatchJob(
      QueueEntryRepository queue,
      BookedInvoiceRepository invoices,
      BatchBookingRunner runner,
      @Value("${brokerverse.jobs.booking-batch-cron:0 0 12 * * *}") String cron) {
    this.queue = queue;
    this.invoices = invoices;
    this.runner = runner;
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Books the accounts queued for booking and the multi-year policy years that have started";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<BatchRun> runs = new ArrayList<>();
    Map<Long, List<BatchItem>> queued = new LinkedHashMap<>();
    for (QueueEntry e : queue.findByStatusOrderByIdAsc(QueueStatus.QUEUED)) {
      queued
          .computeIfAbsent(e.getCompanyId(), k -> new ArrayList<>())
          .add(new BatchItem(e.getArn(), e.getId(), e.getBookingDate(), e.getCostCenter()));
    }
    queued.forEach(
        (company, items) ->
            runs.add(runner.run(company, BatchTrigger.SCHEDULED, businessDate, items)));
    Map<Long, List<DueYear>> due = new LinkedHashMap<>();
    for (BookedInvoice i :
        invoices.findByStatusAndInceptionDateLessThanEqualOrderByIdAsc(
            InvoiceStatus.SCHEDULED, businessDate)) {
      due.computeIfAbsent(i.getCompanyId(), k -> new ArrayList<>())
          .add(new DueYear(i.getId(), i.getArn()));
    }
    due.forEach((company, years) -> runs.add(runner.runDueYears(company, businessDate, years)));
    int booked = runs.stream().mapToInt(BatchRun::getBookedCount).sum();
    int failed = runs.stream().mapToInt(BatchRun::getFailedCount).sum();
    return new JobOutcome(
        booked + failed,
        booked + " invoice(s) booked, " + failed + " failed in " + runs.size() + " run(s)");
  }
}
