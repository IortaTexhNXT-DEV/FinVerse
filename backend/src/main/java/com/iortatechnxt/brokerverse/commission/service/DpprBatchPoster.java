package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.commission.domain.DpprBatch;
import com.iortatechnxt.brokerverse.commission.domain.DpprBatchLine;
import com.iortatechnxt.brokerverse.commission.domain.DpprBatchLineRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts one line of a legacy direct payment PR reversal batch in its own transaction
 * (DATA_MIGRATION_DESIGN 14.4 E), so a refused line does not stop the others.
 */
@Component
public class DpprBatchPoster {

  private final DpprBatchLineRepository lines;
  private final DpprOpenPremium open;
  private final DpPostings postings;
  private final Clock clock;

  /**
   * Creates the poster.
   *
   * @param lines batch lines
   * @param open open premium receivable of an invoice
   * @param postings direct payment postings
   * @param clock clock
   */
  public DpprBatchPoster(
      DpprBatchLineRepository lines, DpprOpenPremium open, DpPostings postings, Clock clock) {
    this.lines = lines;
    this.open = open;
    this.postings = postings;
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
  public String post(DpprBatch batch, Long lineId) {
    DpprBatchLine line = lines.findById(lineId).orElseThrow();
    java.math.BigDecimal now = open.of(line.getInvoiceNo());
    if (now.compareTo(line.getAmount()) != 0) {
      throw new BusinessRuleException(
          "CMR_BATCH_BALANCE_CHANGED",
          line.getInvoiceNo() + " now has an open premium receivable of " + now.toPlainString());
    }
    String journal =
        postings.reverseLegacyPremium(
            line.getInvoiceNo(),
            "DPPR:" + batch.getBatchNo() + ":" + line.getLineNo(),
            BusinessClock.today(clock));
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
}
