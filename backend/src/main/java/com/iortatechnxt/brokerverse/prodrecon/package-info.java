/**
 * Production reconciliation (BDOI Operations BRD-2, PRCID.001-039; docs/architecture/
 * OPERATIONS_DESIGN.md section 4.4).
 *
 * <ul>
 *   <li>Production register extraction per insurer and period from the Operations invoice ledger,
 *       scheduled ({@code PRODUCTION_EXTRACT}) or manual, as a locked-column workbook with a cover
 *       letter, sent by e-mail with password protection (PRCID.001-008/011).
 *   <li>Insurer upload through the flow-in feed {@code INSURER_PRODUCTION} with duplicate block and
 *       attempt history (PRCID.009/010/031/032).
 *   <li>Matching on configurable keys within {@code RECON_TOLERANCE} into the buckets matched,
 *       matched with discrepancies, BDOI only and insurer only (pre-booked / no booking), with
 *       feedback and disposition per item and the unbooked repository (PRCID.012-033).
 *   <li>Early incentive validation through the {@code EarlyIncentiveRules} seam (PRCID.028) and the
 *       reconciliation reports (PRCID.017-019/034-039).
 * </ul>
 *
 * Depends on {@code opsledger} and platform / BRD-1 read services only, never on a sibling
 * Operations module.
 */
package com.iortatechnxt.brokerverse.prodrecon;
