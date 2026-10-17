package com.iortatechnxt.brokerverse.organization.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Employee with the cost centre used for employee payments, cash advances and headcount reports
 * (DIS 3.30.1 / 3.30.2). A separated employee is inactive and kept for history.
 */
@Entity
@Table(name = "org_employee")
public class Employee extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "employee_no", nullable = false, length = 20)
  private String employeeNo;

  @Column(name = "full_name", nullable = false, length = 120)
  private String fullName;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "cost_center", nullable = false, length = 20)
  private String costCenter;

  @Column(length = 80)
  private String position;

  @Column(length = 120)
  private String email;

  @Column(name = "party_code", length = 30)
  private String partyCode;

  @Column(name = "hired_on")
  private LocalDate hiredOn;

  @Column(name = "separated_on")
  private LocalDate separatedOn;

  @Column(nullable = false)
  private boolean active = true;

  protected Employee() {}

  /**
   * Creates an employee.
   *
   * @param companyId company
   * @param employeeNo employee number
   * @param values details
   */
  public Employee(Long companyId, String employeeNo, EmployeeValues values) {
    this.companyId = companyId;
    this.employeeNo = employeeNo;
    change(values);
  }

  /**
   * Changes the details; a separation date makes the employee inactive.
   *
   * @param values details
   */
  public final void change(EmployeeValues values) {
    this.fullName = values.fullName();
    this.branchId = values.branchId();
    this.costCenter = values.costCenter();
    this.position = values.position();
    this.email = values.email();
    this.partyCode = values.partyCode();
    this.hiredOn = values.hiredOn();
    this.separatedOn = values.separatedOn();
    this.active = values.separatedOn() == null;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getEmployeeNo() {
    return employeeNo;
  }

  public String getFullName() {
    return fullName;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getPosition() {
    return position;
  }

  public String getEmail() {
    return email;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public LocalDate getHiredOn() {
    return hiredOn;
  }

  public LocalDate getSeparatedOn() {
    return separatedOn;
  }

  public boolean isActive() {
    return active;
  }
}
