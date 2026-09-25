package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Submission of a bulk batch to one approver with the batch remarks (BRD 1.009.1.5-6).
 *
 * @param approver chosen approver
 * @param remarks batch remarks
 */
public record AccessBatchSubmitRequest(
    @Size(max = 50) String approver, @Size(max = 1000) String remarks) {}
