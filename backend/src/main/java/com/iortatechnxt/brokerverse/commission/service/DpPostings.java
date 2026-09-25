package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Accounting and ledger postings of direct payment commission (OPERATIONS_DESIGN 5 rows 22-24,
 * MKTID.012): the commission collected ({@code OPS_DP_COMMISSION_COLLECT}: bank and creditable
 * withholding tax against the commission receivable, with realization when commission is realized
 * on collection) with an APPLIED movement on the commission components; the premium receivable
 * reversal (DP_REVERSAL movements on the PR components and DTIP, and {@code OPS_DP_PR_REVERSAL}
 * when {@code DP_PR_REVERSAL_POSTING} is on, since booking posts no PR for direct payment invoices)
 * and its reinstatement ({@code OPS_DP_REINSTATE}). Every event is priced at the BOOK rate and
 * keyed by an idempotent source reference; the lead insurer is the party of the commission.
 */
@Component
public class DpPostings {

  private static final String ON_BOOKING = "ON_BOOKING";

  private static final String COMMISSION = DpIntakeService.MODULE;
  private static final Map<LedgerComponent, String> PR_COMPONENTS =
      Map.of(
          LedgerComponent.BASIC, "PR_BASIC",
          LedgerComponent.DST, "PR_DST",
          LedgerComponent.PREMIUM_TAX_VAT, "PR_PTX_VAT",
          LedgerComponent.LGT, "PR_LGT",
          LedgerComponent.FST, "PR_FST",
          LedgerComponent.OTHER, "PR_OTHER");

  private final AccountingEventPublisher publisher;
  private final BookRates rates;
  private final InvoiceLedgerService ledger;
  private final InvoiceLedgerQueryService invoices;
  private final SystemParameterService parameters;

  /**
   * Creates the postings.
   *
   * @param publisher accounting engine
   * @param rates BOOK rates
   * @param ledger Operations ledger (movements)
   * @param invoices Operations ledger (reads)
   * @param parameters business parameters
   */
  public DpPostings(
      AccountingEventPublisher publisher,
      BookRates rates,
      InvoiceLedgerService ledger,
      InvoiceLedgerQueryService invoices,
      SystemParameterService parameters) {
    this.publisher = publisher;
    this.rates = rates;
    this.ledger = ledger;
    this.invoices = invoices;
    this.parameters = parameters;
  }

  /**
   * Posts the commission collected on an account (row 23).
   *
   * @param item account (approved)
   * @param collection value date, bank, OR and billing
   * @return the journal batch number
   */
  public String collect(DpItem item, Collection collection) {
    OpsInvoice invoice = invoice(item);
    String timing = parameters.text("OPS_COMMISSION_REALIZATION", "ON_COLLECTION").strip();
    boolean onBooking =
        timing.length() == ON_BOOKING.length()
            && ON_BOOKING.regionMatches(true, 0, timing, 0, timing.length());
    boolean realize = !onBooking;
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    put(amounts, "CASH", item.getNetCommission());
    put(amounts, "CWT", item.getWtax());
    put(amounts, "COMMISSION_RECEIVABLE", item.getCommission().add(item.getCommissionVat()));
    if (realize) {
      put(amounts, "REALIZED_COMMISSION", item.getCommission());
      put(amounts, "REALIZED_VAT", item.getCommissionVat());
    }
    String ref = "DPC:" + item.getId();
    String batch =
        publisher
            .publish(
                rates.price(
                    event(
                        "OPS_DP_COMMISSION_COLLECT",
                        invoice,
                        new EventKeys(collection.valueDate(), ref, invoice.getInsurerCode()),
                        amounts,
                        Map.of("BANK", collection.bankAccount()),
                        Map.of())))
            .getBatchNo();
    Map<LedgerComponent, BigDecimal> applied = new EnumMap<>(LedgerComponent.class);
    applied.put(LedgerComponent.COMMISSION, item.getCommission());
    applied.put(LedgerComponent.COMMISSION_VAT, item.getCommissionVat());
    applied.put(LedgerComponent.WTAX, item.getWtax());
    ledger.post(
        new MovementRequest(
            item.getInvoiceNo(),
            MovementType.APPLIED,
            COMMISSION,
            ref,
            collection.valueDate(),
            applied,
            new DocumentRefs(null, collection.orNo(), collection.billingNo(), batch),
            "Direct payment commission collected"));
    return batch;
  }

  /**
   * Reverses the premium receivable of a collected account (row 22, MKTID.012): the remaining PR by
   * component and DTIP.
   *
   * @param item account
   * @param seq reversal sequence of the account
   * @param valueDate value date
   * @return the journal batch number, null when the GL posting is off
   */
  public String reversePremium(DpItem item, int seq, LocalDate valueDate) {
    OpsInvoice invoice = invoice(item);
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    for (OpsInvoiceComponent c : invoice.getComponents()) {
      boolean reversible =
          c.getComponent().isPremiumReceivable() || c.getComponent() == LedgerComponent.DTIP;
      if (reversible && c.getBalance().signum() > 0) {
        amounts.put(c.getComponent(), c.getBalance());
      }
    }
    String ref = "DP:" + item.getInvoiceNo() + ":" + seq;
    return premiumMovement(
        invoice, new EventKeys(valueDate, ref, invoice.getInsurerCode()), amounts, false);
  }

  /**
   * Reinstates a reversed premium receivable (row 24, CSHID.004 b).
   *
   * @param item account
   * @param seq reversal sequence to undo
   * @param reinstatement reinstatement sequence of the account
   * @param valueDate value date
   * @return the journal batch number, null when the GL posting is off
   */
  public String reinstatePremium(DpItem item, int seq, int reinstatement, LocalDate valueDate) {
    OpsInvoice invoice = invoice(item);
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    List<OpsInvoiceMovement> reversal =
        invoices.movementsOf(COMMISSION, "DP:" + item.getInvoiceNo() + ":" + seq);
    reversal.forEach(m -> amounts.merge(m.getComponent(), m.getAmount().negate(), BigDecimal::add));
    String ref = "DPR:" + item.getInvoiceNo() + ":" + reinstatement;
    return premiumMovement(
        invoice, new EventKeys(valueDate, ref, invoice.getInsurerCode()), amounts, true);
  }

  private String premiumMovement(
      OpsInvoice invoice,
      EventKeys keys,
      Map<LedgerComponent, BigDecimal> amounts,
      boolean reinstate) {
    if (amounts.isEmpty()) {
      return null;
    }
    String batch = null;
    if (Boolean.parseBoolean(parameters.text("DP_PR_REVERSAL_POSTING", "false").strip())) {
      Map<String, BigDecimal> gl = new LinkedHashMap<>();
      Map<String, String> parties = new HashMap<>();
      amounts.forEach(
          (component, amount) -> {
            String code = component == LedgerComponent.DTIP ? "DTIP" : PR_COMPONENTS.get(component);
            gl.put(code, amount);
            parties.put(
                code,
                component == LedgerComponent.DTIP
                    ? invoice.getInsurerCode()
                    : invoice.getClientCode());
          });
      batch =
          publisher
              .publish(
                  rates.price(
                      event(
                          reinstate ? "OPS_DP_REINSTATE" : "OPS_DP_PR_REVERSAL",
                          invoice,
                          keys,
                          gl,
                          Map.of(),
                          parties)))
              .getBatchNo();
    }
    ledger.post(
        new MovementRequest(
            invoice.getInvoiceNo(),
            MovementType.DP_REVERSAL,
            COMMISSION,
            keys.sourceRef(),
            keys.valueDate(),
            amounts,
            new DocumentRefs(null, null, null, batch),
            reinstate ? "Direct payment PR reinstated" : "Direct payment PR reversed"));
    return batch;
  }

  private OpsInvoice invoice(DpItem item) {
    return invoices
        .find(item.getInvoiceNo())
        .orElseThrow(
            () -> new ResourceNotFoundException("Operations invoice", item.getInvoiceNo()));
  }

  private static void put(Map<String, BigDecimal> amounts, String component, BigDecimal amount) {
    if (amount != null && amount.signum() != 0) {
      amounts.put(component, amount);
    }
  }

  private static BusinessEvent event(
      String type,
      OpsInvoice invoice,
      EventKeys keys,
      Map<String, BigDecimal> amounts,
      Map<String, String> accounts,
      Map<String, String> parties) {
    return new BusinessEvent(
        type,
        invoice.getCompanyId(),
        invoice.getBranchId(),
        keys.valueDate(),
        invoice.getCurrency(),
        COMMISSION,
        keys.sourceRef(),
        invoice.getInvoiceNo(),
        keys.partyCode(),
        invoice.getClassification().productLine(),
        invoice.getClassification().costCenter(),
        type + " " + invoice.getInvoiceNo(),
        amounts,
        accounts,
        parties);
  }

  /**
   * Keys of an event.
   *
   * @param valueDate value date
   * @param sourceRef idempotent source reference
   * @param partyCode main party
   */
  private record EventKeys(LocalDate valueDate, String sourceRef, String partyCode) {}

  /**
   * A collection.
   *
   * @param valueDate collection date
   * @param bankAccount GL bank account (role BANK)
   * @param orNo commission OR, null while handed over
   * @param billingNo billing
   */
  public record Collection(
      LocalDate valueDate, String bankAccount, String orNo, String billingNo) {}
}
