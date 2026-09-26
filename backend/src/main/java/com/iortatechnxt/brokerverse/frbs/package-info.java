/**
 * FRBS - BDOI report pack and service fee (BDOI BRD-5, FRBS 2.10, 3.2;
 * docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md sections 5.4 and 7.3): the reports that need
 * broking data (Mancom, branch production, GAP, cash flow, expense grouping) and the service-fee
 * runs with their payout and liquidation tagging.
 *
 * <p>Service-fee payouts go through {@code opsledger.service.port.DisbursementGateway} (type
 * SERVICE_FEE). Foundation (wave A0): permissions, roles, workflow {@code FRBS_SERVICE_FEE}, event
 * type {@code FRBS_SERVICE_FEE_ACCRUE} in V890. The module itself is built by wave A1-FRBS
 * (V898-V899).
 */
package com.iortatechnxt.brokerverse.frbs;
