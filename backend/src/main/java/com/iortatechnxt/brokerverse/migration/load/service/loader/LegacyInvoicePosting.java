package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerContext;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshot;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.opsledger.service.LegacyInvoiceIntake;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The GL side of an open legacy invoice (DATA_MIGRATION_DESIGN 14.2): the opening event {@code
 * MIG_LEGACY_INVOICE_OPENING} per insurer share (legacy premium receivable by component and PR 2307
 * of the client on the lead share; due to insurer, commission and VAT receivable net of withholding
 * tax, unrealised commission and deferred VAT per share; balanced on the migration clearing
 * account) and the sub-ledger open items of the open balances with document types LEGACY_PREMIUM,
 * LEGACY_DTIP and LEGACY_COMMISSION. A rolled-back batch posts the same events negated and settles
 * the open items against opposite items.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class LegacyInvoicePosting {

  /** Opening event of a legacy invoice. */
  public static final String EVENT = "MIG_LEGACY_INVOICE_OPENING";

  /** Year-end adjustment event of a legacy position. */
  public static final String TRUEUP_EVENT = "MIG_LEGACY_POSITION_TRUEUP";

  private static final String CLEARING = "CLEARING";
  private static final String ROLLBACK = ":RB";
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final List<PartyType> CLIENTS =
      List.of(PartyType.INDIVIDUAL_CLIENT, PartyType.CORPORATE_CLIENT);
  private static final List<PartyType> INSURERS = List.of(PartyType.INSURER);
  private static final Map<LedgerComponent, String> PR =
      Map.of(
          LedgerComponent.BASIC, "PR_BASIC",
          LedgerComponent.DST, "PR_DST",
          LedgerComponent.PREMIUM_TAX_VAT, "PR_PTX_VAT",
          LedgerComponent.LGT, "PR_LGT",
          LedgerComponent.FST, "PR_FST",
          LedgerComponent.OTHER, "PR_OTHER",
          LedgerComponent.PR2307, "PR2307");
  private static final List<String> CREDITS =
      List.of("DTIP", "UNREALIZED", "DEFERRED_VAT", "AMOUNT");

  private final AccountingEventPublisher publisher;
  private final BookRates rates;
  private final OpenItemService openItems;
  private final PartyService parties;

  /**
   * Creates the posting.
   *
   * @param publisher accounting engine
   * @param rates book rates
   * @param openItems party sub-ledger
   * @param parties parties
   */
  public LegacyInvoicePosting(
      AccountingEventPublisher publisher,
      BookRates rates,
      OpenItemService openItems,
      PartyService parties) {
    this.publisher = publisher;
    this.rates = rates;
    this.openItems = openItems;
    this.parties = parties;
  }

  /**
   * Posts the opening of a legacy invoice just recorded in the ledger.
   *
   * @param invoice the invoice (components and shares loaded)
   * @param header legacy header facts
   * @param valueDate value date of the opening
   * @return journal batch numbers
   */
  public List<String> open(
      OpsInvoice invoice, OpsInvoiceOriginSnapshot.Header header, LocalDate valueDate) {
    List<String> batches = new ArrayList<>();
    for (ShareAmounts share : shares(invoice, header)) {
      String batch = publish(invoice, share, valueDate, false);
      if (batch != null) {
        batches.add(batch);
      }
    }
    items(invoice, header, valueDate, batches.isEmpty() ? null : batches.get(0));
    return batches;
  }

  /**
   * Reverses the opening of a legacy invoice of a rolled-back batch.
   *
   * @param invoice the invoice (components and shares loaded)
   * @param header legacy header facts
   * @param valueDate value date of the reversal
   */
  public void reverse(
      OpsInvoice invoice, OpsInvoiceOriginSnapshot.Header header, LocalDate valueDate) {
    for (ShareAmounts share : shares(invoice, header)) {
      publish(invoice, share, valueDate, true);
    }
    String prefix = sourceRef(invoice) + ":";
    for (String code : partyCodes(invoice)) {
      Party party = parties.getByCode(invoice.getCompanyId(), code);
      for (OpenItem item : openItems.partyItems(invoice.getCompanyId(), party.getId())) {
        boolean ours =
            item.getSourceReference() != null
                && item.getSourceReference().startsWith(prefix)
                && !item.getSourceReference().endsWith(ROLLBACK);
        if (ours && item.outstanding().signum() > 0) {
          settle(item, party, valueDate);
        }
      }
    }
  }

  private void settle(OpenItem item, Party party, LocalDate date) {
    OpenItem opposite =
        openItems.record(
            new OpenItemValues(
                item.getCompanyId(),
                item.getBranchId(),
                party.getId(),
                party.getCode(),
                item.getDirection().opposite(),
                item.getDocumentType(),
                item.getDocumentNo(),
                date,
                date,
                item.getCurrency(),
                item.outstanding(),
                item.getBaseAmount(),
                LegacyInvoiceIntake.MODULE,
                item.getSourceReference() + ROLLBACK,
                null,
                "Migration batch rolled back"));
    boolean debit = item.getDirection() == ItemDirection.DEBIT;
    openItems.match(
        debit ? item.getId() : opposite.getId(),
        debit ? opposite.getId() : item.getId(),
        item.outstanding(),
        date);
  }

  private List<String> partyCodes(OpsInvoice invoice) {
    List<String> codes = new ArrayList<>();
    codes.add(invoice.getClientCode());
    invoice.getShares().forEach(s -> codes.add(s.insurerCode()));
    return codes;
  }

  private String publish(OpsInvoice invoice, ShareAmounts share, LocalDate date, boolean reverse) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    share.amounts().forEach((k, v) -> amounts.put(k, reverse ? v.negate() : v));
    return publish(
        new Posting(
            invoice,
            EVENT,
            sourceRef(invoice) + ":" + share.insurerCode() + (reverse ? ROLLBACK : ""),
            share.insurerCode(),
            date,
            "Legacy invoice "
                + invoice.getLegacy().legacyInvoiceNo()
                + (reverse ? " - migration batch rolled back" : " - opening at cut-over")),
        amounts);
  }

  /**
   * Posts a year-end adjustment of the position of a legacy invoice ({@code
   * MIG_LEGACY_POSITION_TRUEUP}): the legacy control account of each component against migration
   * clearing.
   *
   * @param invoice the invoice
   * @param changes change of each position by component name without the legacy prefix (PR_BASIC,
   *     DTIP, COMMISSION, UNREALIZED...), in its natural sign
   * @param sourceRef true-up reference of the item and component
   * @param date value date
   * @return journal batch, null when nothing changed
   */
  public String trueUp(
      OpsInvoice invoice, Map<String, BigDecimal> changes, String sourceRef, LocalDate date) {
    return publish(
        new Posting(
            invoice,
            TRUEUP_EVENT,
            sourceRef,
            invoice.getInsurerCode(),
            date,
            "Year-end adjustment of legacy invoice " + invoice.getLegacy().legacyInvoiceNo()),
        changes);
  }

  /**
   * The name of the event component of a ledger component (without the legacy prefix).
   *
   * @param component ledger component
   * @return PR_BASIC, PR_DST..., PR2307, DTIP, COMMISSION or COMMISSION_VAT; WTAX nets the
   *     commission receivable
   */
  public static String eventComponent(LedgerComponent component) {
    return switch (component) {
      case DTIP, COMMISSION, COMMISSION_VAT -> component.name();
      case WTAX -> "COMMISSION";
      default -> PR.get(component);
    };
  }

  private String publish(Posting p, Map<String, BigDecimal> changes) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    Map<String, String> componentParties = new LinkedHashMap<>();
    BigDecimal clearing = BigDecimal.ZERO;
    for (Map.Entry<String, BigDecimal> e : changes.entrySet()) {
      if (e.getValue().signum() == 0) {
        continue;
      }
      String component = LedgerContext.LEGACY.component(e.getKey());
      amounts.merge(component, e.getValue(), BigDecimal::add);
      clearing =
          CREDITS.contains(e.getKey())
              ? clearing.subtract(e.getValue())
              : clearing.add(e.getValue());
      if (!e.getKey().startsWith("PR")) {
        componentParties.put(component, p.insurerCode());
      }
    }
    if (amounts.isEmpty()) {
      return null;
    }
    amounts.put(CLEARING, clearing);
    OpsInvoice invoice = p.invoice();
    BusinessEvent event =
        new BusinessEvent(
            p.eventType(),
            invoice.getCompanyId(),
            invoice.getBranchId(),
            p.date(),
            invoice.getCurrency(),
            LegacyInvoiceIntake.MODULE,
            p.sourceRef(),
            invoice.getInvoiceNo(),
            invoice.getClientCode(),
            invoice.getClassification().productLine(),
            invoice.getClassification().costCenter(),
            p.narration(),
            amounts,
            Map.of(),
            componentParties);
    return publisher.publish(rates.price(event)).getBatchNo();
  }

  /**
   * An event to publish for an invoice.
   *
   * @param invoice invoice
   * @param eventType event type
   * @param sourceRef idempotency key
   * @param insurerCode party of the insurer components
   * @param date value date
   * @param narration narration
   */
  private record Posting(
      OpsInvoice invoice,
      String eventType,
      String sourceRef,
      String insurerCode,
      LocalDate date,
      String narration) {}

  private static String sourceRef(OpsInvoice invoice) {
    return "MIG:INV:" + invoice.getInvoiceNo();
  }

  /** Amounts per share: the client's premium receivable on the lead share only. */
  private static List<ShareAmounts> shares(
      OpsInvoice invoice, OpsInvoiceOriginSnapshot.Header header) {
    Map<LedgerComponent, BigDecimal> open = invoice.balances();
    Map<String, BigDecimal> insurer = new LinkedHashMap<>();
    insurer.put("DTIP", nz(open.get(LedgerComponent.DTIP)));
    insurer.put(
        "COMMISSION",
        nz(open.get(LedgerComponent.COMMISSION)).subtract(nz(open.get(LedgerComponent.WTAX))));
    insurer.put("COMMISSION_VAT", nz(open.get(LedgerComponent.COMMISSION_VAT)));
    insurer.put(
        "UNREALIZED",
        invoice.getCommission().subtract(nz(header.commissionRealised())).max(BigDecimal.ZERO));
    insurer.put("DEFERRED_VAT", nz(header.deferredVatOpen()));
    List<ShareAmounts> out = new ArrayList<>();
    Map<String, BigDecimal> given = new LinkedHashMap<>();
    List<OpsInvoiceShare> shares = invoice.getShares();
    OpsInvoiceShare lead = invoice.leadShare();
    for (int i = 0; i < shares.size(); i++) {
      OpsInvoiceShare s = shares.get(i);
      boolean last = i == shares.size() - 1;
      Map<String, BigDecimal> amounts = new LinkedHashMap<>();
      if (s.equals(lead)) {
        PR.forEach((c, name) -> amounts.put(name, nz(open.get(c))));
      }
      insurer.forEach(
          (name, total) -> {
            BigDecimal part = last ? total.subtract(nz(given.get(name))) : portion(total, s);
            given.merge(name, part, BigDecimal::add);
            amounts.put(name, part);
          });
      out.add(new ShareAmounts(s.insurerCode(), amounts));
    }
    return out;
  }

  private static BigDecimal portion(BigDecimal total, OpsInvoiceShare share) {
    return total.multiply(share.sharePct()).divide(HUNDRED, Money.SCALE, RoundingMode.HALF_EVEN);
  }

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /** Open items of the open balances. */
  private void items(
      OpsInvoice invoice, OpsInvoiceOriginSnapshot.Header header, LocalDate date, String batch) {
    BigDecimal rate = rates.rate(invoice.getCompanyId(), invoice.getCurrency(), date);
    Item.Dates dates = new Item.Dates(header.invoiceDate(), header.dueDate());
    Map<LedgerComponent, BigDecimal> open = invoice.balances();
    BigDecimal premium = invoice.premiumBalance().add(nz(open.get(LedgerComponent.PR2307)));
    record(
        invoice,
        new Item(
            party(invoice, invoice.getClientCode(), CLIENTS),
            "LEGACY_PREMIUM",
            premium,
            ItemDirection.DEBIT,
            ":PR"),
        dates,
        rate,
        batch);
    for (ShareAmounts share : shares(invoice, header)) {
      Party insurer = party(invoice, share.insurerCode(), INSURERS);
      BigDecimal commission =
          nz(share.amounts().get("COMMISSION")).add(nz(share.amounts().get("COMMISSION_VAT")));
      record(
          invoice,
          new Item(
              insurer,
              "LEGACY_DTIP",
              nz(share.amounts().get("DTIP")),
              ItemDirection.CREDIT,
              ":DTIP:" + insurer.getCode()),
          dates,
          rate,
          batch);
      record(
          invoice,
          new Item(
              insurer,
              "LEGACY_COMMISSION",
              commission,
              ItemDirection.DEBIT,
              ":COMM:" + insurer.getCode()),
          dates,
          rate,
          batch);
    }
  }

  private void record(
      OpsInvoice invoice, Item item, Item.Dates dates, BigDecimal rate, String batch) {
    if (item.amount().signum() == 0) {
      return;
    }
    BigDecimal amount = item.amount().abs();
    openItems.record(
        new OpenItemValues(
            invoice.getCompanyId(),
            invoice.getBranchId(),
            item.party().getId(),
            item.party().getCode(),
            item.amount().signum() > 0 ? item.direction() : item.direction().opposite(),
            item.documentType(),
            invoice.getInvoiceNo(),
            dates.document(),
            dates.due() == null ? dates.document() : dates.due(),
            invoice.getCurrency(),
            amount,
            Money.convert(amount, rate),
            LegacyInvoiceIntake.MODULE,
            sourceRef(invoice) + item.suffix(),
            batch,
            "Legacy invoice " + invoice.getLegacy().legacyInvoiceNo()));
  }

  private Party party(OpsInvoice invoice, String code, List<PartyType> types) {
    try {
      return parties.requireActive(invoice.getCompanyId(), code, types);
    } catch (ResourceNotFoundException ex) {
      throw new BusinessRuleException(
          "MIG_PARTY_MISSING",
          "No sub-ledger party " + code + " for legacy invoice " + invoice.getInvoiceNo(),
          ex);
    }
  }

  /**
   * Event amounts of one insurer share.
   *
   * @param insurerCode insurer
   * @param amounts amounts by component name without the legacy prefix
   */
  private record ShareAmounts(String insurerCode, Map<String, BigDecimal> amounts) {}

  /**
   * An open item to record.
   *
   * @param party party
   * @param documentType document type
   * @param amount signed amount (negative flips the direction)
   * @param direction direction of a positive amount
   * @param suffix source reference suffix
   */
  private record Item(
      Party party, String documentType, BigDecimal amount, ItemDirection direction, String suffix) {

    /**
     * Dates of the legacy document.
     *
     * @param document legacy invoice date
     * @param due legacy due date
     */
    private record Dates(LocalDate document, LocalDate due) {}
  }
}
