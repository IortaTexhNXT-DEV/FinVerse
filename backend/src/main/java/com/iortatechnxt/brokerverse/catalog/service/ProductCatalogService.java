package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.CoverType;
import com.iortatechnxt.brokerverse.catalog.domain.CoverTypeRepository;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLifecycle;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine.LineDetails;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLineRepository;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product master (BRNB.001): product lines and cover types of Annex II and the BDOI risk products
 * with their workflow attributes. Changes are maker-checker; authorization is done by {@link
 * CatalogRecords}. Product Maintenance (BRD-3) adds the hierarchy rules (PMADD01: cover type
 * mandatory for packages, subtypes, risk-code pattern per line), the product lifecycle and the
 * sellability check of new business (BRPM.006/007): the commercial columns of a versioned package
 * are owned by its versions ({@code PRODUCT_FIELD_VERSIONED}).
 */
@Service
@Transactional
public class ProductCatalogService {

  private static final String CODE = "code";
  private static final String NOT_SELLABLE = "PRODUCT_NOT_SELLABLE";

  private final ProductLineRepository lines;
  private final CoverTypeRepository coverTypes;
  private final RiskProductRepository products;
  private final ProductVersionRepository versions;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lines product lines
   * @param coverTypes cover types
   * @param products risk products
   * @param versions package versions
   * @param lovs lists of values (market segments)
   * @param audit audit trail
   * @param clock clock
   */
  public ProductCatalogService(
      ProductLineRepository lines,
      CoverTypeRepository coverTypes,
      RiskProductRepository products,
      ProductVersionRepository versions,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.lines = lines;
    this.coverTypes = coverTypes;
    this.products = products;
    this.versions = versions;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Every product line in display order.
   *
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<ProductLine> lines() {
    return lines.findAllByOrderBySortOrderAscNameAsc();
  }

  /**
   * A product line.
   *
   * @param code line code
   * @return line
   */
  @Transactional(readOnly = true)
  public ProductLine requireLine(String code) {
    return lines
        .findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.PRODUCT_LINE.label(), code));
  }

  /**
   * Adds a product line, pending authorization.
   *
   * @param code code
   * @param details attributes
   * @return line
   */
  public ProductLine createLine(String code, LineDetails details) {
    if (lines.findByCode(code).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.PRODUCT_LINE.label(), code);
    }
    ProductLine saved = lines.save(new ProductLine(code, details));
    record(CatalogKind.PRODUCT_LINE, code, AuditAction.CREATE, "Added " + details.name());
    return saved;
  }

  /**
   * Changes a product line, pending authorization.
   *
   * @param code code
   * @param details attributes
   * @return line
   */
  public ProductLine updateLine(String code, LineDetails details) {
    ProductLine line = requireLine(code);
    line.update(details);
    record(CatalogKind.PRODUCT_LINE, code, AuditAction.UPDATE, "Changed " + details.name());
    return line;
  }

  /**
   * Every cover type.
   *
   * @return cover types by line
   */
  @Transactional(readOnly = true)
  public List<CoverType> coverTypes() {
    return coverTypes.findAllByOrderByLineCodeAscSortOrderAscNameAsc();
  }

  /**
   * Adds a cover type to a line, pending authorization.
   *
   * @param lineCode line
   * @param code code
   * @param name name
   * @param sortOrder order
   * @return cover type
   */
  public CoverType createCoverType(String lineCode, String code, String name, int sortOrder) {
    return createCoverType(lineCode, code, name, sortOrder, null);
  }

  /**
   * Adds a cover type, or a subtype under a top-level cover type of the same line (PMADD01; the
   * hierarchy is at most two levels deep), pending authorization.
   *
   * @param lineCode line
   * @param code code
   * @param name name
   * @param sortOrder order
   * @param parentCode parent cover type, null for a top-level type
   * @return cover type
   */
  public CoverType createCoverType(
      String lineCode, String code, String name, int sortOrder, String parentCode) {
    requireLine(lineCode);
    if (coverTypes.findByLineCodeAndCode(lineCode, code).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.COVER_TYPE.label(), lineCode + "/" + code);
    }
    CoverType parent =
        parentCode == null
            ? null
            : coverTypes
                .findByLineCodeAndCode(lineCode, parentCode)
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            CatalogKind.COVER_TYPE.label(), lineCode + "/" + parentCode));
    CoverType saved = coverTypes.save(new CoverType(lineCode, code, name, sortOrder, parent));
    record(CatalogKind.COVER_TYPE, saved.catalogReference(), AuditAction.CREATE, "Added " + name);
    return saved;
  }

  /**
   * Renames a cover type, pending authorization.
   *
   * @param id cover type
   * @param name name
   * @param sortOrder order
   * @return cover type
   */
  public CoverType updateCoverType(Long id, String name, int sortOrder) {
    CoverType type =
        coverTypes
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.COVER_TYPE.label(), id));
    type.update(name, sortOrder);
    record(CatalogKind.COVER_TYPE, type.catalogReference(), AuditAction.UPDATE, "Renamed " + name);
    return type;
  }

  /**
   * Products matching the filters, by risk code.
   *
   * @param filter line, package flag, segment, status and text
   * @return products
   */
  @Transactional(readOnly = true)
  public List<RiskProduct> products(ProductFilter filter) {
    return products.findAll(ProductSpecifications.of(filter), Sort.by(CODE));
  }

  /**
   * A product by risk code, whatever its status.
   *
   * @param code risk code
   * @return product
   */
  @Transactional(readOnly = true)
  public RiskProduct requireProduct(String code) {
    return products
        .findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException(CatalogKind.PRODUCT.label(), code));
  }

  /**
   * A product that may be used for new business: authorized and active.
   *
   * @param code risk code
   * @return product
   */
  @Transactional(readOnly = true)
  public RiskProduct requireUsableProduct(String code) {
    RiskProduct product = requireProduct(code);
    if (!product.isActive()) {
      throw new BusinessRuleException(
          "PRODUCT_NOT_ACTIVE", "Product " + code + " is not authorized or is inactive");
    }
    return product;
  }

  /**
   * A product that may be sold for a purpose (BRPM.006/007; PRODUCT_MAINTENANCE_DESIGN section
   * 9.1): authorized and active; for new business also not expired or retired and, for a versioned
   * package, with a released version in force on the date. Renewals and endorsements of an expired
   * package stay possible (its versions stay readable).
   *
   * @param code risk code
   * @param purpose why the product is used
   * @param date transaction date
   * @return product
   */
  @Transactional(readOnly = true)
  public RiskProduct requireSellable(String code, RatingQuery.Purpose purpose, LocalDate date) {
    RiskProduct product = requireUsableProduct(code);
    if (purpose != RatingQuery.Purpose.NEW_BUSINESS) {
      return product;
    }
    if (product.getLifecycleStatus() != ProductLifecycle.ACTIVE) {
      throw new BusinessRuleException(
          NOT_SELLABLE,
          "Product " + code + " is " + product.getLifecycleStatus() + " and cannot be sold");
    }
    if (product.isPackaged()
        && versions.existsByProductCode(code)
        && versions.findByProductCodeOrderByVersionNoDesc(code).stream()
            .noneMatch(v -> v.isInForce(date))) {
      throw new BusinessRuleException(
          NOT_SELLABLE, "Package " + code + " has no released version in force on " + date);
    }
    return product;
  }

  /**
   * Adds a product, pending authorization. A packaged product needs its cover type (PMADD01) and a
   * new risk code must follow the line's naming pattern (PQ02).
   *
   * @param code risk code
   * @param details attributes
   * @return product
   */
  public RiskProduct createProduct(String code, ProductDetails details) {
    if (products.findByCode(code).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.PRODUCT.label(), code);
    }
    validate(details);
    if (!requireLine(details.lineCode()).acceptsCode(code)) {
      throw new BusinessRuleException(
          "PRODUCT_CODE_PATTERN",
          "Risk code " + code + " does not follow the naming convention of " + details.lineCode());
    }
    RiskProduct saved = products.save(new RiskProduct(code, details));
    record(CatalogKind.PRODUCT, code, AuditAction.CREATE, "Added " + details.name());
    return saved;
  }

  /**
   * Changes a product, pending authorization.
   *
   * @param code risk code
   * @param details attributes
   * @return product
   */
  public RiskProduct updateProduct(String code, ProductDetails details) {
    RiskProduct product = requireProduct(code);
    validate(details);
    product.update(details, versions.existsByProductCode(code));
    record(CatalogKind.PRODUCT, code, AuditAction.UPDATE, "Changed " + details.name());
    return product;
  }

  private void validate(ProductDetails details) {
    requireLine(details.lineCode());
    if (details.packaged() && details.coverTypeCode() == null) {
      throw new BusinessRuleException(
          "PACKAGE_HIERARCHY_INCOMPLETE", "A package needs its cover type (PMADD01)");
    }
    if (details.coverTypeCode() != null
        && coverTypes
            .findByLineCodeAndCode(details.lineCode(), details.coverTypeCode())
            .isEmpty()) {
      throw new BusinessRuleException(
          "COVER_TYPE_INVALID",
          "Cover type " + details.coverTypeCode() + " does not belong to " + details.lineCode());
    }
    LocalDate today = LocalDate.now(clock);
    if (details.marketSegments() != null) {
      details.marketSegments().forEach(s -> lovs.requireValid("MARKET_SEGMENT", s, today));
    }
  }

  private void record(CatalogKind kind, String key, AuditAction action, String summary) {
    audit.record(kind.label(), key, action, summary);
  }

  /**
   * Product list filters; null values are not applied.
   *
   * @param lineCode product line
   * @param packaged package (true) or non-package (false)
   * @param segment market segment allowed
   * @param text risk code or name fragment
   * @param activeOnly only authorized, active products
   * @param lifecycle only products in this lifecycle (BRPM.006), null for the default
   * @param includeArchived with no lifecycle given: also expired and retired products
   */
  public record ProductFilter(
      String lineCode,
      Boolean packaged,
      String segment,
      String text,
      boolean activeOnly,
      ProductLifecycle lifecycle,
      boolean includeArchived) {

    /**
     * Filters of the sellable (lifecycle ACTIVE) products.
     *
     * @param lineCode product line
     * @param packaged package flag
     * @param segment market segment
     * @param text risk code or name fragment
     * @param activeOnly only authorized, active products
     */
    public ProductFilter(
        String lineCode, Boolean packaged, String segment, String text, boolean activeOnly) {
      this(lineCode, packaged, segment, text, activeOnly, null, false);
    }
  }
}
