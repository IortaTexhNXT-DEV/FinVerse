package com.iortatechnxt.brokerverse.placement.service;

import java.util.Collection;
import java.util.List;

/**
 * Port through which payment confirmations reach the payment gate (BRD 2.3.1, BRNB.067/068,
 * Operations BRD-2 cashiering).
 *
 * <p>Placement is not a payment intake: it records no receipts and no cash entries, only the gate
 * decision and the evidence behind it. Every source of confirmed payments implements this port as a
 * Spring bean in its own module:
 *
 * <ul>
 *   <li>today, {@link ReportPaymentSource}: lines of confirmed CLPC and payment reports matched to
 *       accounts (upload and review on the CLPC Billing screen);
 *   <li>tomorrow, the Operations Cashiering module: receipt applications confirmed against an
 *       account (by ARN, policy or PN number, see {@code AccountQueryService.preBooked}).
 * </ul>
 *
 * <p>{@link PaymentConfirmationSweep} asks every source for the accounts awaiting payment (job
 * {@code PAYMENT_CONFIRMATION_SWEEP}, and immediately after a payment report is confirmed). Each
 * confirmation is applied once per source and reference: the evidence is recorded and, when the
 * account's gate rule is satisfied, {@code AccountLifecycleService.markPaymentConfirmed} moves the
 * account to Ready for placement.
 */
public interface PaymentConfirmationSource {

  /**
   * Code of the source, stored with the evidence (e.g. {@code PAYMENT_REPORT}, {@code CASHIERING}).
   *
   * @return source code, at most 30 characters
   */
  String sourceCode();

  /**
   * Confirmed payments of the given accounts. A source may return a payment again on later calls;
   * the gate applies a (source, reference) pair only once.
   *
   * @param companyId company
   * @param arns Account Reference Numbers of the accounts awaiting payment
   * @return confirmed payments, possibly empty
   */
  List<ConfirmedPayment> confirmedFor(Long companyId, Collection<String> arns);
}
