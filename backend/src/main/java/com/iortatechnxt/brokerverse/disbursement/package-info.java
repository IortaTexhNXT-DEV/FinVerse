/**
 * Disbursement (BDOI BRD-5, DIS 2.2-3.28; docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md
 * sections 5.1, 7.1-7.3): payee master, payment requests (gateway, upload, encoded), disbursement
 * vouchers with an editable proforma entry, the seven payment modes and their instrument statuses,
 * end-of-day processing, account funding, OR / AR and CWT tagging, approval posting, cancellation
 * and regularisation, stale and negotiated checks and the Disbursement reports.
 *
 * <p>It implements {@code opsledger.service.port.DisbursementGateway} with a {@code @Primary}
 * adapter that replaces the in-app queue, and publishes {@code
 * OpsLedgerEvents.DisbursementStatusChanged} (DV stage, instrument status, CANCELLED) for the
 * source modules. Nothing depends on this module. Foundation (wave A0): permissions, roles, LOV
 * types, workflows {@code DISB_VOUCHER}, {@code DISB_FUNDING}, {@code DISB_PAYEE}, {@code
 * DISB_STATUS_EDIT}, event types and parameters in V890, demo accounts, rules and users in V999.
 * The module itself is built by wave A1-DSB (V891-V893).
 */
package com.iortatechnxt.brokerverse.disbursement;
