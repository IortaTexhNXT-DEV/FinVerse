package com.iortatechnxt.brokerverse.integration.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.events.service.IntegrationEvent;
import com.iortatechnxt.brokerverse.events.service.IntegrationEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Publishes {@code bibs.booking.invoice-booked.v1} for every {@link InvoiceBooked}. */
@Component
public class BookingEventAdapter {

  private final IntegrationEventPublisher publisher;
  private final BookedInvoiceRepository invoices;

  /**
   * Creates the adapter.
   *
   * @param publisher integration event publisher
   * @param invoices booked invoices (company of the invoice)
   */
  public BookingEventAdapter(
      IntegrationEventPublisher publisher, BookedInvoiceRepository invoices) {
    this.publisher = publisher;
    this.invoices = invoices;
  }

  /**
   * Stores the event in the outbox of the booking transaction.
   *
   * @param event booked invoice
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
  public void on(InvoiceBooked event) {
    Long companyId =
        invoices.findByInvoiceNo(event.invoiceNo()).map(BookedInvoice::getCompanyId).orElse(null);
    publisher.publish(
        new IntegrationEvent(
            IntegrationTopics.INVOICE_BOOKED,
            IntegrationTopics.TYPE_INVOICE_BOOKED,
            event.invoiceNo(),
            companyId,
            InvoiceBookedPayload.of(event)));
  }

  /**
   * Payload of {@code booking.invoice.booked} (amounts signed: negative for return invoices).
   *
   * @param invoiceNo invoice number
   * @param arn Account Reference Number
   * @param endorsementNo endorsement number, null for an original booking
   * @param kind BOOKING, ENDORSEMENT_PLUS, ENDORSEMENT_MINUS or CANCELLATION
   * @param policyNo policy number
   * @param policyYear policy year
   * @param clientCode client code
   * @param shares insurer codes and shares in percent
   * @param currency currency
   * @param bookingDate booking date
   * @param inceptionDate period start
   * @param expiryDate period end
   * @param riskCode product
   * @param lineCode product line
   * @param productVersionNo package version, null when none
   * @param components premium by component
   * @param grossPremium sum of the components
   * @param commission commission
   * @param vatOnCommission VAT on the commission
   * @param directPayment premium paid directly to the insurer
   */
  public record InvoiceBookedPayload(
      String invoiceNo,
      String arn,
      String endorsementNo,
      String kind,
      String policyNo,
      int policyYear,
      String clientCode,
      List<InvoiceBooked.Share> shares,
      String currency,
      LocalDate bookingDate,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      String riskCode,
      String lineCode,
      Integer productVersionNo,
      Map<String, BigDecimal> components,
      BigDecimal grossPremium,
      BigDecimal commission,
      BigDecimal vatOnCommission,
      boolean directPayment) {

    static InvoiceBookedPayload of(InvoiceBooked e) {
      Map<String, BigDecimal> components = new TreeMap<>();
      e.components().forEach((k, v) -> components.put(k.name(), v));
      return new InvoiceBookedPayload(
          e.invoiceNo(),
          e.arn(),
          e.endorsementNo(),
          e.kind() == null ? null : e.kind().name(),
          e.policyNo(),
          e.policyYear(),
          e.clientCode(),
          e.shares(),
          e.currency(),
          e.bookingDate(),
          e.inceptionDate(),
          e.expiryDate(),
          e.riskCode(),
          e.lineCode(),
          e.productVersionNo(),
          components,
          e.grossPremium(),
          e.commission(),
          e.vatOnCommission(),
          e.directPayment());
    }
  }
}
