/**
 * ACSL - Accounting Control and Sub-Ledger (BDOI BRD-5, ACSL 2.2-2.16, 2.9.1-2.9.2;
 * docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md sections 5.3 and 7.3): insurer SOA upload and
 * per-invoice reconciliation, GL-SL reconciliation, investigation and analysis cases, AR refund
 * application and sub-ledger payment reversal requests, correction entries (assign, create, review,
 * approve, post) and the ACSL reports.
 *
 * <p>It asks cashiering for reversals through {@code
 * opsledger.service.port.PaymentReversalRequester}, records corrections on the invoice ledger
 * through {@code InvoiceCorrectionSink} and answers the ACSL refund validations as a {@code
 * RefundValidationSource}. Foundation (wave A0): permissions, roles, LOV types, workflows {@code
 * ACSL_CASE}, {@code ACSL_CORRECTION}, {@code REM_DEDUCTION}, event type {@code ACSL_CORRECTION} in
 * V890. The module itself is built by wave A1-PRQ (V896-V897).
 */
package com.iortatechnxt.brokerverse.acsl;
