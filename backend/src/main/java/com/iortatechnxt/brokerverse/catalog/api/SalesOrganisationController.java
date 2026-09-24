package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.SalesOfficerRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.SalesOfficerResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.SalesOrganisationResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.SalesUnitRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.SalesUnitResponse;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService.SalesAssignment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

/** Sales organisation: regions, departments, teams, cost centers, account officers (BRNB.075). */
@RestController
@RequestMapping("/api/v1/catalog/sales-organisation")
public class SalesOrganisationController {

  private final SalesOrganisationService sales;

  /**
   * Creates the controller.
   *
   * @param sales sales organisation
   */
  public SalesOrganisationController(SalesOrganisationService sales) {
    this.sales = sales;
  }

  /**
   * Units and officers of a company.
   *
   * @param companyId company
   * @return organisation
   */
  @GetMapping
  @PreAuthorize(CatalogAccess.READ)
  public SalesOrganisationResponse organisation(@RequestParam Long companyId) {
    return new SalesOrganisationResponse(
        sales.units(companyId).stream().map(SalesUnitResponse::from).toList(),
        sales.officers(companyId).stream().map(SalesOfficerResponse::from).toList());
  }

  /**
   * Sales unit and cost center of an account officer (account defaults).
   *
   * @param companyId company
   * @param username account officer
   * @return assignment, or 204 when the user is in no team
   */
  @GetMapping("/assignment")
  @PreAuthorize(CatalogAccess.READ)
  public ResponseEntity<SalesAssignment> assignment(
      @RequestParam Long companyId, @RequestParam String username) {
    return sales
        .assignmentOf(companyId, username)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * Adds a unit.
   *
   * @param request unit
   * @return unit
   */
  @PostMapping("/units")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public SalesUnitResponse createUnit(@Valid @RequestBody SalesUnitRequest request) {
    return SalesUnitResponse.from(
        sales.createUnit(request.companyId(), request.level(), request.code(), request.details()));
  }

  /**
   * Changes a unit.
   *
   * @param id unit
   * @param request unit
   * @return unit
   */
  @PutMapping("/units/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public SalesUnitResponse updateUnit(
      @PathVariable Long id, @Valid @RequestBody SalesUnitRequest request) {
    return SalesUnitResponse.from(sales.updateUnit(id, request.details()));
  }

  /**
   * Places a user in a team.
   *
   * @param request team and user
   * @return officer
   */
  @PostMapping("/officers")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public SalesOfficerResponse assignOfficer(@Valid @RequestBody SalesOfficerRequest request) {
    return SalesOfficerResponse.from(
        sales.assignOfficer(request.companyId(), request.teamCode(), request.username()));
  }
}
