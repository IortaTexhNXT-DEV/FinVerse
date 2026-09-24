package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiTrigger;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Books one invoice inside the caller's transaction (BRNB.027): BIR-sequential number {@code
 * BI-<branch>-<yyyy>-n}, GL posting and open items, service invoice (or its credit for a return
 * invoice) and the {@link InvoiceBooked} event. Everything is in one transaction, so a failure at
 * any step (closed period, missing rule or party) leaves no invoice, no number, no journal and no
 * event.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class InvoiceBooker {

  /** Audit entity type of booked invoices. */
  public static final String ENTITY = "BookedInvoice";

  private final BookedInvoiceRepository invoices;
  private final DocumentNumberService numbers;
  private final BookingPosting posting;
  private final ServiceInvoiceTriggers serviceInvoices;
  private final BookingSettings settings;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the booker.
   *
   * @param invoices invoices
   * @param numbers document numbers
   * @param posting GL and sub-ledger posting
   * @param serviceInvoices service invoices
   * @param settings booking branch
   * @param events event publisher
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public InvoiceBooker(
      BookedInvoiceRepository invoices,
      DocumentNumberService numbers,
      BookingPosting posting,
      ServiceInvoiceTriggers serviceInvoices,
      BookingSettings settings,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.invoices = invoices;
    this.numbers = numbers;
    this.posting = posting;
    this.serviceInvoices = serviceInvoices;
    this.settings = settings;
    this.events = events;
    this.audit = audit;
    this.clock = clock;
    this.currentUser = currentUser;
  }

  /**
   * Books an invoice.
   *
   * @param invoice invoice not yet booked
   * @param date booking date (not in the future; its period must be open)
   * @param source how it is booked
   * @return the booked invoice
   */
  public BookedInvoice book(BookedInvoice invoice, LocalDate date, BookingSource source) {
    if (date.isAfter(LocalDate.now(clock))) {
      throw new BusinessRuleException(
          "BOOKING_DATE_FUTURE", "The booking date " + date + " is in the future");
    }
    String branch = settings.branch(invoice.getCompanyId()).code();
    String number = numbers.next("BI-" + branch + "-" + date.getYear());
    invoice.book(number, date, source, currentUser.username(), clock.instant());
    BookedInvoice saved = invoices.save(invoice);
    posting.post(saved);
    List<ServiceInvoice> issued = serviceInvoicesOf(saved);
    if (!issued.isEmpty()) {
      saved.linkServiceInvoice(issued.get(0).getSiNo());
    }
    events.publishEvent(InvoiceBooked.of(saved));
    audit.record(
        ENTITY,
        number,
        AuditAction.CREATE,
        saved.getKind()
            + " of "
            + saved.getArn()
            + " booked on "
            + date
            + ", gross "
            + saved.getPremium().total().toPlainString()
            + ", commission "
            + saved.getCommission().commission().toPlainString()
            + ", cost center "
            + saved.getFacts().costCenter());
    return saved;
  }

  private List<ServiceInvoice> serviceInvoicesOf(BookedInvoice invoice) {
    if (invoice.getKind() == InvoiceKind.BOOKING) {
      return serviceInvoices.issueFor(invoice, SiTrigger.ON_BOOKING);
    }
    if (invoice.getKind() == InvoiceKind.ENDORSEMENT_PLUS) {
      return serviceInvoices.issueFor(invoice, SiTrigger.ON_ENDORSEMENT);
    }
    return serviceInvoices.creditFor(invoice);
  }
}
