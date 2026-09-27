package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.BatchRow;
import com.iortatechnxt.brokerverse.booking.domain.BatchRun;
import com.iortatechnxt.brokerverse.booking.domain.BatchRunRepository;
import com.iortatechnxt.brokerverse.booking.domain.BatchTrigger;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntryRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs a booking batch (BRNB.036): every account is booked in its own transaction, so one failure
 * does not stop the others (partial success). The run and its per-account results are recorded with
 * a {@code BB-<yyyy>} number; queue entries are marked booked or failed with the reason.
 */
@Component
public class BatchBookingRunner {

  private final BookingService booking;
  private final QueueEntryRepository queue;
  private final BatchRunRepository runs;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the runner.
   *
   * @param booking booking service
   * @param queue booking queue
   * @param runs batch runs
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param txManager transaction manager
   * @param clock clock
   */
  public BatchBookingRunner(
      BookingService booking,
      QueueEntryRepository queue,
      BatchRunRepository runs,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.booking = booking;
    this.queue = queue;
    this.runs = runs;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  /**
   * Books a batch of accounts.
   *
   * @param companyId company
   * @param trigger what started the run
   * @param businessDate business date (default booking date)
   * @param items accounts to book
   * @return the recorded run
   */
  public BatchRun run(
      Long companyId, BatchTrigger trigger, LocalDate businessDate, List<BatchItem> items) {
    String runNo =
        Objects.requireNonNull(tx.execute(s -> numbers.next("BB-" + businessDate.getYear())));
    BookingSource source = sourceOf(trigger);
    List<BatchRow> rows = new ArrayList<>();
    for (BatchItem item : items) {
      rows.add(bookOne(item, runNo, source, businessDate));
    }
    return record(companyId, runNo, trigger, businessDate, rows);
  }

  /**
   * Books the scheduled policy years that have started (multi-year accounts, BRNB.112).
   *
   * @param companyId company
   * @param businessDate business date
   * @param invoices due scheduled invoices (id and ARN)
   * @return the recorded run
   */
  public BatchRun runDueYears(Long companyId, LocalDate businessDate, List<DueYear> invoices) {
    String runNo =
        Objects.requireNonNull(tx.execute(s -> numbers.next("BB-" + businessDate.getYear())));
    List<BatchRow> rows = new ArrayList<>();
    for (DueYear due : invoices) {
      try {
        String invoiceNo =
            tx.execute(
                s ->
                    booking
                        .bookDueYear(due.invoiceId(), businessDate, BookingSource.SCHEDULED)
                        .getInvoiceNo());
        rows.add(BatchRow.booked(due.arn(), invoiceNo));
      } catch (RuntimeException ex) {
        rows.add(BatchRow.failed(due.arn(), messageOf(ex)));
      }
    }
    return record(companyId, runNo, BatchTrigger.SCHEDULED, businessDate, rows);
  }

  private BatchRow bookOne(BatchItem item, String runNo, BookingSource source, LocalDate date) {
    try {
      String invoiceNo =
          tx.execute(
              s -> {
                BookedInvoice invoice =
                    booking.book(
                        item.arn(),
                        BookingOptions.of(
                            item.bookingDate() == null ? date : item.bookingDate(),
                            item.costCenter()),
                        source);
                if (item.entryId() != null) {
                  queue
                      .findById(item.entryId())
                      .ifPresent(e -> e.booked(runNo, invoice.getInvoiceNo()));
                }
                return invoice.getInvoiceNo();
              });
      return BatchRow.booked(item.arn(), invoiceNo);
    } catch (RuntimeException ex) {
      String message = messageOf(ex);
      if (item.entryId() != null) {
        tx.executeWithoutResult(
            s -> queue.findById(item.entryId()).ifPresent(e -> e.failed(runNo, message)));
      }
      return BatchRow.failed(item.arn(), message);
    }
  }

  private BatchRun record(
      Long companyId, String runNo, BatchTrigger trigger, LocalDate date, List<BatchRow> rows) {
    return tx.execute(
        s -> {
          BatchRun run =
              new BatchRun(
                  companyId, runNo, trigger, date, currentUser.username(), clock.instant());
          run.finish(rows, clock.instant());
          BatchRun saved = runs.save(run);
          audit.record(
              "BookingBatch",
              runNo,
              AuditAction.CREATE,
              trigger
                  + " batch: "
                  + saved.getBookedCount()
                  + " booked, "
                  + saved.getFailedCount()
                  + " failed");
          return saved;
        });
  }

  private static BookingSource sourceOf(BatchTrigger trigger) {
    return switch (trigger) {
      case SCHEDULED -> BookingSource.SCHEDULED;
      case UPLOAD -> BookingSource.UPLOAD;
      default -> BookingSource.BATCH;
    };
  }

  private static String messageOf(RuntimeException ex) {
    return ex instanceof BusinessRuleException b
        ? b.getCode() + ": " + b.getMessage()
        : ex.getClass().getSimpleName() + ": " + ex.getMessage();
  }

  /**
   * One account of a batch.
   *
   * @param arn account
   * @param entryId queue entry, null when not queued ("Book now")
   * @param bookingDate booking date chosen, null for the business date
   * @param costCenter cost center chosen, null for the account's
   */
  public record BatchItem(String arn, Long entryId, LocalDate bookingDate, String costCenter) {}

  /**
   * A scheduled policy year due for booking.
   *
   * @param invoiceId scheduled invoice
   * @param arn account
   */
  public record DueYear(Long invoiceId, String arn) {}
}
