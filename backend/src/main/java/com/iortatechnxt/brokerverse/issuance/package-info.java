/**
 * Issuance: e-policy receipt matched to the account (BRNB.073), policy data extraction with a
 * review before the policy number update (BRNB.074/104/112), document trigger rules (BRNB.105),
 * Insurance Advice for mortgaged accounts (BRNB.060/070/095) and the encrypted e-policy dispatch
 * with its report (BRNB.035/077).
 *
 * <p>The policy number reaches the account through {@code AccountLifecycleService.recordPolicy}.
 * Contracts for other modules: {@code IssuanceQueryService.policyFor(arn)} and the port {@code
 * PolicyDataExtractor}. See docs/architecture/BROKING_ARCHITECTURE.md (issuance).
 */
package com.iortatechnxt.brokerverse.issuance;
