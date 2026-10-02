package com.iortatechnxt.brokerverse.organization.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.organization.api.dto.EmployeeRequest;
import com.iortatechnxt.brokerverse.organization.api.dto.EmployeeResponse;
import com.iortatechnxt.brokerverse.organization.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Employee master with cost centres (DIS 3.30.1). */
@RestController
@RequestMapping("/api/v1/organization/employees")
public class EmployeeController {

  private static final int MAX_PAGE_SIZE = 200;
  private static final String MAINTAIN = "hasAuthority('EMPLOYEE_MAINTAIN')";

  private final EmployeeService service;

  /**
   * Creates the controller.
   *
   * @param service employee service
   */
  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  /**
   * Employees of a company.
   *
   * @param companyId company
   * @param q number, name or cost-centre fragment
   * @param page page
   * @param size size
   * @return employees, active first
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('EMPLOYEE_MAINTAIN','MASTER_VIEW')")
  public PageResponse<EmployeeResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        service.search(companyId, q, PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        EmployeeResponse::from);
  }

  /**
   * Adds an employee.
   *
   * @param request employee
   * @return employee
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
    return EmployeeResponse.from(
        service.create(request.companyId(), request.employeeNo(), request.values()));
  }

  /**
   * Changes an employee.
   *
   * @param id employee
   * @param request employee
   * @return employee
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  public EmployeeResponse update(
      @PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
    return EmployeeResponse.from(service.update(id, request.values()));
  }
}
