package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService.ApplyOutcome;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Asks every {@link PaymentConfirmationSource} for the accounts awaiting payment and applies the
 * confirmed payments to the payment gate, each in its own transaction so one failing account does
 * not stop the others (BRNB.067/068, Operations BRD-2 cashiering seam).
 */
@Component
public class PaymentConfirmationSweep {

  private final List<PaymentConfirmationSource> sources;
  private final PaymentGateService gate;
  private final PlacementAccounts accounts;
  private final TransactionTemplate newTransaction;

  /**
   * Creates the sweep.
   *
   * @param sources payment confirmation sources
   * @param gate payment gate
   * @param accounts account look-ups
   * @param transactionManager transaction manager
   */
  public PaymentConfirmationSweep(
      List<PaymentConfirmationSource> sources,
      PaymentGateService gate,
      PlacementAccounts accounts,
      PlatformTransactionManager transactionManager) {
    this.sources = List.copyOf(sources);
    this.gate = gate;
    this.accounts = accounts;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Sweeps every account of a company awaiting payment.
   *
   * @param companyId company
   * @return number of gates opened
   */
  public int sweep(Long companyId) {
    List<String> arns =
        accounts.all(companyId, AccountStatus.AWAITING_PAYMENT, null).stream()
            .map(Account::getArn)
            .toList();
    return sweep(companyId, arns);
  }

  /**
   * Sweeps the given accounts.
   *
   * @param companyId company
   * @param arns accounts
   * @return number of gates opened
   */
  public int sweep(Long companyId, Collection<String> arns) {
    if (arns.isEmpty()) {
      return 0;
    }
    int opened = 0;
    for (PaymentConfirmationSource source : sources) {
      for (ConfirmedPayment payment : source.confirmedFor(companyId, arns)) {
        if (apply(source.sourceCode(), payment).opened()) {
          opened++;
        }
      }
    }
    return opened;
  }

  /**
   * Applies one confirmation in its own transaction.
   *
   * @param sourceCode source
   * @param payment confirmation
   * @return outcome; a refused transition is reported, not thrown
   */
  public ApplyOutcome apply(String sourceCode, ConfirmedPayment payment) {
    try {
      return newTransaction.execute(status -> gate.applyPayment(sourceCode, payment));
    } catch (BusinessRuleException e) {
      return new ApplyOutcome(false, e.getMessage());
    }
  }
}
