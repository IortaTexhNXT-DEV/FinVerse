package com.iortatechnxt.brokerverse.receivables.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Saves (or refreshes) the reconciliation of a bank account as of a statement date.
 *
 * @param companyId company
 * @param bankAccountCode GL bank account
 * @param asOf statement date
 */
public record ReconciliationRequest(
    @NotNull Long companyId, @NotBlank String bankAccountCode, @NotNull LocalDate asOf) {}
