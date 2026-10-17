/**
 * Cashiering (BDOI Operations BRD-2, docs/architecture/OPERATIONS_DESIGN.md section 4.2): receipt
 * series, acknowledgement receipts (AR) and Head Office official receipts (OR) with cancellation
 * and reinstatement (CSHID.001-006/011-015), payment intake from files, over the counter, the PDC
 * warehouse and check pick-up (CSHID.008/009), matching and application by premium component
 * (CSHID.020/022), pre-booked payments, unapplied payments and their dispositions (CSHID.024/025),
 * minimal balances (CSHID.016), AR Insurance (CSHID.021), BIR 2307 tagging, validation and routing
 * (CSHID.026/027, MKTID.010/013, DBMID.001), batch printing (CSHID.019) and the Cashiering reports
 * (CSHID.023).
 *
 * <p>The module depends on {@code opsledger} (invoice ledger, ports, events) and on the platform
 * and BRD-1 read contracts only. It implements the ledger ports {@code ReceiptIssuer}, {@code
 * UnappliedSink} and {@code PaymentReapplier}, and placement's {@code PaymentConfirmationSource}.
 */
package com.iortatechnxt.brokerverse.cashiering;
