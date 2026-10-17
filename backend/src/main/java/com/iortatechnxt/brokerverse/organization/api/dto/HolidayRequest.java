package com.iortatechnxt.brokerverse.organization.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Holiday maintenance request.
 *
 * @param companyId company
 * @param branchId branch, null for company-wide
 * @param holidayDate date
 * @param description description
 */
public record HolidayRequest(
    @NotNull Long companyId,
    Long branchId,
    @NotNull LocalDate holidayDate,
    @NotBlank @Size(max = 120) String description) {}
