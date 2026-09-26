package com.iortatechnxt.brokerverse.catalog.api.dto;

import java.util.List;

/**
 * An insurer with its branches (LGT) and commission rates.
 *
 * @param insurer profile
 * @param branches branches
 * @param commissions commission rates
 */
public record InsurerDetailResponse(
    InsurerResponse insurer, List<BranchResponse> branches, List<CommissionResponse> commissions) {}
