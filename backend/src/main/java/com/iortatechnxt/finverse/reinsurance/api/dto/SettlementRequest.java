package com.iortatechnxt.finverse.reinsurance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Settles an approved statement of account.
 *
 * @param settlementDate payment or receipt date
 * @param bankAccountCode GL bank account paid from or received into
 */
public record SettlementRequest(
    @NotNull LocalDate settlementDate, @NotBlank @Size(max = 20) String bankAccountCode) {}
