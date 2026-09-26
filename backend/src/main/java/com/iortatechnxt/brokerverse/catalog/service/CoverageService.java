package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.Clause;
import com.iortatechnxt.brokerverse.catalog.domain.Clause.ClauseDetails;
import com.iortatechnxt.brokerverse.catalog.domain.ClauseRepository;
import com.iortatechnxt.brokerverse.catalog.domain.Coverage;
import com.iortatechnxt.brokerverse.catalog.domain.Coverage.CoverageDetails;
import com.iortatechnxt.brokerverse.catalog.domain.CoverageRepository;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coverage / peril master and clause library (PMADD01/02; PRODUCT_MAINTENANCE_DESIGN section 4.1):
 * the lowest level of the product hierarchy and the warranties, clauses, exclusions and deductible
 * wordings that insurer terms reference. Changes are maker-checker (authorised through {@link
 * CatalogRecords} with PRODUCT_AUTHORIZE) and audited.
 */
@Service
@Transactional
public class CoverageService {

  private static final String COVERAGE_KIND = "COVERAGE_KIND";
  private static final String CLAUSE_KIND = "CLAUSE_KIND";

  private final CoverageRepository coverages;
  private final ClauseRepository clauses;
  private final ProductCatalogService catalog;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param coverages coverages
   * @param clauses clauses
   * @param catalog product lines
   * @param lovs lists of values (kinds)
   * @param audit audit trail
   * @param clock clock
   */
  public CoverageService(
      CoverageRepository coverages,
      ClauseRepository clauses,
      ProductCatalogService catalog,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.coverages = coverages;
    this.clauses = clauses;
    this.catalog = catalog;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Coverages of one line, or of every line.
   *
   * @param lineCode line, null for all
   * @return coverages in order
   */
  @Transactional(readOnly = true)
  public List<Coverage> coverages(String lineCode) {
    return lineCode == null
        ? coverages.findAllByOrderByLineCodeAscSortOrderAscCodeAsc()
        : coverages.findByLineCodeOrderBySortOrderAscCodeAsc(lineCode);
  }

  /**
   * A coverage of a line.
   *
   * @param lineCode line
   * @param code code
   * @return coverage
   */
  @Transactional(readOnly = true)
  public Coverage requireCoverage(String lineCode, String code) {
    return coverages
        .findByLineCodeAndCode(lineCode, code)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(CatalogKind.COVERAGE.label(), lineCode + "/" + code));
  }

  /**
   * Adds a coverage, pending authorization.
   *
   * @param lineCode line
   * @param code code
   * @param details attributes
   * @return coverage
   */
  public Coverage createCoverage(String lineCode, String code, CoverageDetails details) {
    catalog.requireLine(lineCode);
    if (coverages.findByLineCodeAndCode(lineCode, code).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.COVERAGE.label(), lineCode + "/" + code);
    }
    lovs.requireValid(COVERAGE_KIND, details.kind(), today());
    Coverage saved = coverages.save(new Coverage(lineCode, code, details));
    audit.record(
        CatalogKind.COVERAGE.label(),
        saved.catalogReference(),
        AuditAction.CREATE,
        "Added " + details.name());
    return saved;
  }

  /**
   * Changes a coverage, pending authorization.
   *
   * @param id coverage
   * @param details attributes
   * @return coverage
   */
  public Coverage updateCoverage(Long id, CoverageDetails details) {
    Coverage coverage =
        coverages
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.COVERAGE.label(), id));
    lovs.requireValid(COVERAGE_KIND, details.kind(), today());
    coverage.update(details);
    audit.record(
        CatalogKind.COVERAGE.label(),
        coverage.catalogReference(),
        AuditAction.UPDATE,
        "Changed " + details.name());
    return coverage;
  }

  /**
   * Every clause of the library.
   *
   * @return clauses by code
   */
  @Transactional(readOnly = true)
  public List<Clause> clauses() {
    return clauses.findAllByOrderByCodeAsc();
  }

  /**
   * A clause by code.
   *
   * @param code code
   * @return clause
   */
  @Transactional(readOnly = true)
  public Clause requireClause(String code) {
    return clauses
        .findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.CLAUSE.label(), code));
  }

  /**
   * Adds a clause, pending authorization.
   *
   * @param code code
   * @param details attributes
   * @return clause
   */
  public Clause createClause(String code, ClauseDetails details) {
    if (clauses.findByCode(code).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.CLAUSE.label(), code);
    }
    validate(details);
    Clause saved = clauses.save(new Clause(code, details));
    audit.record(CatalogKind.CLAUSE.label(), code, AuditAction.CREATE, "Added " + details.title());
    return saved;
  }

  /**
   * Changes a clause, pending authorization.
   *
   * @param id clause
   * @param details attributes
   * @return clause
   */
  public Clause updateClause(Long id, ClauseDetails details) {
    Clause clause =
        clauses
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.CLAUSE.label(), id));
    validate(details);
    clause.update(details);
    audit.record(
        CatalogKind.CLAUSE.label(),
        clause.getCode(),
        AuditAction.UPDATE,
        "Changed " + details.title());
    return clause;
  }

  private void validate(ClauseDetails details) {
    lovs.requireValid(CLAUSE_KIND, details.kind(), today());
    if (details.lineCode() != null) {
      catalog.requireLine(details.lineCode());
    }
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }
}
