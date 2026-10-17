/**
 * Refund and cash-advance requests (BDOI BRD-5, MKT 1.2-2.26;
 * docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md sections 5.2 and 7.3): Marketing refund
 * requests (RRF) with ACSL and Cashiering validation of cancelled accounts, employee cash-advance
 * requests (RFP) with HR approval, disbursed-check cancellation requests, the client payout account
 * write-back and cash-advance liquidation (if confirmed, AQ18).
 *
 * <p>Payments go through {@code opsledger.service.port.DisbursementGateway}; validations through
 * {@code opsledger.service.RefundValidations} (port {@code RefundValidationSource}). Foundation
 * (wave A0): permissions, roles, LOV types, workflows {@code PRQ_REFUND}, {@code PRQ_CASH_ADVANCE},
 * {@code PRQ_CHECK_CANCEL} in V890. The module itself is built by wave A1-PRQ (V894-V895).
 */
package com.iortatechnxt.brokerverse.payrequest;
