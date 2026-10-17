/**
 * Bulk processing framework: template download, XLSX / CSV / ODS upload, row sanitisation and
 * validation, review, partial commit (one transaction per row) and error report (BRD-1
 * BRNB.024/025/039/064). Business modules plug in a {@link
 * com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler}.
 *
 * <p>See docs/architecture/BROKING_ARCHITECTURE.md section 3.3.
 */
package com.iortatechnxt.brokerverse.bulk;
