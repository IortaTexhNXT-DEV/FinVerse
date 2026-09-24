package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Creates a CLPC billing batch (BRNB.067).
 *
 * @param companyId company
 * @param arns accounts to bill; empty to bill every candidate
 */
public record BillingBatchRequest(@NotNull Long companyId, @Size(max = 2000) List<String> arns) {}
