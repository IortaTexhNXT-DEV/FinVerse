package com.iortatechnxt.brokerverse.organization.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Create / update branch request (Office Master Maintenance).
 *
 * @param companyId owning company (ignored on update)
 * @param code branch code (ignored on update)
 * @param name branch name
 * @param region region
 * @param address address
 * @param openingDate date of opening (ignored on update)
 * @param headOffice head office flag
 * @param forexAuthorized forex authorization flag
 * @param contactPhone phone
 * @param contactEmail email
 * @param managerName branch manager
 * @param weeklyHolidays ISO weekday numbers, e.g. "6,7"
 */
public record BranchRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Z0-9]+") String code,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 60) String region,
    @Size(max = 300) String address,
    @NotNull LocalDate openingDate,
    boolean headOffice,
    boolean forexAuthorized,
    @Size(max = 40) String contactPhone,
    @Email @Size(max = 120) String contactEmail,
    @Size(max = 120) String managerName,
    @Pattern(regexp = "^$|^[1-7](,[1-7])*$") String weeklyHolidays) {}
