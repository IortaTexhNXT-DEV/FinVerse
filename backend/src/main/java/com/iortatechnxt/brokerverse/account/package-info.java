/**
 * Broking accounts: the risk record placed with an insurer, identified by its Account Reference
 * Number (ARN) from quotation to invoice, with risk items, duplicate fall-out, Free First Year,
 * direct payment, multi-year structure and the NB_ACCOUNT workflow actions (BRNB.019, 025, 026,
 * 029, 032, 050-054, 066, 102, 109, 111-114).
 *
 * <p>Contracts for later modules: {@code AccountService.createDraft}, {@code AccountQueryService}
 * and {@code AccountLifecycleService}; event {@code AccountStatusChanged}. See
 * docs/architecture/BROKING_ARCHITECTURE.md section 8 (account).
 */
package com.iortatechnxt.brokerverse.account;
