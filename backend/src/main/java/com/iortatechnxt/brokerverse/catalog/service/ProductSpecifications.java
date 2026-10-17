package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.ProductLifecycle;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService.ProductFilter;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Product list filters as a JPA specification (BRNB.001; lifecycle filter BRPM.006: without a
 * lifecycle only sellable products are listed unless archived ones are asked for).
 */
final class ProductSpecifications {

  private static final String CODE = "code";
  private static final String LIFECYCLE = "lifecycleStatus";

  private ProductSpecifications() {}

  /**
   * The filter as a JPA specification.
   *
   * @param f filters
   * @return specification
   */
  static Specification<RiskProduct> of(ProductFilter f) {
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
      if (f.lifecycle() != null) {
        where.add(cb.equal(root.get(LIFECYCLE), f.lifecycle()));
      } else if (!f.includeArchived()) {
        where.add(cb.equal(root.get(LIFECYCLE), ProductLifecycle.ACTIVE));
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
}
