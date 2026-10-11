package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.booking.service.InvoiceSearch;
import com.iortatechnxt.brokerverse.opsledger.domain.FeedSource;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun.FileRef;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Rebuilds the Operations ledger from booking (OPERATIONS_DESIGN 2.2 replay): every booked invoice
 * of a company, of an account or one invoice that is missing from the ledger is copied from {@code
 * BookingQueryService} as a logged {@code OPS_INVOICE_FEED} run, one record per invoice.
 */
@Service
public class InvoiceFeedReplayService {

  private static final int PAGE = 200;

  private final BookingQueryService bookings;
  private final OpsInvoiceRepository invoices;
  private final InvoiceLedgerWriter writer;
  private final FlowInService flowIn;
  private final CompanyRepository companies;

  /**
   * Creates the service.
   *
   * @param bookings booked invoices
   * @param invoices ledger invoices
   * @param writer ledger writer
   * @param flowIn flow-in runs
   * @param companies companies
   */
  public InvoiceFeedReplayService(
      BookingQueryService bookings,
      OpsInvoiceRepository invoices,
      InvoiceLedgerWriter writer,
      FlowInService flowIn,
      CompanyRepository companies) {
    this.bookings = bookings;
    this.invoices = invoices;
    this.writer = writer;
    this.flowIn = flowIn;
    this.companies = companies;
  }

  /**
   * Replays every booked invoice of every company that is missing from the ledger.
   *
   * @param trigger what started the replay
   * @return the run
   */
  public FlowInRun replayAll(Trigger trigger) {
    List<String> numbers = new ArrayList<>();
    for (Company company : companies.findAll()) {
      numbers.addAll(bookedInvoices(company.getId()));
    }
    return replay(numbers, trigger);
  }

  /**
   * Replays the booked invoices of one company that are missing from the ledger.
   *
   * @param companyId company
   * @param trigger what started the replay
   * @return the run
   */
  public FlowInRun replayCompany(Long companyId, Trigger trigger) {
    return replay(bookedInvoices(companyId), trigger);
  }

  /**
   * Replays the booked invoices of an account that are missing from the ledger.
   *
   * @param arn Account Reference Number
   * @return the run
   */
  public FlowInRun replayAccount(String arn) {
    return replay(
        bookings.invoicesForArn(arn).stream().map(InvoiceBooked::invoiceNo).toList(),
        Trigger.MANUAL);
  }

  /**
   * Replays one booked invoice when it is missing from the ledger.
   *
   * @param invoiceNo invoice
   * @return the run
   */
  public FlowInRun replayInvoice(String invoiceNo) {
    return replay(List.of(invoiceNo), Trigger.MANUAL);
  }

  private List<String> bookedInvoices(Long companyId) {
    List<String> numbers = new ArrayList<>();
    InvoiceSearch search =
        new InvoiceSearch(companyId, null, InvoiceStatus.BOOKED, null, null, null, null, null);
    int page = 0;
    Page<BookedInvoice> slice;
    do {
      slice = bookings.search(search, PageRequest.of(page++, PAGE, Sort.by("id")));
      slice.forEach(i -> numbers.add(i.getInvoiceNo()));
    } while (slice.hasNext());
    return numbers;
  }

  private FlowInRun replay(List<String> invoiceNos, Trigger trigger) {
    List<String> missing = new ArrayList<>(invoiceNos);
    if (!invoiceNos.isEmpty()) {
      missing.removeAll(invoices.findExisting(invoiceNos));
    }
    return flowIn.run(
        InvoiceLedgerFeed.FEED,
        trigger,
        FileRef.NONE,
        ctx ->
            missing.forEach(
                no ->
                    ctx.accept(
                        no,
                        no,
                        () ->
                            writer
                                .record(bookings.invoice(no), FeedSource.REPLAY)
                                .map(i -> i.getInvoiceNo())
                                .orElse(no))));
  }
}
