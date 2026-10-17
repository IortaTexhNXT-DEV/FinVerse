/**
 * Product Maintenance (BRD-3) package request process (BRPM.005, BRPM.008-019, BRPM.021/022,
 * PMADD03/04): the Package Request Form and its approvals (Marketing, TSU Team Lead, TSU Head),
 * insurer negotiation in numbered rounds with a quotation slip per round, insurer responses with
 * exception outcomes, the master and client comparative outputs, the requirements pack and ManCom
 * sign-off, the MBS set-up through the catalog {@code PackageSetupService}, the release follow-up
 * on the catalog events, advisories, the package expiry monitor with renewal requests, the package
 * reports and the Product Maintenance home.
 *
 * <p>Master data (versions, coverages, insurer terms) stays in {@code catalog}; this module only
 * uses its published contracts ({@code catalog.service.version}). See
 * docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md (sections 4.4, 7, 8, 10, 11 and "P1-B: as
 * built").
 */
package com.iortatechnxt.brokerverse.productmaint;
