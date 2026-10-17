package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatchLine;
import com.iortatechnxt.brokerverse.cashiering.domain.LegacyBatchLineRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPosting.PostingContext;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts one line of a Cashiering legacy batch in its own transaction (DATA_MIGRATION_DESIGN 14.4 D
 * and F), so a refused line does not stop the others.
 */
@Component
public class LegacyBatchPoster {

  /** Event of an unapplied payment taken to other income. */
  public static final String UNAPPLIED_TO_INCOME = "OPS_UNAPPLIED_TO_INCOME";

  /** Closing action of an item taken to income. */
  static final String INCOME_RECLASS = "income_reclass";

  private final LegacyBatchLineRepository lines;
  private final UnappliedService unapplied;
  private final CashieringPosting posting;
  private final CwtPostings cwt;
  private final InvoiceLedgerQueryService ledger;
  private final Clock clock;

  /**
   * Creates the poster.
   *
   * @param lines batch lines
   * @param unapplied unapplied items
   * @param posting accounting events
   * @param cwt 2307 postings
   * @param ledger Operations ledger
   * @param clock clock
   */
  public LegacyBatchPoster(
      LegacyBatchLineRepository lines,
      UnappliedService unapplied,
      CashieringPosting posting,
      CwtPostings cwt,
      InvoiceLedgerQueryService ledger,
      Clock clock) {
    this.lines = lines;
    this.unapplied = unapplied;
    this.posting = posting;
    this.cwt = cwt;
    this.ledger = ledger;
    this.clock = clock;
  }

  /**
   * Posts a line.
   *
   * @param batch batch
   * @param lineId line
   * @return the journal batch
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String post(LegacyBatch batch, Long lineId) {
    LegacyBatchLine line = lines.findById(lineId).orElseThrow();
    String journal =
        batch.getKind() == LegacyBatch.Kind.INCOME_RECLASS
            ? toIncome(batch, line)
            : pr2307(batch, line);
    line.posted(journal);
    return journal;
  }

  /**
   * Records a refused line.
   *
   * @param lineId line
   * @param message reason
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void fail(Long lineId, String message) {
    lines.findById(lineId).ifPresent(l -> l.failed(message));
  }

  private String toIncome(LegacyBatch batch, LegacyBatchLine line) {
    Unapplied item = unapplied.get(line.getUnappliedId());
    if (item.getBalance().compareTo(line.getAmount()) != 0) {
      throw new BusinessRuleException(
          "CASH_BATCH_BALANCE_CHANGED",
          item.getReference() + " now has a balance of " + item.getBalance().toPlainString());
    }
    String component = item.getLegacy().ledgerContext().component(CashieringPosting.AMOUNT);
    String journal =
        posting.publish(
            new PostingContext(
                item.getCompanyId(),
                item.getBranchId(),
                BusinessClock.today(clock),
                item.getCurrency(),
                item.getReference(),
                item.getClientCode(),
                null,
                null,
                "Unapplied payment "
                    + item.getReference()
                    + " to other income, "
                    + batch.getBatchNo(),
                null),
            UNAPPLIED_TO_INCOME,
            "UIR:" + batch.getBatchNo() + ":" + item.getReference(),
            Map.of(component, line.getAmount()),
            Map.of(component, item.getClientCode() == null ? "" : item.getClientCode()));
    unapplied.close(item, INCOME_RECLASS, "Reclassified to other income in " + batch.getBatchNo());
    return journal;
  }

  private String pr2307(LegacyBatch batch, LegacyBatchLine line) {
    OpsInvoice invoice = ledger.require(line.getInvoiceNo());
    invoice.loadCollections();
    BigDecimal onPr2307 =
        invoice.component(LedgerComponent.PR2307).getBalance().max(BigDecimal.ZERO);
    String ref = "P2R:" + batch.getBatchNo() + ":" + line.getLineNo();
    String narration = "Legacy PR 2307 reversal " + batch.getBatchNo();
    if (onPr2307.compareTo(line.getAmount()) < 0) {
      cwt.reclass(invoice, line.getAmount().subtract(onPr2307), ref + ":PR", narration);
      invoice = ledger.require(line.getInvoiceNo());
      invoice.loadCollections();
    }
    return cwt.dtipOffset(invoice, line.getAmount(), ref, narration);
  }
}
