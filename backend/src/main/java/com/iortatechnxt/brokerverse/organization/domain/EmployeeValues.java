package com.iortatechnxt.brokerverse.organization.domain;

import java.time.LocalDate;

/**
 * Employee details (DIS 3.30.1).
 *
 * @param fullName full name
 * @param branchId branch
 * @param costCenter cost centre code (dimension COST_CENTER)
 * @param position position
 * @param email e-mail
 * @param partyCode party of the employee as a payee (party type EMPLOYEE), may be null
 * @param hiredOn hiring date
 * @param separatedOn separation date (makes the employee inactive)
 */
public record EmployeeValues(
    String fullName,
    Long branchId,
    String costCenter,
    String position,
    String email,
    String partyCode,
    LocalDate hiredOn,
    LocalDate separatedOn) {}
