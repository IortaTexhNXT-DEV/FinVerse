package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.placement.domain.PaymentReport;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService.ApplyOutcome;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Confirms a payment report (BRNB.067/068): the report is confirmed and the CLPC outcome written to
 * the billing batch, then the payment gate of each matched, paid account opens in its own
 * transaction, so an account whose status changed meanwhile is reported on its line without
 * stopping the others.
 */
@Component
public class PaymentReportConfirmation {

  private final PaymentReportService reports;
  private final PaymentConfirmationSweep sweep;
  private final TransactionTemplate newTransaction;

  /**
   * Creates the orchestrator.
   *
   * @param reports payment reports
   * @param sweep payment gate application
   * @param transactionManager transaction manager
   */
  public PaymentReportConfirmation(
      PaymentReportService reports,
      PaymentConfirmationSweep sweep,
      PlatformTransactionManager transactionManager) {
    this.reports = reports;
    this.sweep = sweep;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Confirms a report and opens the gates of its matched, paid accounts.
   *
   * @param reportId report
   * @return the report with the outcome of each line
   */
  public PaymentReport confirm(Long reportId) {
    List<Long> lines = newTransaction.execute(status -> reports.markConfirmed(reportId));
    for (Long lineId : lines == null ? List.<Long>of() : lines) {
      ConfirmedPayment payment = reports.confirmationOf(reportId, lineId);
      ApplyOutcome outcome = sweep.apply(ReportPaymentSource.SOURCE, payment);
      newTransaction.executeWithoutResult(
          status -> reports.markApplied(reportId, lineId, outcome.opened(), outcome.message()));
    }
    return reports.get(reportId);
  }
}
