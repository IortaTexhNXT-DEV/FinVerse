package com.iortatechnxt.brokerverse.commission.domain;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Beneficiary;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Calculation;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.PeriodType;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.SchemeType;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * An incentive scheme (CMRID.003/005/006): No Touch and Top Up (production targets with rates and
 * multipliers per period) and Motor Mania (fixed amount per policy by minimum basic premium, passed
 * on to branches), with the production it covers (insurer, segments, product lines) and its tiers.
 * The schemes are seeded without tiers; BDOI gives targets, rates and amounts (OQ39).
 */
@Entity
@Table(name = "cmr_incentive_scheme")
public class IncentiveScheme extends BaseEntity {

  private static final String SEPARATOR = ",";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 30, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "scheme_type", nullable = false, length = 20)
  private SchemeType schemeType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Calculation calculation;

  @Enumerated(EnumType.STRING)
  @Column(name = "period_type", nullable = false, length = 20)
  private PeriodType periodType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Beneficiary beneficiary;

  @Column(name = "insurer_code", length = 30)
  private String insurerCode;

  @Column(length = 200)
  private String segments;

  @Column(name = "product_lines", length = 200)
  private String productLines;

  @Column(name = "effective_from")
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @Column(nullable = false)
  private boolean active;

  @Column(length = 500)
  private String description;

  @ElementCollection
  @CollectionTable(name = "cmr_incentive_tier", joinColumns = @JoinColumn(name = "scheme_id"))
  @OrderColumn(name = "tier_index")
  private final List<IncentiveTier> tiers = new ArrayList<>();

  protected IncentiveScheme() {}

  /**
   * A new scheme.
   *
   * @param companyId company
   * @param code code
   * @param terms terms
   */
  public IncentiveScheme(Long companyId, String code, Terms terms) {
    this.companyId = companyId;
    this.code = code;
    update(terms);
  }

  /**
   * Changes the scheme.
   *
   * @param t terms
   */
  public final void update(Terms t) {
    this.name = t.name();
    this.schemeType = t.schemeType();
    this.calculation = t.calculation();
    this.periodType = t.periodType();
    this.beneficiary = t.beneficiary();
    this.insurerCode = t.insurerCode();
    this.segments = String.join(SEPARATOR, t.segments());
    this.productLines = String.join(SEPARATOR, t.productLines());
    this.effectiveFrom = t.effectiveFrom();
    this.effectiveTo = t.effectiveTo();
    this.active = t.active();
    this.description = t.description();
    this.tiers.clear();
    this.tiers.addAll(t.tiers());
  }

  /**
   * Whether the scheme covers an invoice's classification.
   *
   * @param insurer insurer
   * @param segment market segment
   * @param productLine product line
   * @return true when every set filter matches
   */
  public boolean covers(String insurer, String segment, String productLine) {
    return (insurerCode == null || insurerCode.equals(insurer))
        && matches(getSegments(), segment)
        && matches(getProductLines(), productLine);
  }

  private static boolean matches(List<String> filter, String value) {
    return filter.isEmpty() || value != null && filter.contains(value.toUpperCase(Locale.ROOT));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public SchemeType getSchemeType() {
    return schemeType;
  }

  public Calculation getCalculation() {
    return calculation;
  }

  public PeriodType getPeriodType() {
    return periodType;
  }

  public Beneficiary getBeneficiary() {
    return beneficiary;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public List<String> getSegments() {
    return split(segments);
  }

  public List<String> getProductLines() {
    return split(productLines);
  }

  private static List<String> split(String value) {
    return value == null || value.isBlank()
        ? List.of()
        : Arrays.stream(value.split(SEPARATOR))
            .map(v -> v.strip().toUpperCase(Locale.ROOT))
            .filter(v -> !v.isEmpty())
            .toList();
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

  public List<IncentiveTier> getTiers() {
    return Collections.unmodifiableList(tiers);
  }

  /**
   * Scheme terms.
   *
   * @param name name
   * @param schemeType No Touch, Top Up, Motor Mania or other
   * @param calculation target tiered or fixed per policy
   * @param periodType period
   * @param beneficiary BDOI or branches
   * @param insurerCode insurer, null for every insurer
   * @param segments segments covered, empty for all
   * @param productLines product lines covered, empty for all
   * @param effectiveFrom first day, may be null
   * @param effectiveTo last day, may be null
   * @param active active
   * @param description description
   * @param tiers tiers
   */
  public record Terms(
      String name,
      SchemeType schemeType,
      Calculation calculation,
      PeriodType periodType,
      Beneficiary beneficiary,
      String insurerCode,
      List<String> segments,
      List<String> productLines,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active,
      String description,
      List<IncentiveTier> tiers) {

    /** Defensive copies. */
    public Terms {
      segments = segments == null ? List.of() : List.copyOf(segments);
      productLines = productLines == null ? List.of() : List.copyOf(productLines);
      tiers = tiers == null ? List.of() : List.copyOf(tiers);
    }
  }
}
