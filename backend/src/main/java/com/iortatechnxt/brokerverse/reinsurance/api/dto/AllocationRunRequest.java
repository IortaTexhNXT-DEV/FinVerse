package com.iortatechnxt.brokerverse.reinsurance.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Period of an RI allocation run (approval dates of the premium transactions).
 *
 * @param companyId company
 * @param fromDate first approval date
 * @param toDate last approval date
 */
public record AllocationRunRequest(
    @NotNull Long companyId, @NotNull LocalDate fromDate, @NotNull LocalDate toDate) {}
