/**
 * Package quotations (BRNB.004, 013-015, 020-024, 028, 041-045, 063, 102; MKTID.011): quotation
 * requests (manual capture with the e-mail attached, bulk upload, source-system port), quotations
 * with their ARN, versions and diff, four-eyes approval, protected send to the client, acceptance
 * and conversion into accounts through {@code AccountService.createDraft}.
 *
 * <p>Contracts for other modules: {@code QuotationQueryService.getByArn}, the port {@code
 * QuotationRequestSource}. See docs/architecture/BROKING_ARCHITECTURE.md section 11.
 */
package com.iortatechnxt.brokerverse.quotation;
