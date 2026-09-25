package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventType;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventTypeRepository;
import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalService;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Posts an approved voucher and reverses a cancelled one (DIS 2.19.0, 2.20.0, 3.27.0; design 6 rows
 * 1-7, 11). A proforma that follows the rule is published as the {@code DISB_VOUCHER} event (the
 * engine records it in the event register; its lines are refreshed from the rule at approval); an
 * edited proforma is posted as given through the system journal under the same source reference
 * {@code DV:<no>}. The cancellation of an approved voucher posts the same entry with the opposite
 * sign under {@code DV:<no>:CANCEL}. Both are idempotent on their source reference.
 */
@Component
public class VoucherPosting {

  private final AccountingEventPublisher publisher;
  private final SystemJournalService journals;
  private final AccountingEventTypeRepository eventTypes;
  private final ProformaBuilder proforma;
  private final BookRates rates;

  /**
   * Creates the poster.
   *
   * @param publisher accounting engine
   * @param journals system journals
   * @param eventTypes event types (journal type)
   * @param proforma proforma entry
   * @param rates BOOK rates
   */
  public VoucherPosting(
      AccountingEventPublisher publisher,
      SystemJournalService journals,
      AccountingEventTypeRepository eventTypes,
      ProformaBuilder proforma,
      BookRates rates) {
    this.publisher = publisher;
    this.journals = journals;
    this.eventTypes = eventTypes;
    this.proforma = proforma;
    this.rates = rates;
  }

  /**
   * Posts the approval entry.
   *
   * @param v voucher
   * @return the journal batch and the rate used
   */
  public Posted post(Voucher v) {
    if (!v.isProformaEdited()) {
      v.replaceLines(proforma.fromRule(v), false);
      BusinessEvent event = proforma.event(v, false);
      JournalBatch batch = publisher.publish(event);
      return new Posted(batch.getBatchNo(), event.exchangeRate());
    }
    BigDecimal rate = rates.rate(v.getCompanyId(), v.getCurrency(), v.getValueDate());
    JournalBatch batch = journal(v, rate, false);
    return new Posted(batch.getBatchNo(), rate);
  }

  /**
   * Posts the reversal of an approved voucher (DIS 2.20.0).
   *
   * @param v voucher (posted)
   * @return reversal batch number
   */
  public String reverse(Voucher v) {
    if (!v.isProformaEdited()) {
      return publisher.publish(proforma.event(v, true)).getBatchNo();
    }
    BigDecimal rate =
        v.getExchangeRate() != null
            ? v.getExchangeRate()
            : rates.rate(v.getCompanyId(), v.getCurrency(), v.getValueDate());
    return journal(v, rate, true).getBatchNo();
  }

  private JournalBatch journal(Voucher v, BigDecimal rate, boolean reverse) {
    if (v.getLines().isEmpty()) {
      throw new BusinessRuleException("DV_ENTRY_EMPTY", "DV " + v.getDvNo() + " has no entry");
    }
    JournalType type =
        eventTypes
            .findById(DisbursementSettings.EVENT_VOUCHER)
            .map(AccountingEventType::getJournalType)
            .orElse(JournalType.PAYMENT);
    return journals.post(
        new SystemJournalRequest(
            v.getCompanyId(),
            v.getBranchId(),
            type,
            v.getValueDate(),
            v.getCurrency(),
            (reverse ? "Cancellation of DV " : "DV ") + v.getDvNo() + " - " + v.getPayeeName(),
            v.getDvNo(),
            DisbursementSettings.MODULE,
            ProformaBuilder.sourceRef(v, reverse),
            proforma.journalLines(v, rate, reverse)));
  }

  /**
   * A posted approval.
   *
   * @param batchNo journal batch
   * @param rate exchange rate used, null for the base currency at SPOT
   */
  public record Posted(String batchNo, BigDecimal rate) {}
}
