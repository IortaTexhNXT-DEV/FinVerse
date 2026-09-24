package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.CoverType;
import com.iortatechnxt.brokerverse.catalog.domain.CoverTypeRepository;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine.LineDetails;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLineRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product master (BRNB.001): product lines and cover types of Annex II and the BDOI risk products
 * with their workflow attributes. Changes are maker-checker; authorization is done by {@link
 * CatalogRecords}.
 */
@Service
@Transactional
public class ProductCatalogService {

  private static final String CODE = "code";

  private final ProductLineRepository lines;
  private final CoverTypeRepository coverTypes;
  private final RiskProductRepository products;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lines product lines
   * @param coverTypes cover types
   * @param products risk products
   * @param lovs lists of values (market segments)
   * @param audit audit trail
   * @param clock clock
   */
  public ProductCatalogService(
      ProductLineRepository lines,
      CoverTypeRepository coverTypes,
      RiskProductRepository products,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.lines = lines;
    this.coverTypes = coverTypes;
    this.products = products;
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
    requireLine(lineCode);
    if (coverTypes.findByLineCodeAndCode(lineCode, code).isPresent()) {
      throw new DuplicateResourceException(CatalogKind.COVER_TYPE.label(), lineCode + "/" + code);
    }
    CoverType saved = coverTypes.save(new CoverType(lineCode, code, name, sortOrder));
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
    return products.findAll(specification(filter), Sort.by(CODE));
  }

  private static Specification<RiskProduct> specification(ProductFilter f) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      if (f.lineCode() != null) {
        where.add(cb.equal(root.get("lineCode"), f.lineCode()));
      }
      if (f.packaged() != null) {
        where.add(cb.equal(root.get("packaged"), f.packaged()));
      }
      if (f.segment() != null) {
        where.add(
            cb.or(
                cb.isNull(root.get("marketSegments")),
                cb.like(root.get("marketSegments"), "%" + f.segment() + "%")));
      }
      if (f.activeOnly()) {
        where.add(cb.equal(root.get("recordStatus"), RecordStatus.ACTIVE));
      }
      if (f.text() != null && !f.text().isBlank()) {
        String like = "%" + f.text().trim().toLowerCase(Locale.ROOT) + "%";
        where.add(
            cb.or(
                cb.like(cb.lower(root.get(CODE)), like),
                cb.like(cb.lower(root.get("name")), like)));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
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
   * Adds a product, pending authorization.
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
    product.update(details);
    record(CatalogKind.PRODUCT, code, AuditAction.UPDATE, "Changed " + details.name());
    return product;
  }

  private void validate(ProductDetails details) {
    requireLine(details.lineCode());
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
   */
  public record ProductFilter(
      String lineCode, Boolean packaged, String segment, String text, boolean activeOnly) {}
}
