package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.issuance.domain.AdviceTrigger;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyDispatchService.DispatchEmail;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Issuance actions on several records at once: Insurance Advice generation for several accounts
 * (BRNB.070 "single or multiple") and batch e-policy dispatch (BRNB.077). Each record runs in its
 * own transaction and reports its outcome.
 */
@Service
public class IssuanceBatchService {

  private static final int MAX_ITEMS = 200;

  private final InsuranceAdviceService advices;
  private final EpolicyDispatchService dispatch;
  private final TransactionTemplate newTransaction;

  /**
   * Creates the service.
   *
   * @param advices insurance advices
   * @param dispatch e-policy dispatch
   * @param transactionManager transaction manager
   */
  public IssuanceBatchService(
      InsuranceAdviceService advices,
      EpolicyDispatchService dispatch,
      PlatformTransactionManager transactionManager) {
    this.advices = advices;
    this.dispatch = dispatch;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Generates the Insurance Advice of several mortgaged accounts.
   *
   * @param arns accounts
   * @return outcome per account (the IA number when generated)
   */
  public List<Outcome> generateAdvices(List<String> arns) {
    return limit(arns).stream()
        .distinct()
        .map(arn -> run(arn, () -> advices.generate(arn, AdviceTrigger.MANUAL).getIaNo()))
        .toList();
  }

  /**
   * Sends several e-policies with their proposed e-mail (account contact, template, hint).
   *
   * @param epolicyIds confirmed e-policies
   * @param passwordHint password hint; the configured hint when blank
   * @return outcome per e-policy
   */
  public List<Outcome> dispatch(List<Long> epolicyIds, String passwordHint) {
    return limit(epolicyIds).stream()
        .distinct()
        .map(
            id ->
                run(
                    String.valueOf(id),
                    () -> {
                      DispatchEmail draft = dispatch.draft(id);
                      String hint =
                          passwordHint == null || passwordHint.isBlank()
                              ? draft.passwordHint()
                              : passwordHint;
                      return dispatch
                          .dispatch(
                              id,
                              new DispatchEmail(
                                  draft.to(), draft.cc(), draft.subject(), draft.body(), hint))
                          .getArn();
                    }))
        .toList();
  }

  private static <T> List<T> limit(List<T> items) {
    if (items == null || items.isEmpty() || items.size() > MAX_ITEMS) {
      throw new BusinessRuleException(
          "ISSUANCE_SELECTION", "Select between 1 and " + MAX_ITEMS + " records");
    }
    return items;
  }

  private Outcome run(String reference, Supplier<String> action) {
    try {
      return new Outcome(reference, true, newTransaction.execute(status -> action.get()));
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      return new Outcome(reference, false, e.getMessage());
    }
  }

  /**
   * Outcome for one record.
   *
   * @param reference ARN or e-policy id
   * @param ok done
   * @param message result (IA number, ARN) or reason of the refusal
   */
  public record Outcome(String reference, boolean ok, String message) {}
}
