package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.CoverageDtos.ClauseRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.CoverageDtos.ClauseResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.CoverageDtos.CoverageRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.CoverageDtos.CoverageResponse;
import com.iortatechnxt.brokerverse.catalog.service.CoverageService;
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
 * Coverage / peril master and clause library (PMADD01/02). Authorisation and deactivation run
 * through {@code /api/v1/catalog/records/{COVERAGE|CLAUSE}/{id}}.
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class CoverageController {

  private final CoverageService coverages;

  /**
   * Creates the controller.
   *
   * @param coverages coverages and clauses
   */
  public CoverageController(CoverageService coverages) {
    this.coverages = coverages;
  }

  /**
   * Coverages of a line, or of every line.
   *
   * @param line product line
   * @return coverages
   */
  @GetMapping("/coverages")
  @PreAuthorize(CatalogAccess.READ)
  public List<CoverageResponse> coverages(@RequestParam(required = false) String line) {
    return coverages.coverages(line).stream().map(CoverageResponse::from).toList();
  }

  /**
   * Adds a coverage.
   *
   * @param request coverage
   * @return coverage
   */
  @PostMapping("/coverages")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.PRODUCT_MAINTAIN)
  public CoverageResponse createCoverage(@Valid @RequestBody CoverageRequest request) {
    return CoverageResponse.from(
        coverages.createCoverage(request.lineCode(), request.code(), request.details()));
  }

  /**
   * Changes a coverage.
   *
   * @param id coverage
   * @param request attributes
   * @return coverage
   */
  @PutMapping("/coverages/{id}")
  @PreAuthorize(CatalogAccess.PRODUCT_MAINTAIN)
  public CoverageResponse updateCoverage(
      @PathVariable Long id, @Valid @RequestBody CoverageRequest request) {
    return CoverageResponse.from(coverages.updateCoverage(id, request.details()));
  }

  /**
   * The clause library.
   *
   * @return clauses
   */
  @GetMapping("/clauses")
  @PreAuthorize(CatalogAccess.READ)
  public List<ClauseResponse> clauses() {
    return coverages.clauses().stream().map(ClauseResponse::from).toList();
  }

  /**
   * Adds a clause.
   *
   * @param request clause
   * @return clause
   */
  @PostMapping("/clauses")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.PRODUCT_MAINTAIN)
  public ClauseResponse createClause(@Valid @RequestBody ClauseRequest request) {
    return ClauseResponse.from(coverages.createClause(request.code(), request.details()));
  }

  /**
   * Changes a clause.
   *
   * @param id clause
   * @param request attributes
   * @return clause
   */
  @PutMapping("/clauses/{id}")
  @PreAuthorize(CatalogAccess.PRODUCT_MAINTAIN)
  public ClauseResponse updateClause(
      @PathVariable Long id, @Valid @RequestBody ClauseRequest request) {
    return ClauseResponse.from(coverages.updateClause(id, request.details()));
  }
}
