package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs each item of a cashiering batch job in its own transaction (BRQID.006: the job does not stop
 * on a failed record): a refused item (locked invoice, closed period, missing rule) is logged and
 * skipped, the others are committed.
 */
@Component
public class ItemTransactions {

  private static final Logger LOG = LoggerFactory.getLogger(ItemTransactions.class);

  private final TransactionTemplate template;

  /**
   * Creates the helper.
   *
   * @param transactions transaction manager
   */
  public ItemTransactions(PlatformTransactionManager transactions) {
    this.template = new TransactionTemplate(transactions);
    this.template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Runs one item.
   *
   * @param what item description for the log
   * @param work the work, true when it did something
   * @return the work's result, false when refused
   */
  public boolean run(String what, BooleanSupplier work) {
    try {
      return Boolean.TRUE.equals(template.execute(status -> work.getAsBoolean()));
    } catch (BusinessRuleException ex) {
      LOG.warn("Cashiering item {} skipped: {} {}", what, ex.getCode(), ex.getMessage());
      return false;
    } catch (RuntimeException ex) {
      LOG.error("Cashiering item {} failed", what, ex);
      return false;
    }
  }
}
