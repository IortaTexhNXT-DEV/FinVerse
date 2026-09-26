package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceOpenItem;
import com.iortatechnxt.brokerverse.booking.domain.OpenItemRole;
import com.iortatechnxt.brokerverse.booking.service.BookingEvents.PostingFacts;
import com.iortatechnxt.brokerverse.booking.service.BookingEvents.ShareEvent;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.currency.domain.RateType;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts a booked invoice inside the booking transaction (BRNB.027): publishes its {@code
 * BROKER_BOOKING} events to the accounting engine (the only posting path) and records the open
 * items of the invoice - client premium receivable (DEBIT), insurer DTIP (CREDIT) and insurer
 * commission receivable (DEBIT) per share. Return invoices flip the directions. Any failure (closed
 * period, missing rule or party) rolls the whole booking back.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class BookingPosting {

  private static final List<PartyType> CLIENTS =
      List.of(PartyType.INDIVIDUAL_CLIENT, PartyType.CORPORATE_CLIENT);
  private static final List<PartyType> INSURERS = List.of(PartyType.INSURER);

  private final BookingEvents events;
  private final AccountingEventPublisher publisher;
  private final OpenItemService openItems;
  private final PartyService parties;
  private final CurrencyService currencies;
  private final BookingSettings settings;

  /**
   * Creates the posting.
   *
   * @param events event builder
   * @param publisher accounting engine
   * @param openItems party sub-ledger
   * @param parties parties
   * @param currencies exchange rates
   * @param settings company facts
   */
  public BookingPosting(
      BookingEvents events,
      AccountingEventPublisher publisher,
      OpenItemService openItems,
      PartyService parties,
      CurrencyService currencies,
      BookingSettings settings) {
    this.events = events;
    this.publisher = publisher;
    this.openItems = openItems;
    this.parties = parties;
    this.currencies = currencies;
    this.settings = settings;
  }

  /**
   * The posting facts of an invoice.
   *
   * @param invoice invoice (booked or about to be)
   * @return facts
   */
  public static PostingFacts factsOf(BookedInvoice invoice) {
    return new PostingFacts(
        invoice.getCompanyId(),
        invoice.getBranchId(),
        invoice.getInvoiceNo(),
        invoice.getBookingDate(),
        invoice.getCurrency(),
        invoice.getFacts().clientCode(),
        invoice.getFacts().lineCode(),
        invoice.getFacts().costCenter(),
        invoice.getKind() + " " + invoice.getInvoiceNo() + " " + invoice.getArn(),
        invoice.getFlags().directPayment(),
        invoice.getPremium(),
        invoice.getCommission(),
        invoice.getShares());
  }

  /**
   * Posts a booked invoice and records its open items on it.
   *
   * @param invoice booked invoice (numbered and dated)
   */
  public void post(BookedInvoice invoice) {
    boolean premiumLegs = !invoice.getFlags().directPayment();
    Party client = premiumLegs ? party(invoice, invoice.getFacts().clientCode()) : null;
    BigDecimal rate = rateOf(invoice);
    List<String> batches = new ArrayList<>();
    List<ItemSpec> specs = new ArrayList<>();
    for (ShareEvent share : events.events(factsOf(invoice))) {
      if (!share.hasAmounts()) {
        continue;
      }
      String batch = publisher.publish(share.event()).getBatchNo();
      batches.add(batch);
      Party insurer = party(invoice, share.insurerCode());
      if (premiumLegs) {
        specs.add(
            new ItemSpec(
                insurer,
                OpenItemRole.INSURER_DTIP,
                share.premium().total(),
                ItemDirection.CREDIT,
                ":DTIP:" + insurer.getCode(),
                batch));
      }
      specs.add(
          new ItemSpec(
              insurer,
              OpenItemRole.INSURER_COMMISSION,
              share.commission().receivable(),
              ItemDirection.DEBIT,
              ":COMM:" + insurer.getCode(),
              batch));
    }
    List<InvoiceOpenItem> items = new ArrayList<>();
    if (client != null) {
      items.addAll(
          record(
              invoice,
              new ItemSpec(
                  client,
                  OpenItemRole.CLIENT_PREMIUM,
                  invoice.getPremium().total(),
                  ItemDirection.DEBIT,
                  ":PR",
                  batches.isEmpty() ? null : batches.get(0)),
              rate));
    }
    specs.forEach(spec -> items.addAll(record(invoice, spec, rate)));
    invoice.recordPosting(batches, items);
  }

  private List<InvoiceOpenItem> record(BookedInvoice invoice, ItemSpec spec, BigDecimal rate) {
    if (spec.amount().signum() == 0) {
      return List.of();
    }
    boolean positive = spec.amount().signum() > 0;
    BigDecimal amount = spec.amount().abs();
    OpenItem item =
        openItems.record(
            new OpenItemValues(
                invoice.getCompanyId(),
                invoice.getBranchId(),
                spec.party().getId(),
                spec.party().getCode(),
                positive ? spec.direction() : spec.direction().opposite(),
                documentType(spec.role(), positive),
                invoice.getInvoiceNo(),
                invoice.getBookingDate(),
                invoice.getBookingDate().plusDays(spec.party().getCreditDays()),
                invoice.getCurrency(),
                amount,
                Money.convert(amount, rate),
                BookingEvents.MODULE,
                "BKG:" + invoice.getInvoiceNo() + spec.suffix(),
                spec.batchNo(),
                invoice.getKind() + " " + invoice.getArn()));
    return List.of(new InvoiceOpenItem(item.getId(), spec.role()));
  }

  private static String documentType(OpenItemRole role, boolean positive) {
    return switch (role) {
      case CLIENT_PREMIUM -> positive ? "BOOKED_INVOICE" : "RETURN_PREMIUM";
      case INSURER_DTIP -> positive ? "DUE_TO_INSURER" : "DTIP_RETURN";
      case INSURER_COMMISSION -> positive ? "COMMISSION_RECEIVABLE" : "COMMISSION_RETURN";
    };
  }

  private Party party(BookedInvoice invoice, String code) {
    boolean client = code.equals(invoice.getFacts().clientCode());
    try {
      return parties.requireActive(invoice.getCompanyId(), code, client ? CLIENTS : INSURERS);
    } catch (ResourceNotFoundException ex) {
      throw new BusinessRuleException(
          client ? "CLIENT_PARTY_MISSING" : "INSURER_PARTY_MISSING",
          "No sub-ledger party " + code + " for invoice " + invoice.getInvoiceNo(),
          ex);
    }
  }

  private BigDecimal rateOf(BookedInvoice invoice) {
    String base = settings.company(invoice.getCompanyId()).getBaseCurrency();
    return currencies.rateOn(base, invoice.getCurrency(), RateType.SPOT, invoice.getBookingDate());
  }

  /**
   * An open item to record.
   *
   * @param party party
   * @param role role
   * @param amount signed amount (negative flips the direction)
   * @param direction direction of a positive amount
   * @param suffix source reference suffix
   * @param batchNo journal batch
   */
  private record ItemSpec(
      Party party,
      OpenItemRole role,
      BigDecimal amount,
      ItemDirection direction,
      String suffix,
      String batchNo) {}
}
