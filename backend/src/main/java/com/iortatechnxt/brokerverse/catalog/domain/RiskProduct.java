package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * A BDOI risk product (risk code, e.g. CAR00, MTR10, PAR01) with the attributes that drive the
 * account workflow: package or not, fleet, segments, mortgage, direct payment, multi-year, Free
 * First Year, payment gate, commission, minimum premium and TSU involvement (BRNB.001, Annex I,
 * Appendix B).
 */
@Entity
@Table(name = "cat_product")
public class RiskProduct extends AuthorizableEntity implements CatalogRecord {

  /** Longest multi-year term accepted (BRNB.112). */
  public static final int MAX_TERM_YEARS = 10;

  private static final String SEPARATOR = ",";

  @Column(nullable = false, length = 20, updatable = false)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "line_code", nullable = false, length = 30)
  private String lineCode;

  @Column(name = "cover_type_code", length = 30)
  private String coverTypeCode;

  @Column(nullable = false)
  private boolean packaged;

  @Column(name = "fleet_capable", nullable = false)
  private boolean fleetCapable;

  @Column(name = "market_segments", length = 300)
  private String marketSegments;

  @Column(name = "mortgage_applicable", nullable = false)
  private boolean mortgageApplicable;

  @Column(name = "direct_payment_eligible", nullable = false)
  private boolean directPaymentEligible;

  @Column(name = "multi_year_allowed", nullable = false)
  private boolean multiYearAllowed;

  @Column(name = "max_term_years", nullable = false)
  private int maxTermYears;

  @Column(name = "ffy_eligible", nullable = false)
  private boolean ffyEligible;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_gate", nullable = false, length = 30)
  private PaymentGate paymentGate;

  @Column(name = "default_rate", precision = 19, scale = 8)
  private BigDecimal defaultRate;

  @Column(name = "default_commission_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal defaultCommissionRate;

  @Column(name = "minimum_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal minimumPremium;

  @Column(name = "max_sum_insured", precision = 19, scale = 2)
  private BigDecimal maxSumInsured;

  @Enumerated(EnumType.STRING)
  @Column(name = "tsu_involvement", nullable = false, length = 20)
  private TsuInvolvement tsuInvolvement;

  protected RiskProduct() {}

  /**
   * Creates a product, pending authorization.
   *
   * @param code risk code
   * @param details attributes
   */
  public RiskProduct(String code, ProductDetails details) {
    this.code = code;
    apply(details);
  }

  /**
   * Changes the product; it must be authorized again.
   *
   * @param details new attributes
   */
  public void update(ProductDetails details) {
    apply(details);
    markModified();
  }

  private static int requireValid(ProductDetails d) {
    int term = d.multiYearAllowed() ? d.maxTermYears() : 1;
    if (term < 1 || term > MAX_TERM_YEARS) {
      throw new BusinessRuleException(
          "PRODUCT_TERM_INVALID", "The maximum term must be between 1 and 10 years");
    }
    if (d.minimumPremium() == null || d.minimumPremium().signum() < 0) {
      throw new BusinessRuleException(
          "PRODUCT_MINIMUM_INVALID", "The minimum premium cannot be negative");
    }
    return term;
  }

  private static String segmentsOf(List<String> segments) {
    return segments == null || segments.isEmpty() ? null : String.join(SEPARATOR, segments);
  }

  private void apply(ProductDetails d) {
    int term = requireValid(d);
    this.name = d.name();
    this.lineCode = d.lineCode();
    this.coverTypeCode = d.coverTypeCode();
    this.packaged = d.packaged();
    this.fleetCapable = d.fleetCapable();
    this.marketSegments = segmentsOf(d.marketSegments());
    this.mortgageApplicable = d.mortgageApplicable();
    this.directPaymentEligible = d.directPaymentEligible();
    this.multiYearAllowed = d.multiYearAllowed();
    this.maxTermYears = term;
    this.ffyEligible = d.ffyEligible();
    this.paymentGate = d.paymentGate();
    this.defaultRate = d.defaultRate();
    this.defaultCommissionRate = d.defaultCommissionRate();
    this.minimumPremium = d.minimumPremium();
    this.maxSumInsured = d.maxSumInsured();
    this.tsuInvolvement = d.tsuInvolvement() == null ? TsuInvolvement.BY_RULES : d.tsuInvolvement();
  }

  /**
   * Market segments allowed (empty = every segment).
   *
   * @return segment codes
   */
  public List<String> getMarketSegmentList() {
    return marketSegments == null ? List.of() : Arrays.asList(marketSegments.split(SEPARATOR));
  }

  /**
   * Whether the product may be sold to a market segment.
   *
   * @param segment segment code, may be null
   * @return true when allowed
   */
  public boolean allowsSegment(String segment) {
    return marketSegments == null || segment == null || getMarketSegmentList().contains(segment);
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

  public String getLineCode() {
    return lineCode;
  }

  public String getCoverTypeCode() {
    return coverTypeCode;
  }

  public boolean isPackaged() {
    return packaged;
  }

  public boolean isFleetCapable() {
    return fleetCapable;
  }

  public boolean isMortgageApplicable() {
    return mortgageApplicable;
  }

  public boolean isDirectPaymentEligible() {
    return directPaymentEligible;
  }

  public boolean isMultiYearAllowed() {
    return multiYearAllowed;
  }

  public int getMaxTermYears() {
    return maxTermYears;
  }

  public boolean isFfyEligible() {
    return ffyEligible;
  }

  public PaymentGate getPaymentGate() {
    return paymentGate;
  }

  public BigDecimal getDefaultRate() {
    return defaultRate;
  }

  public BigDecimal getDefaultCommissionRate() {
    return defaultCommissionRate;
  }

  public BigDecimal getMinimumPremium() {
    return minimumPremium;
  }

  public BigDecimal getMaxSumInsured() {
    return maxSumInsured;
  }

  /**
   * Whether a total sum insured exceeds the package TSI limit.
   *
   * @param totalSumInsured total sum insured
   * @return true when a limit is set and exceeded
   */
  public boolean exceedsPackageLimit(BigDecimal totalSumInsured) {
    return maxSumInsured != null
        && totalSumInsured != null
        && totalSumInsured.compareTo(maxSumInsured) > 0;
  }

  public TsuInvolvement getTsuInvolvement() {
    return tsuInvolvement;
  }

  /**
   * Maintainable attributes of a product.
   *
   * @param name name
   * @param lineCode product line
   * @param coverTypeCode cover type within the line, may be null
   * @param packaged package product (false = non-package, priced through TSU)
   * @param fleetCapable may cover several vehicles
   * @param marketSegments segments allowed, empty for all
   * @param mortgageApplicable may carry a mortgagee (Insurance Advice)
   * @param directPaymentEligible may be paid directly to the insurer (BRNB.114)
   * @param multiYearAllowed multi-year terms allowed (BRNB.112)
   * @param maxTermYears longest term in years
   * @param ffyEligible Free First Year allowed (BRNB.113)
   * @param paymentGate payment gate before placement
   * @param defaultRate default premium rate in percent, may be null
   * @param defaultCommissionRate default commission rate in percent
   * @param minimumPremium minimum premium
   * @param maxSumInsured package TSI limit, null for none
   * @param tsuInvolvement TSU involvement rule
   */
  public record ProductDetails(
      String name,
      String lineCode,
      String coverTypeCode,
      boolean packaged,
      boolean fleetCapable,
      List<String> marketSegments,
      boolean mortgageApplicable,
      boolean directPaymentEligible,
      boolean multiYearAllowed,
      int maxTermYears,
      boolean ffyEligible,
      PaymentGate paymentGate,
      BigDecimal defaultRate,
      BigDecimal defaultCommissionRate,
      BigDecimal minimumPremium,
      BigDecimal maxSumInsured,
      TsuInvolvement tsuInvolvement) {}
}
