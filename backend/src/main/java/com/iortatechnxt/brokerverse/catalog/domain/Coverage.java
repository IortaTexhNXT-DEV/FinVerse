package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A coverage or peril of a product line, the lowest level of the product hierarchy (PMADD01: LOB
 * &gt; Subline / Type &gt; Subtype &gt; Coverage / Peril). Package versions list the coverages they
 * include and each insurer's terms per coverage (PMADD02). Maker-checker master data.
 */
@Entity
@Table(name = "cat_coverage")
public class Coverage extends AuthorizableEntity implements CatalogRecord {

  @Column(name = "line_code", nullable = false, length = 30, updatable = false)
  private String lineCode;

  @Column(nullable = false, length = 30, updatable = false)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false, length = 20)
  private String kind;

  @Column(nullable = false)
  private boolean basic;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected Coverage() {}

  /**
   * Creates a coverage, pending authorization.
   *
   * @param lineCode product line
   * @param code code, unique within the line
   * @param details name, kind, basic flag and order
   */
  public Coverage(String lineCode, String code, CoverageDetails details) {
    this.lineCode = lineCode;
    this.code = code;
    apply(details);
  }

  /**
   * Changes the coverage; it must be authorized again.
   *
   * @param details new attributes
   */
  public void update(CoverageDetails details) {
    apply(details);
    markModified();
  }

  private void apply(CoverageDetails details) {
    this.name = details.name();
    this.kind = details.kind();
    this.basic = details.basic();
    this.sortOrder = details.sortOrder();
  }

  @Override
  public String catalogReference() {
    return lineCode + "/" + code;
  }

  @Override
  public String catalogDescription() {
    return name;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getKind() {
    return kind;
  }

  public boolean isBasic() {
    return basic;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  /**
   * Maintainable attributes of a coverage.
   *
   * @param name name
   * @param kind kind (LOV COVERAGE_KIND: SECTION, COVERAGE, PERIL, EXTENSION)
   * @param basic basic cover: a package needs at least one included basic coverage
   * @param sortOrder display order
   */
  public record CoverageDetails(String name, String kind, boolean basic, int sortOrder) {}
}
