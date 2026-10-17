/**
 * Adjustment (BDOI Operations BRD-2, ADJID.001-026/028 and MKTID.008;
 * docs/architecture/OPERATIONS_DESIGN.md sections 4.5, 5 rows 16-21, 7, 11 and 12).
 *
 * <ul>
 *   <li>Endorsement requests (financial, non-financial, internal) raised on booked invoices of the
 *       Operations ledger, singly, for several invoices at once or by upload ({@code ADJ_BATCH});
 *       workflow {@code OPS_ENDORSEMENT} with return, validation, four-eyes approval and batch
 *       posting ({@code VB-<yyyy>}); duplicate check with override; the invoice is locked while a
 *       request is open.
 *   <li>Recompute per insurer share with the catalog calculator in endorsement mode, before / after
 *       per component and the service invoice impact (ADJID.014/027).
 *   <li>Posting through booking's {@code EndorsementPostingService} (no premium entry is written
 *       here), the ledger movement on the original invoice, payment re-application through the
 *       cashiering port {@code PaymentReapplier}, the AR Insurer set-up of remitted decreases and
 *       the {@code PENDING_NEG_ADJ} flag for remittance.
 *   <li>Endorsement slip and validation slip, minimal balance write-off / credit file, reports and
 *       the {@code ADJ_DAILY_REPORT} job.
 * </ul>
 *
 * Depends on opsledger, booking, catalog and account (plus the platform); no sibling Operations
 * module is called.
 */
package com.iortatechnxt.brokerverse.adjustment;
