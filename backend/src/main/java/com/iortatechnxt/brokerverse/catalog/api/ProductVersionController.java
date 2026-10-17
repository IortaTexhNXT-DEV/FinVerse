package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.ProductVersionDtos.NewVersionRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.ProductVersionDtos.ValidateRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.ProductVersionDtos.VersionContentRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.ProductVersionDtos.VersionDetail;
import com.iortatechnxt.brokerverse.catalog.api.dto.ProductVersionDtos.VersionSummary;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.version.CatalogPackageSetupService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueries;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionService;
import com.iortatechnxt.brokerverse.catalog.service.version.VersionRef;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Package versions of a product (BRPM.006/007, PMADD01/02/06; PRODUCT_MAINTENANCE_DESIGN section
 * 10): history, "New Version" drafts, the draft content, submission and the validation checkpoint,
 * and the Validation Queue.
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class ProductVersionController {

  private final ProductVersionQueries queries;
  private final ProductVersionService versions;
  private final CatalogPackageSetupService setup;
  private final ProductCatalogService catalog;

  /**
   * Creates the controller.
   *
   * @param queries version look-ups
   * @param versions checkpoint
   * @param setup draft set-up
   * @param catalog products
   */
  public ProductVersionController(
      ProductVersionQueries queries,
      ProductVersionService versions,
      CatalogPackageSetupService setup,
      ProductCatalogService catalog) {
    this.queries = queries;
    this.versions = versions;
    this.setup = setup;
    this.catalog = catalog;
  }

  /**
   * Every version of a product, newest first.
   *
   * @param code risk code
   * @return versions
   */
  @GetMapping("/products/{code}/versions")
  @PreAuthorize(CatalogAccess.READ)
  public List<VersionSummary> versions(@PathVariable String code) {
    RiskProduct product = catalog.requireProduct(code);
    return queries.entities(code).stream()
        .map(v -> VersionSummary.from(v, product.getName()))
        .toList();
  }

  /**
   * One version with its content.
   *
   * @param code risk code
   * @param versionNo version
   * @return version
   */
  @GetMapping("/products/{code}/versions/{versionNo}")
  @PreAuthorize(CatalogAccess.READ)
  public VersionDetail version(@PathVariable String code, @PathVariable int versionNo) {
    return detail(queries.require(code, versionNo));
  }

  /**
   * "New Version": a DRAFT copied from the version in force (MBS).
   *
   * @param code risk code
   * @param request company and change summary
   * @return the draft
   */
  @PostMapping("/products/{code}/versions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.PRODUCT_MAINTAIN)
  public VersionDetail newVersion(
      @PathVariable String code, @Valid @RequestBody NewVersionRequest request) {
    VersionRef ref =
        setup.draftFromCurrent(request.companyId(), code, request.changeSummary().strip());
    return detail(queries.require(code, ref.versionNo()));
  }

  /**
   * Replaces the content of a DRAFT version (MBS).
   *
   * @param code risk code
   * @param versionNo version
   * @param request content
   * @return the draft
   */
  @PutMapping("/products/{code}/versions/{versionNo}")
  @PreAuthorize(CatalogAccess.PRODUCT_MAINTAIN)
  public VersionDetail update(
      @PathVariable String code,
      @PathVariable int versionNo,
      @Valid @RequestBody VersionContentRequest request) {
    ProductVersion current = queries.require(code, versionNo);
    setup.updateDraftVersion(code, versionNo, request.spec(code, current.getSourceRequestNo()));
    return detail(queries.require(code, versionNo));
  }

  /**
   * Submits a complete draft for validation (MBS).
   *
   * @param code risk code
   * @param versionNo version
   * @return the version
   */
  @PostMapping("/products/{code}/versions/{versionNo}/submit")
  @PreAuthorize(CatalogAccess.PRODUCT_MAINTAIN)
  public VersionDetail submit(@PathVariable String code, @PathVariable int versionNo) {
    return detail(versions.submitForValidation(code, versionNo));
  }

  /**
   * Validates and releases a submitted version (never its maker or submitter, PMADD06).
   *
   * @param code risk code
   * @param versionNo version
   * @param request confirmed checklist
   * @return the version
   */
  @PostMapping("/products/{code}/versions/{versionNo}/validate")
  @PreAuthorize(CatalogAccess.VALIDATE)
  public VersionDetail validate(
      @PathVariable String code,
      @PathVariable int versionNo,
      @Valid @RequestBody ValidateRequest request) {
    return detail(versions.validate(code, versionNo, request.checklist()));
  }

  /**
   * Returns a submitted version to MBS with a reason.
   *
   * @param code risk code
   * @param versionNo version
   * @param request reason
   * @return the version
   */
  @PostMapping("/products/{code}/versions/{versionNo}/return")
  @PreAuthorize(CatalogAccess.VALIDATE)
  public VersionDetail returnToDraft(
      @PathVariable String code,
      @PathVariable int versionNo,
      @Valid @RequestBody ReasonRequest request) {
    return detail(versions.returnToDraft(code, versionNo, request.reason()));
  }

  /**
   * The versions waiting for validation, oldest first (Validation Queue).
   *
   * @return versions
   */
  @GetMapping("/validation-queue")
  @PreAuthorize(CatalogAccess.VALIDATE)
  public List<VersionSummary> validationQueue() {
    return queries.forValidation().stream()
        .map(v -> VersionSummary.from(v, catalog.requireProduct(v.getProductCode()).getName()))
        .toList();
  }

  private VersionDetail detail(ProductVersion v) {
    return VersionDetail.from(v, catalog.requireProduct(v.getProductCode()));
  }
}
