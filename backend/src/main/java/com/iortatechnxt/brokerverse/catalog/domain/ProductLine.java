package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Product line of Annex II (Property, Motor, Engineering...). It declares the kind of risk items
 * its accounts carry and the Appendix A formula used to rate them (BRNB.001/051).
 */
@Entity
@Table(name = "cat_product_line")
public class ProductLine extends AuthorizableEntity implements CatalogRecord {

  @Column(nullable = false, length = 30, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_item_kind", nullable = false, length = 20)
  private RiskItemKind riskItemKind;

  @Enumerated(EnumType.STRING)
  @Column(name = "rating_method", nullable = false, length = 20)
  private RatingMethod ratingMethod;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected ProductLine() {}

  /**
   * Creates a line, pending authorization.
   *
   * @param code code
   * @param details name, item kind, formula and order
   */
  public ProductLine(String code, LineDetails details) {
    this.code = code;
    apply(details);
  }

  /**
   * Changes the line; it must be authorized again.
   *
   * @param details new attributes
   */
  public void update(LineDetails details) {
    apply(details);
    markModified();
  }

  private void apply(LineDetails details) {
    this.name = details.name();
    this.riskItemKind = details.riskItemKind();
    this.ratingMethod = details.ratingMethod();
    this.sortOrder = details.sortOrder();
  }

  @Override
  public String catalogReference() {
    return code;
  }

  @Override
  public String catalogDescription() {
    return name;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public RiskItemKind getRiskItemKind() {
    return riskItemKind;
  }

  public RatingMethod getRatingMethod() {
    return ratingMethod;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  /**
   * Maintainable attributes of a product line.
   *
   * @param name name
   * @param riskItemKind kind of risk items
   * @param ratingMethod Appendix A formula
   * @param sortOrder display order
   */
  public record LineDetails(
      String name, RiskItemKind riskItemKind, RatingMethod ratingMethod, int sortOrder) {}
}
