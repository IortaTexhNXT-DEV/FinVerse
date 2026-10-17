package com.iortatechnxt.brokerverse.booking.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Auto-book rule (BRNB.076): an account of the product and market segment entering POLICY_ISSUED is
 * queued for the next booking batch. A blank product or segment matches any.
 */
@Entity
@Table(name = "bkg_auto_book_rule")
public class AutoBookRule extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "product_code", length = 20)
  private String productCode;

  @Column(name = "market_segment", length = 40)
  private String marketSegment;

  @Column(nullable = false)
  private boolean enabled;

  @Column(nullable = false, length = 300)
  private String description;

  protected AutoBookRule() {}

  /**
   * Creates a rule.
   *
   * @param companyId company
   * @param criteria product, segment, enabled and description
   */
  public AutoBookRule(Long companyId, Criteria criteria) {
    this.companyId = companyId;
    apply(criteria);
  }

  /**
   * Replaces the criteria.
   *
   * @param criteria product, segment, enabled and description
   */
  public final void apply(Criteria criteria) {
    this.productCode = blankToNull(criteria.productCode());
    this.marketSegment = blankToNull(criteria.marketSegment());
    this.enabled = criteria.enabled();
    this.description = criteria.description();
  }

  /**
   * Whether an enabled rule covers a product and segment.
   *
   * @param product product code
   * @param segment market segment
   * @return true when it matches
   */
  public boolean matches(String product, String segment) {
    return enabled && any(productCode, product) && any(marketSegment, segment);
  }

  private static boolean any(String criterion, String value) {
    return criterion == null || Objects.equals(criterion, value);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getMarketSegment() {
    return marketSegment;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Rule criteria.
   *
   * @param productCode product, blank for any
   * @param marketSegment segment, blank for any
   * @param enabled enabled
   * @param description description
   */
  public record Criteria(
      String productCode, String marketSegment, boolean enabled, String description) {}
}
