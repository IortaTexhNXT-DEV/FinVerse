package com.iortatechnxt.brokerverse.tax.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request to prepare a return.
 *
 * @param companyId company
 * @param formCode form
 * @param periodStart first day of the filing period
 */
public record TaxReturnRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) String formCode,
    @NotNull LocalDate periodStart) {}
