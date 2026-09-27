package com.iortatechnxt.brokerverse.organization.api.dto;

import com.iortatechnxt.brokerverse.organization.domain.EmployeeValues;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Employee create / update (DIS 3.30.1). {@code companyId} and {@code employeeNo} are ignored on
 * update.
 *
 * @param companyId company
 * @param employeeNo employee number
 * @param fullName full name
 * @param branchId branch
 * @param costCenter cost centre code
 * @param position position
 * @param email e-mail
 * @param partyCode party of the employee as a payee
 * @param hiredOn hiring date
 * @param separatedOn separation date
 */
public record EmployeeRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9\\-]+") String employeeNo,
    @NotBlank @Size(max = 120) String fullName,
    @NotNull Long branchId,
    @NotBlank @Size(max = 20) String costCenter,
    @Size(max = 80) String position,
    @Email @Size(max = 120) String email,
    @Size(max = 30) String partyCode,
    LocalDate hiredOn,
    LocalDate separatedOn) {

  /**
   * The employee details.
   *
   * @return values
   */
  public EmployeeValues values() {
    return new EmployeeValues(
        fullName.trim(),
        branchId,
        costCenter.trim(),
        blankToNull(position),
        blankToNull(email),
        blankToNull(partyCode),
        hiredOn,
        separatedOn);
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
