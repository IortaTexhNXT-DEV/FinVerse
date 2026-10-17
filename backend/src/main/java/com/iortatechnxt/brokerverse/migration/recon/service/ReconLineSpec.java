package com.iortatechnxt.brokerverse.migration.recon.service;

import com.iortatechnxt.brokerverse.migration.recon.domain.ReconLine;

/**
 * A reconciliation line to record.
 *
 * @param level L1 to L5 or TU
 * @param measure measure
 * @param currency currency
 * @param values source, staged and target values
 * @param detail detail
 */
public record ReconLineSpec(
    String level, String measure, String currency, ReconLine.Values values, String detail) {}
