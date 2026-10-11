package com.iortatechnxt.brokerverse.organization.api.dto;

import com.iortatechnxt.brokerverse.organization.domain.Employee;
import java.time.LocalDate;

/**
 * Employee view.
 *
 * @param id id
 * @param employeeNo employee number
 * @param fullName full name
 * @param branchId branch
 * @param costCenter cost centre
 * @param position position
 * @param email e-mail
 * @param partyCode payee party
 * @param hiredOn hiring date
 * @param separatedOn separation date
 * @param active whether active
 */
public record EmployeeResponse(
    Long id,
    String employeeNo,
    String fullName,
    Long branchId,
    String costCenter,
    String position,
    String email,
    String partyCode,
    LocalDate hiredOn,
    LocalDate separatedOn,
    boolean active) {

  /**
   * Maps an entity.
   *
   * @param e employee
   * @return response
   */
  public static EmployeeResponse from(Employee e) {
    return new EmployeeResponse(
        e.getId(),
        e.getEmployeeNo(),
        e.getFullName(),
        e.getBranchId(),
        e.getCostCenter(),
        e.getPosition(),
        e.getEmail(),
        e.getPartyCode(),
        e.getHiredOn(),
        e.getSeparatedOn(),
        e.isActive());
  }
}
