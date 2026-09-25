package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.OpsInvoiceBooked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Automatch upon booking (PRCID.024): when an invoice enters the Operations ledger, insurer
 * production of an open cycle waiting for it (same reference or policy number) is paired with it,
 * in a transaction of its own after the ledger committed. A failure is logged and never reaches the
 * ledger feed; the {@code RECON_AUTOMATCH} job pairs the line later.
 */
@Component
public class ReconBookingListener {

  private static final Logger LOG = LoggerFactory.getLogger(ReconBookingListener.class);

  private final ReconMatchingService matching;
  private final TransactionTemplate tx;

  /**
   * Creates the listener.
   *
   * @param matching matching engine
   * @param txManager transaction manager
   */
  public ReconBookingListener(ReconMatchingService matching, PlatformTransactionManager txManager) {
    this.matching = matching;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Pairs waiting insurer production with a new invoice.
   *
   * @param event invoice copied into the ledger
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(OpsInvoiceBooked event) {
    try {
      Integer paired =
          tx.execute(
              s ->
                  matching.invoiceBooked(
                      event.companyId(), event.insurerCode(), event.invoiceNo(), event.policyNo()));
      if (paired != null && paired > 0) {
        LOG.info("Invoice {} paired with {} insurer line(s)", event.invoiceNo(), paired);
      }
    } catch (RuntimeException ex) {
      LOG.warn("Automatch of invoice {} skipped: {}", event.invoiceNo(), ex.getMessage());
    }
  }
}
