package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceDraft;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntry;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntryRepository;
import com.iortatechnxt.brokerverse.booking.domain.QueueStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Books accounts (BRNB.027/036/038/061/111/112): an account in POLICY_ISSUED - placed and issued,
 * or booked directly when the insurer had already issued the policy - is booked in <b>one
 * transaction</b>: invoice(s), GL entry, open items, service invoice, the account's {@code book}
 * transition ({@code AccountLifecycleService.recordBooking}, with the incentive flag and cost
 * center) and the {@link InvoiceBooked} event. A failure rolls everything back.
 *
 * <p>The booking key is the ARN plus the transaction number ({@code NB} for the original booking):
 * a second booking of the same transaction is refused (BRNB.076). A multi-year account books its
 * first policy year and schedules the others ({@link #bookDueYear}).
 */
@Service
@Transactional
public class BookingService {

  private final AccountQueryService accounts;
  private final AccountLifecycleService lifecycle;
  private final BookedInvoiceRepository invoices;
  private final QueueEntryRepository queue;
  private final InvoiceBuilder builder;
  private final InvoiceBooker booker;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts account reads
   * @param lifecycle account lifecycle
   * @param invoices invoices
   * @param queue booking queue
   * @param builder invoice builder
   * @param booker invoice booker
   * @param clock clock
   */
  public BookingService(
      AccountQueryService accounts,
      AccountLifecycleService lifecycle,
      BookedInvoiceRepository invoices,
      QueueEntryRepository queue,
      InvoiceBuilder builder,
      InvoiceBooker booker,
      Clock clock) {
    this.accounts = accounts;
    this.lifecycle = lifecycle;
    this.invoices = invoices;
    this.queue = queue;
    this.builder = builder;
    this.booker = booker;
    this.clock = clock;
  }

  /**
   * Books an account.
   *
   * @param arn Account Reference Number
   * @param options booking date, cost center, CWT 2 % and insurer shares
   * @param source how it is booked
   * @return the booked invoice of the first policy year
   */
  public BookedInvoice book(String arn, BookingOptions options, BookingSource source) {
    Account account = requireBookable(arn);
    LocalDate date = options.bookingDate() == null ? LocalDate.now(clock) : options.bookingDate();
    List<InvoiceDraft> drafts = builder.drafts(account, options, date);
    for (InvoiceDraft later : drafts.subList(1, drafts.size())) {
      invoices.save(BookedInvoice.draft(later));
    }
    BookedInvoice first = booker.book(BookedInvoice.draft(drafts.get(0)), date, source);
    lifecycle.recordBooking(
        arn,
        first.getInvoiceNo(),
        date,
        first.getFlags().incentiveEligible(),
        first.getFacts().costCenter());
    queue.findAllByArnAndStatus(arn, QueueStatus.FAILED).forEach(QueueEntry::clearFailure);
    queue
        .findByArnAndStatus(arn, QueueStatus.QUEUED)
        .ifPresent(e -> e.booked(null, first.getInvoiceNo()));
    return first;
  }

  /**
   * Books a scheduled policy year of a multi-year account (BRNB.112), dated the business date.
   *
   * @param invoiceId scheduled invoice
   * @param businessDate business date (on or after the year's inception)
   * @param source how it is booked
   * @return the booked invoice
   */
  public BookedInvoice bookDueYear(Long invoiceId, LocalDate businessDate, BookingSource source) {
    BookedInvoice invoice =
        invoices
            .findById(invoiceId)
            .orElseThrow(() -> new BusinessRuleException("INVOICE_NOT_FOUND", "No invoice"));
    if (invoice.getStatus() != InvoiceStatus.SCHEDULED
        || invoice.getInceptionDate().isAfter(businessDate)) {
      throw new BusinessRuleException(
          "POLICY_YEAR_NOT_DUE",
          "Policy year "
              + invoice.getPolicyYear()
              + " of "
              + invoice.getArn()
              + " is not due for booking");
    }
    LocalDate today = LocalDate.now(clock);
    return booker.book(invoice, businessDate.isAfter(today) ? today : businessDate, source);
  }

  /**
   * The account to book: live and in POLICY_ISSUED, not already booked (BRNB.076).
   *
   * @param arn account
   * @return account, loaded
   */
  public Account requireBookable(String arn) {
    Account account = accounts.requireByArn(arn);
    if (invoices.existsByArnAndTransactionNo(arn, InvoiceBuilder.ORIGINAL)
        || account.getStatus() == AccountStatus.BOOKED) {
      throw new BusinessRuleException(
          "DUPLICATE_BOOKING", "Account " + arn + " is already booked (BRNB.076)");
    }
    if (account.getStatus() != AccountStatus.POLICY_ISSUED) {
      throw new BusinessRuleException(
          "ACCOUNT_NOT_BOOKABLE",
          "Account " + arn + " is " + account.getStatus() + ": only issued policies are booked");
    }
    return account;
  }
}
