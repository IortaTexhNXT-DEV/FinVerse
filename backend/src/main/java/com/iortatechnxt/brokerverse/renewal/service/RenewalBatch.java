package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs an action on each selected renewal in its own transaction: a business refusal of one renewal
 * is reported with its reason and the others go on.
 */
@Component
public class RenewalBatch {

  private final TransactionTemplate tx;

  /**
   * Creates the runner.
   *
   * @param txManager transactions
   */
  public RenewalBatch(PlatformTransactionManager txManager) {
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Runs the action on each renewal.
   *
   * @param refs renewal references (at least one)
   * @param action action on one renewal reference
   * @return done and refused renewals
   */
  public BatchOutcome run(List<String> refs, Consumer<String> action) {
    if (refs == null || refs.isEmpty()) {
      throw new BusinessRuleException("RNW_SELECTION_EMPTY", "Select at least one account");
    }
    BatchOutcome.Builder outcome = new BatchOutcome.Builder();
    for (String ref : refs.stream().distinct().toList()) {
      try {
        tx.executeWithoutResult(s -> action.accept(ref));
        outcome.done(ref);
      } catch (BusinessRuleException | ResourceNotFoundException e) {
        outcome.refused(ref, e.getMessage());
      }
    }
    return outcome.build();
  }
}
