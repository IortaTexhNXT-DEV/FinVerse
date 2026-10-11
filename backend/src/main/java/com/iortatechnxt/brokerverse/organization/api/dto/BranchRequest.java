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
 * @param birBranchCode BIR branch code of the registered branch (3 to 5 digits), may be null
 * @param rdoCode Revenue District Office of the branch, may be null
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
    @Pattern(regexp = "^$|^[1-7](,[1-7])*$") String weeklyHolidays,
    @Size(max = 5) @Pattern(regexp = "^$|^[0-9]{3,5}$") String birBranchCode,
    @Size(max = 5) @Pattern(regexp = "^$|^[0-9]{3}[A-Z]?$") String rdoCode) {

  /**
   * A branch without its BIR registration.
   *
   * @param companyId company
   * @param code code
   * @param name name
   * @param region region
   * @param address address
   * @param openingDate opening date
   * @param headOffice head office
   * @param forexAuthorized forex authorized
   * @param contactPhone phone
   * @param contactEmail e-mail
   * @param managerName manager
   * @param weeklyHolidays weekly holidays
   */
  @SuppressWarnings("java:S107") // mirrors the request fields
  public BranchRequest(
      Long companyId,
      String code,
      String name,
      String region,
      String address,
      LocalDate openingDate,
      boolean headOffice,
      boolean forexAuthorized,
      String contactPhone,
      String contactEmail,
      String managerName,
      String weeklyHolidays) {
    this(
        companyId,
        code,
        name,
        region,
        address,
        openingDate,
        headOffice,
        forexAuthorized,
        contactPhone,
        contactEmail,
        managerName,
        weeklyHolidays,
        null,
        null);
  }
}
