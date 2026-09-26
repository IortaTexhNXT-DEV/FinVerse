package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.SalesStamp;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountSearch;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntry;
import com.iortatechnxt.brokerverse.booking.domain.QueueEntryRepository;
import com.iortatechnxt.brokerverse.booking.domain.QueueStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Booking Workbench (BRNB.036): tiles and tabs Ready to Book (issued accounts not queued),
 * Queued for Batch, Booked Account and Failed, searchable by proposal number (ARN) or client.
 */
@Service
@Transactional(readOnly = true)
public class BookingWorkbenchService {

  private final AccountQueryService accounts;
  private final QueueEntryRepository queue;
  private final BookedInvoiceRepository invoices;
  private final BookingQueryService queries;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param accounts account reads
   * @param queue booking queue
   * @param invoices booked invoices
   * @param queries invoice search
   * @param clock clock
   */
  public BookingWorkbenchService(
      AccountQueryService accounts,
      QueueEntryRepository queue,
      BookedInvoiceRepository invoices,
      BookingQueryService queries,
      Clock clock) {
    this.accounts = accounts;
    this.queue = queue;
    this.invoices = invoices;
    this.queries = queries;
    this.clock = clock;
  }

  /**
   * Tile counts.
   *
   * @param companyId company
   * @return counts
   */
  public Counts counts(Long companyId) {
    return new Counts(
        readyAccounts(companyId, Filter.NONE).size(),
        queue.countByCompanyIdAndStatus(companyId, QueueStatus.QUEUED),
        invoices.countByCompanyIdAndStatusAndBookingDate(
            companyId, InvoiceStatus.BOOKED, LocalDate.now(clock)),
        queue.countByCompanyIdAndStatus(companyId, QueueStatus.FAILED));
  }

  /**
   * Rows of a tab.
   *
   * @param companyId company
   * @param tab tab
   * @param filter ARN, client code or name fragment, and product line
   * @param pageable page
   * @return rows
   */
  public Page<Row> rows(Long companyId, Tab tab, Filter filter, Pageable pageable) {
    return switch (tab) {
      case READY -> ready(companyId, filter, pageable);
      case QUEUED -> entries(companyId, QueueStatus.QUEUED, filter, pageable);
      case FAILED -> entries(companyId, QueueStatus.FAILED, filter, pageable);
      case BOOKED -> booked(companyId, filter, pageable);
    };
  }

  private Page<Row> ready(Long companyId, Filter filter, Pageable pageable) {
    List<Row> rows = readyAccounts(companyId, filter).stream().map(Row::ofAccount).toList();
    return page(rows, pageable);
  }

  private List<Account> readyAccounts(Long companyId, Filter filter) {
    Set<String> queued =
        queue
            .findByCompanyIdAndStatusInOrderByIdAsc(companyId, List.of(QueueStatus.QUEUED))
            .stream()
            .map(QueueEntry::getArn)
            .collect(Collectors.toSet());
    AccountSearch search =
        new AccountSearch(
            companyId,
            blankToNull(filter.text()),
            null,
            null,
            null,
            null,
            blankToNull(filter.lineCode()),
            null,
            List.of(AccountStatus.POLICY_ISSUED),
            null,
            null,
            null,
            null,
            null,
            false);
    return accounts
        .search(search, Pageable.unpaged(Sort.by(Sort.Direction.ASC, "createdAt", "id")))
        .stream()
        .filter(a -> !queued.contains(a.getArn()))
        .toList();
  }

  private Page<Row> entries(Long companyId, QueueStatus status, Filter filter, Pageable pageable) {
    List<Row> rows =
        queue.findByCompanyIdAndStatusInOrderByIdAsc(companyId, List.of(status)).stream()
            .map(this::rowOf)
            .filter(filter::accepts)
            .toList();
    return page(rows, pageable);
  }

  private Row rowOf(QueueEntry entry) {
    Account account = accounts.requireByArn(entry.getArn());
    return new Row(
        entry.getId(),
        account.getId(),
        null,
        account.getClientName(),
        account.getClientCode(),
        account.getArn(),
        null,
        entry.getStatus().name(),
        account.getLineCode(),
        account.getProductCode(),
        sales(account, SalesStamp::department),
        entry.getBookingDate(),
        entry.getLastError(),
        entry.getSource().name(),
        entry.getCostCenter());
  }

  private Page<Row> booked(Long companyId, Filter filter, Pageable pageable) {
    return queries
        .search(
            new InvoiceSearch(
                companyId,
                filter.text(),
                InvoiceStatus.BOOKED,
                null,
                null,
                null,
                null,
                filter.lineCode()),
            PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "bookingDate", "id")))
        .map(Row::ofInvoice);
  }

  private static Page<Row> page(List<Row> rows, Pageable pageable) {
    int from = (int) Math.min(pageable.getOffset(), rows.size());
    int to = Math.min(from + pageable.getPageSize(), rows.size());
    return new PageImpl<>(rows.subList(from, to), pageable, rows.size());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  static String sales(Account account, Function<SalesStamp, String> value) {
    return Optional.ofNullable(account.getSales()).map(value).orElse(null);
  }

  /**
   * Workbench filter.
   *
   * @param text ARN, client code or client name fragment
   * @param lineCode product line
   */
  public record Filter(String text, String lineCode) {

    /** No filter. */
    public static final Filter NONE = new Filter(null, null);

    boolean accepts(Row row) {
      boolean lineOk = lineCode == null || lineCode.isBlank() || lineCode.equals(row.lineCode());
      return lineOk && textMatches(row);
    }

    private boolean textMatches(Row row) {
      return text == null || text.isBlank() || row.matches(text.strip().toLowerCase(Locale.ROOT));
    }
  }

  /** Workbench tab. */
  public enum Tab {
    /** Issued accounts not queued. */
    READY,
    /** Queued for the next batch. */
    QUEUED,
    /** Booked invoices. */
    BOOKED,
    /** Queue entries a batch could not book. */
    FAILED
  }

  /**
   * Tile counts.
   *
   * @param readyToBook issued accounts not queued
   * @param queued queued for batch
   * @param bookedToday invoices booked today
   * @param failed failed queue entries
   */
  public record Counts(long readyToBook, long queued, long bookedToday, long failed) {}

  /**
   * One workbench row.
   *
   * @param id queue entry (queued / failed tabs), account (ready) or invoice (booked)
   * @param accountId account
   * @param invoiceId invoice (booked tab)
   * @param clientName client name
   * @param clientCode client code
   * @param arn proposal number (ARN)
   * @param invoiceNo invoice number (booked tab)
   * @param status POLICY_ISSUED, QUEUED, BOOKED or FAILED
   * @param lineCode product line
   * @param productCode product
   * @param department sales department
   * @param bookingDate booking date (booked) or the date chosen for the batch
   * @param message failure reason
   * @param source queue source or booking source
   * @param costCenter cost center chosen or booked
   */
  public record Row(
      Long id,
      Long accountId,
      Long invoiceId,
      String clientName,
      String clientCode,
      String arn,
      String invoiceNo,
      String status,
      String lineCode,
      String productCode,
      String department,
      LocalDate bookingDate,
      String message,
      String source,
      String costCenter) {

    static Row ofAccount(Account a) {
      return new Row(
          a.getId(),
          a.getId(),
          null,
          a.getClientName(),
          a.getClientCode(),
          a.getArn(),
          null,
          a.getStatus().name(),
          a.getLineCode(),
          a.getProductCode(),
          sales(a, SalesStamp::department),
          null,
          null,
          a.isDirectBooking() ? "DIRECT_BOOKING" : null,
          sales(a, SalesStamp::costCenter));
    }

    static Row ofInvoice(BookedInvoice i) {
      return new Row(
          i.getId(),
          i.getAccountId(),
          i.getId(),
          i.getFacts().clientName(),
          i.getFacts().clientCode(),
          i.getArn(),
          i.getInvoiceNo(),
          i.getStatus().name(),
          i.getFacts().lineCode(),
          i.getFacts().riskCode(),
          i.getFacts().department(),
          i.getBookingDate(),
          i.getKind().name(),
          i.getSource().name(),
          i.getFacts().costCenter());
    }

    boolean matches(String filter) {
      return contains(arn, filter) || contains(clientName, filter) || contains(clientCode, filter);
    }

    private static boolean contains(String value, String filter) {
      return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }
  }
}
