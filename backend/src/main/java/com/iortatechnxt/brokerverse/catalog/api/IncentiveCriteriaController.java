package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.IncentiveDtos.CriteriaRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.IncentiveDtos.CriteriaResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.IncentiveDtos.DeactivateRequest;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria;
import com.iortatechnxt.brokerverse.catalog.service.IncentiveCriteriaService;
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

/**
 * Incentive criteria on the products matrix (PMADD07/08): list with history, create, change (a
 * pending row is changed; an active row is amended by a successor), deactivate. Authorisation runs
 * through {@code /api/v1/catalog/records/INCENTIVE_CRITERIA/{id}/authorize} (PRODUCT_AUTHORIZE).
 */
@RestController
@RequestMapping("/api/v1/catalog/incentive-criteria")
public class IncentiveCriteriaController {

  private final IncentiveCriteriaService criteria;

  /**
   * Creates the controller.
   *
   * @param criteria incentive criteria
   */
  public IncentiveCriteriaController(IncentiveCriteriaService criteria) {
    this.criteria = criteria;
  }

  /**
   * Every row of a company's criteria (current and history).
   *
   * @param companyId company
   * @return rows
   */
  @GetMapping
  @PreAuthorize(CatalogAccess.READ)
  public List<CriteriaResponse> list(@RequestParam Long companyId) {
    return criteria.list(companyId).stream().map(CriteriaResponse::from).toList();
  }

  /**
   * The rows of the criterion code of a row, newest first.
   *
   * @param id any row of the criterion
   * @return rows
   */
  @GetMapping("/{id}/history")
  @PreAuthorize(CatalogAccess.READ)
  public List<CriteriaResponse> history(@PathVariable Long id) {
    IncentiveCriteria row = criteria.get(id);
    return criteria.history(row.getCompanyId(), row.getCode()).stream()
        .map(CriteriaResponse::from)
        .toList();
  }

  /**
   * Creates a criterion, pending authorisation.
   *
   * @param request criterion
   * @return row
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.INCENTIVES)
  public CriteriaResponse create(@Valid @RequestBody CriteriaRequest request) {
    return CriteriaResponse.from(
        criteria.create(request.companyId(), request.code(), request.details()));
  }

  /**
   * Changes a pending row, or amends an active one (a successor row, pending authorisation).
   *
   * @param id row
   * @param request attributes
   * @return the changed row or the successor
   */
  @PutMapping("/{id}")
  @PreAuthorize(CatalogAccess.INCENTIVES)
  public CriteriaResponse update(
      @PathVariable Long id, @Valid @RequestBody CriteriaRequest request) {
    IncentiveCriteria row = criteria.get(id);
    IncentiveCriteria result =
        row.isActive()
            ? criteria.amend(id, request.details())
            : criteria.update(id, request.details());
    return CriteriaResponse.from(result);
  }

  /**
   * Deactivates a criterion (it stays in the history).
   *
   * @param id row
   * @param request last effective day
   * @return row
   */
  @PostMapping("/{id}/deactivate")
  @PreAuthorize(CatalogAccess.INCENTIVES)
  public CriteriaResponse deactivate(
      @PathVariable Long id, @RequestBody(required = false) DeactivateRequest request) {
    return CriteriaResponse.from(
        criteria.deactivate(id, request == null ? null : request.lastDay()));
  }
}
