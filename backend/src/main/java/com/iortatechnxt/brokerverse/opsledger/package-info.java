/**
 * Operations invoice ledger (BDOI Operations BRD-2, docs/architecture/OPERATIONS_DESIGN.md section
 * 4.1): the spine every Operations module reads and writes.
 *
 * <ul>
 *   <li>One {@code ops_invoice} per booked invoice or endorsement invoice, copied from booking's
 *       {@code InvoiceBooked} event after commit (and replayable from {@code BookingQueryService}),
 *       with components and outstanding balances, insurer shares (ADJID.027), movements (RMTID.038,
 *       ADJID.024), flags, remittance / payment status (RMTID.019/032), locks (RMTID.040) and
 *       cumulative adjustments (ADJID.028).
 *   <li>Invoice 360 view (RMTID.026), Operations home counts (BRQID.003).
 *   <li>Ports the Operations modules implement or call ({@code service.port}) with default adapters
 *       ({@code service.adapter}): receipt issuing, payment re-application, unapplied items,
 *       Disbursement (in-app queue until OQ02), Collection and insurer inboxes (manual upload),
 *       shared-drive drop (in-system extract repository).
 *   <li>Flow-in framework for external systems (BRQID.004/005).
 * </ul>
 *
 * Sibling Operations modules (cashiering, remittance, prodrecon, adjustment, commission) depend on
 * this module and never on each other.
 */
package com.iortatechnxt.brokerverse.opsledger;
