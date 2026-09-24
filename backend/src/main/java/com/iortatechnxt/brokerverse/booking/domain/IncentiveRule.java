package com.iortatechnxt.brokerverse.booking.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Incentive eligibility rule (BRNB.107): a booking of the product, market segment and source
 * channel dated within the period is flagged incentive eligible. Blank criteria match any. The
 * qualification rules are parked (Q33): the table ships empty apart from a demo rule.
 */
@Entity
@Table(name = "bkg_incentive_rule")
public class IncentiveRule extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "product_code", length = 20)
  private String productCode;

  @Column(name = "market_segment", length = 40)
  private String marketSegment;

  @Column(name = "source_channel", length = 40)
  private String sourceChannel;

  @Column(name = "period_from", nullable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to")
  private LocalDate periodTo;

  @Column(nullable = false)
  private boolean active;

  @Column(nullable = false, length = 300)
  private String description;

  protected IncentiveRule() {}

  /**
   * Creates a rule.
   *
   * @param companyId company
   * @param criteria criteria
   */
  public IncentiveRule(Long companyId, Criteria criteria) {
    this.companyId = companyId;
    apply(criteria);
  }

  /**
   * Replaces the criteria.
   *
   * @param criteria criteria
   */
  public final void apply(Criteria criteria) {
    if (criteria.periodFrom() == null
        || criteria.periodTo() != null && criteria.periodTo().isBefore(criteria.periodFrom())) {
      throw new BusinessRuleException(
          "INCENTIVE_PERIOD_INVALID", "The period end must not be before the period start");
    }
    this.productCode = blankToNull(criteria.productCode());
    this.marketSegment = blankToNull(criteria.marketSegment());
    this.sourceChannel = blankToNull(criteria.sourceChannel());
    this.periodFrom = criteria.periodFrom();
    this.periodTo = criteria.periodTo();
    this.active = criteria.active();
    this.description = criteria.description();
  }

  /**
   * Whether the active rule covers a booking.
   *
   * @param product product code
   * @param segment market segment
   * @param channel source channel
   * @param date booking date
   * @return true when it matches
   */
  public boolean matches(String product, String segment, String channel, LocalDate date) {
    return active
        && covers(date)
        && any(productCode, product)
        && any(marketSegment, segment)
        && any(sourceChannel, channel);
  }

  private boolean covers(LocalDate date) {
    return !date.isBefore(periodFrom) && (periodTo == null || !date.isAfter(periodTo));
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

  public String getSourceChannel() {
    return sourceChannel;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public boolean isActive() {
    return active;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Rule criteria.
   *
   * @param productCode product, blank for any
   * @param marketSegment segment, blank for any
   * @param sourceChannel channel, blank for any
   * @param periodFrom first booking date covered
   * @param periodTo last booking date covered, null for open-ended
   * @param active active
   * @param description description
   */
  public record Criteria(
      String productCode,
      String marketSegment,
      String sourceChannel,
      LocalDate periodFrom,
      LocalDate periodTo,
      boolean active,
      String description) {}
}
