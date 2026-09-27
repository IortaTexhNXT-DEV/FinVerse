package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
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

  /**
   * Runs an action that reports its own refusal and keeps what it recorded (for instance a letter
   * refused by the recipient policy keeps the reason).
   *
   * @param refs renewal references (at least one)
   * @param action action on one renewal reference; returns the refusal, null when done
   * @return done and refused renewals
   */
  public BatchOutcome runReporting(List<String> refs, Function<String, String> action) {
    if (refs == null || refs.isEmpty()) {
      throw new BusinessRuleException("RNW_SELECTION_EMPTY", "Select at least one account");
    }
    BatchOutcome.Builder outcome = new BatchOutcome.Builder();
    for (String ref : refs.stream().distinct().toList()) {
      try {
        String refusal = tx.execute(s -> action.apply(ref));
        if (refusal == null) {
          outcome.done(ref);
        } else {
          outcome.refused(ref, refusal);
        }
      } catch (BusinessRuleException | ResourceNotFoundException e) {
        outcome.refused(ref, e.getMessage());
      }
    }
    return outcome.build();
  }
}
