/**
 * Commission receivables (BDOI Operations BRD-2, CMRID.001-015, MKTID.012, RMTID.037;
 * docs/architecture/OPERATIONS_DESIGN.md sections 4.6 and 5 rows 22-26).
 *
 * <ul>
 *   <li>Direct payment (DP) lists of Head Office and branches (flow-in feed {@code
 *       COLLECTION_DP_LIST}, naming convention, submission tracker), consolidated without
 *       duplicates, validated against the Operations ledger and sanitised; the commission
 *       receivable of each account comes from the ledger (CMRID.001/002/007/008/013).
 *   <li>Commission billings per insurer (workflow {@code OPS_DP_BILLING}) sent with password
 *       protection, the insurer feedback due in {@code CMR_FEEDBACK_WORKING_DAYS} working days (job
 *       {@code DP_FEEDBACK_SLA}, alert {@code DP_FEEDBACK_OVERDUE}), answers from the screen or the
 *       feed {@code INSURER_DP_RESPONSE}, rejected accounts returned to Collection (CMRID.009-012).
 *   <li>Collection with the commission OR through the cashiering port {@code ReceiptIssuer}, then
 *       the premium receivable reversal and its reinstatement (MKTID.012, CSHID.004 b).
 *   <li>Incentive schemes No Touch, Top Up and Motor Mania with a tier editor, runs with exclusion
 *       rules, accrual and pass-on to branches through the {@code DisbursementGateway}
 *       (CMRID.003/005/006).
 *   <li>BIR certificate submissions to Comptrollership (workflow {@code OPS_BIR_CERT}, CMRID.015),
 *       estimated items (RMTID.037) and the commission reports (CMRID.004/014).
 * </ul>
 *
 * Depends on {@code opsledger} and platform / BRD-1 read services only, never on a sibling
 * Operations module.
 */
package com.iortatechnxt.brokerverse.commission;
