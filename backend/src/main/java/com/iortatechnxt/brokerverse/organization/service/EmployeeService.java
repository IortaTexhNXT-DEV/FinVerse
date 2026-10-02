package com.iortatechnxt.brokerverse.organization.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Employee;
import com.iortatechnxt.brokerverse.organization.domain.EmployeeRepository;
import com.iortatechnxt.brokerverse.organization.domain.EmployeeValues;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Employee master (DIS 3.30.1): employee number, branch and cost centre, used by Disbursement for
 * employee payments and cash advances and by the headcount per cost centre (DIS 3.30.2, {@code
 * ORG-HEADCOUNT-CC}).
 */
@Service
@Transactional
public class EmployeeService {

  private static final String ENTITY = "Employee";

  private final EmployeeRepository employees;
  private final OrganizationService organization;
  private final DimensionService dimensions;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param employees employee repository
   * @param organization organization (branches)
   * @param dimensions dimension values (cost centres)
   * @param audit audit trail
   */
  public EmployeeService(
      EmployeeRepository employees,
      OrganizationService organization,
      DimensionService dimensions,
      AuditTrailService audit) {
    this.employees = employees;
    this.organization = organization;
    this.dimensions = dimensions;
    this.audit = audit;
  }

  /**
   * Employees matching a text.
   *
   * @param companyId company
   * @param text number, name or cost-centre fragment; null for all
   * @param pageable page
   * @return employees, active first
   */
  @Transactional(readOnly = true)
  public Page<Employee> search(Long companyId, String text, Pageable pageable) {
    String t = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    return employees.search(companyId, t, pageable);
  }

  /**
   * Active employees of a company (headcount).
   *
   * @param companyId company
   * @return employees by cost centre
   */
  @Transactional(readOnly = true)
  public List<Employee> active(Long companyId) {
    return employees.findByCompanyIdAndActiveTrueOrderByCostCenterAscFullNameAsc(companyId);
  }

  /**
   * One employee.
   *
   * @param id id
   * @return employee
   */
  @Transactional(readOnly = true)
  public Employee get(Long id) {
    return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Adds an employee.
   *
   * @param companyId company
   * @param employeeNo employee number
   * @param values details
   * @return employee
   */
  public Employee create(Long companyId, String employeeNo, EmployeeValues values) {
    String no = employeeNo.trim().toUpperCase(Locale.ROOT);
    if (employees.existsByCompanyIdAndEmployeeNo(companyId, no)) {
      throw new DuplicateResourceException(ENTITY, no);
    }
    validate(companyId, values);
    Employee saved = employees.save(new Employee(companyId, no, values));
    audit.record(
        ENTITY, no, AuditAction.CREATE, "Added " + values.fullName() + ", " + values.costCenter());
    return saved;
  }

  /**
   * Changes an employee (a separation date deactivates the employee).
   *
   * @param id employee
   * @param values details
   * @return employee
   */
  public Employee update(Long id, EmployeeValues values) {
    Employee employee = get(id);
    validate(employee.getCompanyId(), values);
    String before = employee.getCostCenter();
    employee.change(values);
    audit.record(
        ENTITY,
        employee.getEmployeeNo(),
        AuditAction.UPDATE,
        "Updated "
            + values.fullName()
            + (Objects.equals(before, values.costCenter())
                ? ""
                : ", cost centre " + before + " -> " + values.costCenter())
            + (employee.isActive() ? "" : ", separated " + values.separatedOn()));
    return employee;
  }

  private void validate(Long companyId, EmployeeValues values) {
    Branch branch = organization.requireActiveBranch(values.branchId());
    if (!branch.getCompany().getId().equals(companyId)) {
      throw new BusinessRuleException(
          "BRANCH_OF_OTHER_COMPANY", "The branch belongs to another company");
    }
    if (values.costCenter() == null || values.costCenter().isBlank()) {
      throw new BusinessRuleException("COST_CENTER_REQUIRED", "Enter the employee's cost centre");
    }
    dimensions.validateOptional(companyId, DimensionType.COST_CENTER, values.costCenter());
    requireDatesInOrder(values);
  }

  private static void requireDatesInOrder(EmployeeValues values) {
    if (values.hiredOn() != null
        && values.separatedOn() != null
        && values.separatedOn().isBefore(values.hiredOn())) {
      throw new BusinessRuleException(
          "SEPARATION_BEFORE_HIRING", "The separation date is before the hiring date");
    }
  }
}
