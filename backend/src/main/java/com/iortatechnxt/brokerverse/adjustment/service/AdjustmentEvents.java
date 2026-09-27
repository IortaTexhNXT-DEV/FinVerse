package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The accounting events adjustment posts itself (OPERATIONS_DESIGN section 5): AR Insurer set-up
 * (row 18), minimal balance write-off / credit (row 21) and the commission-only adjustment. Premium
 * entries of endorsements and cancellations are posted by booking, never here. Every event carries
 * the invoice's line of business and cost center and is priced at the BOOK rate (CSHID.012).
 */
@Component
public class AdjustmentEvents {

  /** AR Insurer set-up of a remitted decrease. */
  public static final String AR_INSURER_SETUP = "OPS_AR_INSURER_SETUP";

  /** Minimal balance write-off or credit. */
  public static final String WRITE_OFF = "OPS_WRITE_OFF";

  /** Commission change without premium change. */
  public static final String COMMISSION = "OPS_ADJ_COMMISSION";

  private final AccountingEventPublisher publisher;
  private final BookRates rates;

  /**
   * Creates the helper.
   *
   * @param publisher accounting engine
   * @param rates BOOK rates
   */
  public AdjustmentEvents(AccountingEventPublisher publisher, BookRates rates) {
    this.publisher = publisher;
    this.rates = rates;
  }

  /**
   * Posts an event of an invoice.
   *
   * @param invoice invoice concerned
   * @param valueDate accounting date
   * @param spec event type, references, party and amounts
   * @return the journal (the existing one when already posted)
   */
  public JournalBatch post(OpsInvoice invoice, LocalDate valueDate, Spec spec) {
    BusinessEvent event =
        new BusinessEvent(
            spec.eventType(),
            invoice.getCompanyId(),
            invoice.getBranchId(),
            valueDate,
            invoice.getCurrency(),
            Adjustments.MODULE,
            spec.sourceRef(),
            spec.reference(),
            spec.partyCode(),
            invoice.getClassification().productLine(),
            invoice.getClassification().costCenter(),
            spec.narration(),
            spec.amounts(),
            Map.of(),
            spec.componentParties());
    return publisher.publish(rates.price(event));
  }

  /**
   * One event.
   *
   * @param eventType event type
   * @param sourceRef idempotency key
   * @param reference business reference shown on the ledger
   * @param partyCode sub-ledger party
   * @param narration narration
   * @param amounts amount components
   * @param componentParties party per component, empty when all lines use the party
   */
  public record Spec(
      String eventType,
      String sourceRef,
      String reference,
      String partyCode,
      String narration,
      Map<String, BigDecimal> amounts,
      Map<String, String> componentParties) {}
}
