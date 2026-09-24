package com.iortatechnxt.brokerverse.organization.api;

import com.iortatechnxt.brokerverse.organization.api.dto.BranchRequest;
import com.iortatechnxt.brokerverse.organization.api.dto.BranchResponse;
import com.iortatechnxt.brokerverse.organization.api.dto.CompanyRequest;
import com.iortatechnxt.brokerverse.organization.api.dto.CompanyResponse;
import com.iortatechnxt.brokerverse.organization.api.dto.HolidayRequest;
import com.iortatechnxt.brokerverse.organization.api.dto.HolidayResponse;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import jakarta.validation.Valid;
import java.util.List;
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

/** REST API for companies, branches (Office Master) and holiday calendars. */
@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {

  private static final String VIEW = "hasAuthority('MASTER_VIEW')";

  /**
   * Company and branch lists feed the workspace selectors of every signed-in user (broking roles
   * have no master-data permission). They hold company and branch reference data only; changes
   * still need the master-data permissions.
   */
  private static final String WORKSPACE = "isAuthenticated()";

  private static final String MAINTAIN = "hasAuthority('MASTER_MAINTAIN')";
  private static final String AUTHORIZE = "hasAuthority('MASTER_AUTHORIZE')";

  private final OrganizationService service;

  /**
   * Creates the controller.
   *
   * @param service organization service
   */
  public OrganizationController(OrganizationService service) {
    this.service = service;
  }

  /**
   * Lists companies.
   *
   * @return companies
   */
  @GetMapping("/companies")
  @PreAuthorize(WORKSPACE)
  public List<CompanyResponse> companies() {
    return service.listCompanies().stream().map(CompanyResponse::from).toList();
  }

  /**
   * Creates a company.
   *
   * @param request request
   * @return company
   */
  @PostMapping("/companies")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public CompanyResponse createCompany(@Valid @RequestBody CompanyRequest request) {
    return CompanyResponse.from(service.createCompany(request));
  }

  /**
   * Updates a company.
   *
   * @param id id
   * @param request request
   * @return company
   */
  @PutMapping("/companies/{id}")
  @PreAuthorize(MAINTAIN)
  public CompanyResponse updateCompany(
      @PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
    return CompanyResponse.from(service.updateCompany(id, request));
  }

  /**
   * Authorizes a company.
   *
   * @param id id
   * @return company
   */
  @PostMapping("/companies/{id}/authorize")
  @PreAuthorize(AUTHORIZE)
  public CompanyResponse authorizeCompany(@PathVariable Long id) {
    return CompanyResponse.from(service.authorizeCompany(id));
  }

  /**
   * Lists branches of a company.
   *
   * @param companyId company id
   * @return branches
   */
  @GetMapping("/branches")
  @PreAuthorize(WORKSPACE)
  public List<BranchResponse> branches(@RequestParam Long companyId) {
    return service.listBranches(companyId).stream().map(BranchResponse::from).toList();
  }

  /**
   * Creates a branch.
   *
   * @param request request
   * @return branch
   */
  @PostMapping("/branches")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public BranchResponse createBranch(@Valid @RequestBody BranchRequest request) {
    return BranchResponse.from(service.createBranch(request));
  }

  /**
   * Updates a branch.
   *
   * @param id id
   * @param request request
   * @return branch
   */
  @PutMapping("/branches/{id}")
  @PreAuthorize(MAINTAIN)
  public BranchResponse updateBranch(
      @PathVariable Long id, @Valid @RequestBody BranchRequest request) {
    return BranchResponse.from(service.updateBranch(id, request));
  }

  /**
   * Authorizes a branch.
   *
   * @param id id
   * @return branch
   */
  @PostMapping("/branches/{id}/authorize")
  @PreAuthorize(AUTHORIZE)
  public BranchResponse authorizeBranch(@PathVariable Long id) {
    return BranchResponse.from(service.authorizeBranch(id));
  }

  /**
   * Deactivates a branch.
   *
   * @param id id
   * @return branch
   */
  @PostMapping("/branches/{id}/deactivate")
  @PreAuthorize(AUTHORIZE)
  public BranchResponse deactivateBranch(@PathVariable Long id) {
    return BranchResponse.from(service.deactivateBranch(id));
  }

  /**
   * Lists holidays.
   *
   * @param companyId company id
   * @param year year
   * @return holidays
   */
  @GetMapping("/holidays")
  @PreAuthorize(VIEW)
  public List<HolidayResponse> holidays(@RequestParam Long companyId, @RequestParam int year) {
    return service.listHolidays(companyId, year).stream().map(HolidayResponse::from).toList();
  }

  /**
   * Adds a holiday.
   *
   * @param request request
   * @return holiday
   */
  @PostMapping("/holidays")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public HolidayResponse addHoliday(@Valid @RequestBody HolidayRequest request) {
    return HolidayResponse.from(service.addHoliday(request));
  }
}
