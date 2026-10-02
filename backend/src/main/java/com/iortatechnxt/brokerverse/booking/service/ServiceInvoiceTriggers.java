package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceType;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceTypeRepository;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.booking.domain.SiRecipient;
import com.iortatechnxt.brokerverse.booking.domain.SiTrigger;
import com.iortatechnxt.brokerverse.booking.service.BookingEvents.ShareAmounts;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.CreditRequest;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.IssueRequest;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.Remaining;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service invoices triggered by bookings (BRNB.100): the active types triggered {@code ON_BOOKING}
 * (original bookings) or {@code ON_ENDORSEMENT} (positive endorsements) are issued in the booking
 * transaction - one per insurer share for insurer types, one for an internal type addressed to the
 * cost center - and return invoices credit the service invoices of the invoice they relate to
 * (BRNB.081; ADJID.014).
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ServiceInvoiceTriggers {

  private final ServiceInvoiceService serviceInvoices;
  private final ServiceInvoiceRepository repository;
  private final ServiceInvoiceTypeRepository types;

  /**
   * Creates the triggers.
   *
   * @param serviceInvoices issue and credit
   * @param repository service invoices of an invoice
   * @param types types by trigger
   */
  public ServiceInvoiceTriggers(
      ServiceInvoiceService serviceInvoices,
      ServiceInvoiceRepository repository,
      ServiceInvoiceTypeRepository types) {
    this.serviceInvoices = serviceInvoices;
    this.repository = repository;
    this.types = types;
  }

  /**
   * Issues the service invoices a booked invoice triggers (one per insurer share for insurer types;
   * one for an internal type, addressed to the cost center).
   *
   * @param invoice booked invoice (positive)
   * @param trigger ON_BOOKING or ON_ENDORSEMENT
   * @return service invoices issued
   */
  public List<ServiceInvoice> issueFor(BookedInvoice invoice, SiTrigger trigger) {
    List<ServiceInvoice> issued = new ArrayList<>();
    if (invoice.getCommission().commission().signum() <= 0) {
      return issued;
    }
    for (ServiceInvoiceType type : types.findByTriggerAndActiveTrueOrderByCodeAsc(trigger)) {
      if (type.getRecipient() == SiRecipient.INTERNAL) {
        issued.add(
            serviceInvoices.issue(
                requestFor(invoice, type, invoice.getFacts().costCenter(), null)));
        continue;
      }
      for (ShareAmounts share : sharesOf(invoice)) {
        issued.add(serviceInvoices.issue(requestFor(invoice, type, share.insurerCode(), share)));
      }
    }
    return issued;
  }

  /**
   * Credits the service invoices of the invoice a return invoice relates to, insurer by insurer, up
   * to what is left on each (BRNB.081; ADJID.014).
   *
   * @param returnInvoice negative endorsement or cancellation
   * @return credits issued
   */
  public List<ServiceInvoice> creditFor(BookedInvoice returnInvoice) {
    List<ServiceInvoice> credits = new ArrayList<>();
    if (returnInvoice.getParentInvoiceNo() == null) {
      return credits;
    }
    List<ServiceInvoice> originals =
        repository.findByInvoiceNoOrderByIdAsc(returnInvoice.getParentInvoiceNo()).stream()
            .filter(s -> s.getKind() == SiKind.INVOICE)
            .toList();
    for (ShareAmounts share : sharesOf(returnInvoice)) {
      BigDecimal commission = share.commission().commission().negate();
      BigDecimal vat = share.commission().vatOnCommission().negate();
      originals.stream()
          .filter(s -> s.getRecipientCode().equals(share.insurerCode()))
          .findFirst()
          .flatMap(original -> creditUpTo(original, commission, vat, returnInvoice))
          .ifPresent(credits::add);
    }
    return credits;
  }

  private Optional<ServiceInvoice> creditUpTo(
      ServiceInvoice original, BigDecimal commission, BigDecimal vat, BookedInvoice returnInvoice) {
    Remaining left = serviceInvoices.remaining(original);
    BigDecimal c = commission.min(left.commission());
    BigDecimal v = vat.min(left.vat());
    if (c.signum() <= 0 && v.signum() <= 0) {
      return Optional.empty();
    }
    return Optional.of(
        serviceInvoices.credit(
            original.getSiNo(),
            new CreditRequest(
                c.max(BigDecimal.ZERO),
                v.max(BigDecimal.ZERO),
                returnInvoice.getInvoiceNo(),
                returnInvoice.getKind() + " " + returnInvoice.getEndorsementNo())));
  }

  private IssueRequest requestFor(
      BookedInvoice invoice, ServiceInvoiceType type, String recipient, ShareAmounts share) {
    CommissionTerms terms = share == null ? invoice.getCommission() : share.commission();
    return new IssueRequest(
        invoice.getCompanyId(),
        type.getCode(),
        invoice.getInvoiceNo(),
        invoice.getArn(),
        recipient,
        null,
        invoice.getBookingDate(),
        invoice.getCurrency(),
        terms.commission(),
        terms.vatOnCommission(),
        terms.wtaxAmount(),
        invoice.getKind() + " " + invoice.getInvoiceNo());
  }

  private static List<ShareAmounts> sharesOf(BookedInvoice invoice) {
    return BookingEvents.split(invoice.getPremium(), invoice.getCommission(), invoice.getShares());
  }
}
