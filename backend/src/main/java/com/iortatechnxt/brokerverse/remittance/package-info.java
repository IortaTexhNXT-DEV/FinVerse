/**
 * Remittance of collected premium to insurers (BDOI Operations BRD-2, OPERATIONS_DESIGN 4.3).
 *
 * <ul>
 *   <li>Extraction per insurer and remittance type, scheduled or manual, from the Operations
 *       invoice ledger: paid AR against the outstanding DTIP (RMTID.001-006/014/017/020/031).
 *   <li>Batches with read-only amounts, exclusions and restore (RMTID.002 addendum), the Process
 *       Remittance workflow with four-eyes approval (RMTID.007-011/019/029/036), posting and push
 *       to Disbursement (OPERATIONS_DESIGN 5 rows 12-13), the remittance schedule and payment
 *       request (RMTID.011), insurer OR upload and exception report (RMTID.012/013/016).
 *   <li>Marketing holds (RMTID.021, MKTID.002-007), special remittances (MKTID.009, RMTID.030/033),
 *       early-remittance incentives (RMTID.023), notifications (RMTID.033-035) and the Remittance
 *       reports (RMTID.039).
 * </ul>
 *
 * Depends on {@code opsledger} and platform modules only; never on the sibling Operations modules.
 */
package com.iortatechnxt.brokerverse.remittance;
