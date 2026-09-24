package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.BatchRun;
import com.iortatechnxt.brokerverse.booking.domain.BatchTrigger;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntry;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntryRepository;
import com.iortatechnxt.brokerverse.booking.domain.QueueSource;
import com.iortatechnxt.brokerverse.booking.domain.QueueStatus;
import com.iortatechnxt.brokerverse.booking.service.BatchBookingRunner.BatchItem;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The booking queue and batches (BRNB.036/076).
 *
 * <p><b>Contract for placement</b> ("For Booking" bulk action) and other modules: {@link
 * #enqueue(Long, Collection, QueueSource)} queues accounts in POLICY_ISSUED for the next batch and
 * reports, per ARN, whether it was queued. The queue is booked when a user confirms the batch, by
 * "Book now" on a selection, or by the end-of-day BOOKING_BATCH job. Before that, an entry's
 * booking date and cost center can be changed and entries removed; cancelling the batch removes
 * them all.
 */
@Service
@Transactional
public class BookingQueueService {

  private static final String ENTITY = "BookingQueue";

  private final QueueEntryRepository queue;
  private final AccountQueryService accounts;
  private final BatchBookingRunner runner;
  private final DimensionService dimensions;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param queue queue entries
   * @param accounts account reads
   * @param runner batch runner
   * @param dimensions cost center validation
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public BookingQueueService(
      QueueEntryRepository queue,
      AccountQueryService accounts,
      BatchBookingRunner runner,
      DimensionService dimensions,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.queue = queue;
    this.accounts = accounts;
    this.runner = runner;
    this.dimensions = dimensions;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Queues accounts for the next booking batch (contract). An account must belong to the company
   * and be in POLICY_ISSUED; one already queued is left as it is.
   *
   * @param companyId company
   * @param arns Account Reference Numbers
   * @param source who queues them
   * @return one result per distinct ARN
   */
  public List<EnqueueResult> enqueue(Long companyId, Collection<String> arns, QueueSource source) {
    Set<String> distinct = new LinkedHashSet<>();
    arns.stream().filter(Objects::nonNull).map(String::strip).forEach(distinct::add);
    List<EnqueueResult> results = new ArrayList<>();
    for (String arn : distinct) {
      results.add(enqueueOne(companyId, arn, source));
    }
    return results;
  }

  /**
   * Queues an account that is entering POLICY_ISSUED in the current transaction (auto-book,
   * BRNB.076); its status may not be mirrored yet.
   *
   * @param account account
   * @param source who queues it
   * @return result
   */
  public EnqueueResult enqueueIssued(Account account, QueueSource source) {
    return save(account, source);
  }

  private EnqueueResult enqueueOne(Long companyId, String arn, QueueSource source) {
    Account account =
        accounts.preBooked(companyId, arn).stream()
            .filter(a -> a.getArn().equals(arn))
            .findFirst()
            .orElse(null);
    if (account == null) {
      return new EnqueueResult(arn, false, "Unknown account, or already booked");
    }
    if (account.getStatus() != AccountStatus.POLICY_ISSUED) {
      return new EnqueueResult(
          arn, false, "Account is " + account.getStatus() + ", not ready for booking");
    }
    return save(account, source);
  }

  private EnqueueResult save(Account account, QueueSource source) {
    String arn = account.getArn();
    if (queue.findByArnAndStatus(arn, QueueStatus.QUEUED).isPresent()) {
      return new EnqueueResult(arn, false, "Already queued");
    }
    queue.findAllByArnAndStatus(arn, QueueStatus.FAILED).forEach(QueueEntry::clearFailure);
    queue.save(
        new QueueEntry(
            account.getCompanyId(),
            arn,
            account.getId(),
            source,
            currentUser.username(),
            clock.instant()));
    audit.record(ENTITY, arn, AuditAction.CREATE, "Queued for booking (" + source + ")");
    return new EnqueueResult(arn, true, null);
  }

  /**
   * Changes the booking date and cost center of a queued account (BRNB.036 edit before booking).
   *
   * @param id queue entry
   * @param bookingDate booking date, null for the batch date
   * @param costCenter cost center, null for the account's
   * @return entry
   */
  public QueueEntry edit(Long id, LocalDate bookingDate, String costCenter) {
    QueueEntry entry = require(id);
    if (bookingDate != null && bookingDate.isAfter(LocalDate.now(clock))) {
      throw new BusinessRuleException(
          "BOOKING_DATE_FUTURE", "The booking date " + bookingDate + " is in the future");
    }
    dimensions.validateOptional(entry.getCompanyId(), DimensionType.COST_CENTER, costCenter);
    entry.edit(bookingDate, costCenter);
    audit.record(
        ENTITY,
        entry.getArn(),
        AuditAction.UPDATE,
        "Booking date " + bookingDate + ", cost center " + costCenter);
    return entry;
  }

  /**
   * Removes a queued account (BRNB.036 delete before booking).
   *
   * @param id queue entry
   * @return entry
   */
  public QueueEntry remove(Long id) {
    QueueEntry entry = require(id);
    entry.remove();
    audit.record(ENTITY, entry.getArn(), AuditAction.UPDATE, "Removed from the booking queue");
    return entry;
  }

  /**
   * Cancels the pending batch: every queued account of the company is removed.
   *
   * @param companyId company
   * @return accounts removed
   */
  public int cancelBatch(Long companyId) {
    List<QueueEntry> entries =
        queue.findByCompanyIdAndStatusInOrderByIdAsc(companyId, List.of(QueueStatus.QUEUED));
    entries.forEach(QueueEntry::remove);
    audit.record(
        ENTITY, companyId, AuditAction.UPDATE, "Batch cancelled: " + entries.size() + " removed");
    return entries.size();
  }

  /**
   * Confirms the batch: books the queued accounts (all, or those chosen).
   *
   * @param companyId company
   * @param entryIds entries to book, empty for all queued
   * @param businessDate business date, null for today
   * @return the batch run with a result per account
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public BatchRun confirmBatch(Long companyId, Collection<Long> entryIds, LocalDate businessDate) {
    List<BatchItem> items =
        queue
            .findByCompanyIdAndStatusInOrderByIdAsc(companyId, List.of(QueueStatus.QUEUED))
            .stream()
            .filter(e -> entryIds.isEmpty() || entryIds.contains(e.getId()))
            .map(e -> new BatchItem(e.getArn(), e.getId(), e.getBookingDate(), e.getCostCenter()))
            .toList();
    if (items.isEmpty()) {
      throw new BusinessRuleException("BATCH_EMPTY", "No queued account to book");
    }
    return runner.run(companyId, BatchTrigger.MANUAL, dateOf(businessDate), items);
  }

  /**
   * Books a selection now (workbench "Book now"), each account in its own transaction.
   *
   * @param companyId company
   * @param arns accounts
   * @param bookingDate booking date, null for today
   * @return the batch run
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public BatchRun bookNow(Long companyId, Collection<String> arns, LocalDate bookingDate) {
    if (arns.isEmpty()) {
      throw new BusinessRuleException("BATCH_EMPTY", "Select at least one account to book");
    }
    List<BatchItem> items =
        arns.stream()
            .distinct()
            .map(
                arn ->
                    new BatchItem(
                        arn,
                        queue
                            .findByArnAndStatus(arn, QueueStatus.QUEUED)
                            .map(QueueEntry::getId)
                            .orElse(null),
                        bookingDate,
                        null))
            .toList();
    return runner.run(companyId, BatchTrigger.BOOK_NOW, dateOf(bookingDate), items);
  }

  private LocalDate dateOf(LocalDate date) {
    return date == null ? LocalDate.now(clock) : date;
  }

  private QueueEntry require(Long id) {
    return queue.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Outcome of queuing one account.
   *
   * @param arn account
   * @param queued true when queued now
   * @param message why it was not queued
   */
  public record EnqueueResult(String arn, boolean queued, String message) {}
}
