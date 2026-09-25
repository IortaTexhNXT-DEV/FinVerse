package com.iortatechnxt.brokerverse.frbs.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A service-fee rate (FRBS 2.10.0, Appendix A VI): the share of fully paid commission, net of the
 * insurer's withholding tax, paid to the units and referrers of a service-fee segment (LOV {@code
 * SERVICE_FEE_SEGMENT}) for the market segments it covers. The values are BDOI's to confirm (AQ20).
 */
@Entity
@Table(name = "frbs_service_fee_rule")
public class ServiceFeeRule extends BaseEntity {

  /** The only base BDOI has described. */
  public static final String BASE = "COMMISSION_FULLY_PAID";

  @Column(nullable = false, length = 20)
  private String segment;

  @Column(name = "market_segments", nullable = false, length = 200)
  private String marketSegments;

  @Column(nullable = false, precision = 9, scale = 4)
  private BigDecimal rate;

  @Column(name = "net_of_wtax", nullable = false)
  private boolean netOfWtax;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Column(nullable = false)
  private boolean active;

  @Column(length = 250)
  private String description;

  protected ServiceFeeRule() {}

  /**
   * A new rule.
   *
   * @param values content
   */
  public ServiceFeeRule(RuleValues values) {
    apply(values);
  }

  /**
   * Replaces the content.
   *
   * @param values content
   */
  public final void apply(RuleValues values) {
    segment = values.segment();
    marketSegments = String.join(",", values.marketSegments());
    rate = values.rate();
    netOfWtax = values.netOfWtax();
    effectiveFrom = values.effectiveFrom();
    effectiveTo = values.effectiveTo();
    active = values.active();
    description = values.description();
  }

  /**
   * Whether the rule applies to an invoice of a market segment paid on a date.
   *
   * @param marketSegment invoice market segment
   * @param date date the invoice was fully paid
   * @return true when active, effective and covering the segment
   */
  public boolean covers(String marketSegment, LocalDate date) {
    boolean effective =
        !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
    boolean known = marketSegment != null && getMarketSegments().contains(marketSegment);
    return active && effective && known;
  }

  public String getSegment() {
    return segment;
  }

  /**
   * The market segments covered.
   *
   * @return codes
   */
  public List<String> getMarketSegments() {
    return List.of(marketSegments.split(",")).stream().map(String::trim).toList();
  }

  public BigDecimal getRate() {
    return rate;
  }

  public String getBase() {
    return BASE;
  }

  public boolean isNetOfWtax() {
    return netOfWtax;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public boolean isActive() {
    return active;
  }

  public String getDescription() {
    return description;
  }

  /**
   * The content of a rule.
   *
   * @param segment service-fee segment
   * @param marketSegments market segments covered
   * @param rate rate in percent
   * @param netOfWtax whether the commission base is net of the insurer's withholding tax
   * @param effectiveFrom first day
   * @param effectiveTo last day, null when open
   * @param active whether applied
   * @param description description
   */
  public record RuleValues(
      String segment,
      List<String> marketSegments,
      BigDecimal rate,
      boolean netOfWtax,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active,
      String description) {

    /** Copies the segments. */
    public RuleValues {
      marketSegments = marketSegments == null ? List.of() : List.copyOf(marketSegments);
    }
  }
}
