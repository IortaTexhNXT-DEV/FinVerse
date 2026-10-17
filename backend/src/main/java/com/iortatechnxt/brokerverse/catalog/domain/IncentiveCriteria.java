package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * An incentive criterion on the maintained products matrix, for example "CPC2" (PMADD07/08;
 * PRODUCT_MAINTENANCE_DESIGN section 4.3). Effective-dated and maker-checker: an ACTIVE row is
 * never edited; a change end-dates it when its successor row is authorised, so history is kept.
 */
@Entity
@Table(name = "cat_incentive_criteria")
public class IncentiveCriteria extends EffectiveDatedRecord {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 30, updatable = false)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "incentive_type", nullable = false, length = 30)
  private String incentiveType;

  @Enumerated(EnumType.STRING)
  @Column(name = "value_basis", nullable = false, length = 20)
  private IncentiveValueBasis valueBasis;

  @Column(precision = 19, scale = 8)
  private BigDecimal value;

  @Column(name = "rule_params", length = 2000)
  private String ruleParams;

  @Column(length = 1000)
  private String description;

  @Column(name = "successor_of", updatable = false)
  private Long successorOf;

  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.SUBSELECT)
  @CollectionTable(
      name = "cat_incentive_criteria_product",
      joinColumns = @JoinColumn(name = "criteria_id"))
  private List<IncentiveScope> scopes = new ArrayList<>();

  protected IncentiveCriteria() {}

  /**
   * Creates a criterion (or the successor of an active one), pending authorisation.
   *
   * @param companyId company
   * @param code code
   * @param details attributes, products and period
   * @param successorOf id of the active row it replaces, null for a new criterion
   */
  public IncentiveCriteria(Long companyId, String code, Details details, Long successorOf) {
    super(details.effectiveFrom(), details.effectiveTo());
    this.companyId = companyId;
    this.code = code;
    this.successorOf = successorOf;
    apply(details);
  }

  /**
   * Changes a criterion that is not yet active; it must be authorised again.
   *
   * @param details new attributes
   */
  public void update(Details details) {
    if (isActive()) {
      throw new BusinessRuleException(
          "INCENTIVE_ACTIVE_LOCKED", "An active criterion is changed by amending it (successor)");
    }
    setEffectivity(details.effectiveFrom(), details.effectiveTo());
    apply(details);
    markModified();
  }

  private void apply(Details d) {
    if (d.valueBasis() != IncentiveValueBasis.RULE && d.value() == null) {
      throw new BusinessRuleException(
          "INCENTIVE_VALUE_REQUIRED", "Enter the value of the incentive criterion");
    }
    if (d.valueBasis() == IncentiveValueBasis.RATE && d.value() != null) {
      requirePercent(d.value(), "The incentive rate");
    }
    this.name = d.name();
    this.incentiveType = d.incentiveType();
    this.valueBasis = d.valueBasis();
    this.value = d.value();
    this.ruleParams = d.ruleParams();
    this.description = d.description();
    this.scopes = new ArrayList<>(d.scopes());
  }

  /**
   * Ends an active criterion the day before its successor starts, or on a given last day.
   *
   * @param lastDay last effective day
   */
  public void endOn(LocalDate lastDay) {
    setEffectivity(getEffectiveFrom(), lastDay);
  }

  /**
   * Whether the criterion applies to a transaction on a date.
   *
   * @param facts product, cover type, segment, channel and insurer
   * @param date transaction date
   * @return true when effective and one products-matrix entry matches
   */
  public boolean appliesTo(Facts facts, LocalDate date) {
    return isEffectiveOn(date)
        && scopes.stream()
            .anyMatch(
                s ->
                    s.matches(
                        facts.productCode(),
                        facts.coverTypeCode(),
                        facts.segment(),
                        facts.channel(),
                        facts.insurerCode()));
  }

  @Override
  public String catalogReference() {
    return code + " " + getEffectiveFrom();
  }

  @Override
  public String catalogDescription() {
    return name;
  }

  @Override
  public Long catalogCompanyId() {
    return companyId;
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

  public String getIncentiveType() {
    return incentiveType;
  }

  public IncentiveValueBasis getValueBasis() {
    return valueBasis;
  }

  public BigDecimal getValue() {
    return value;
  }

  public String getRuleParams() {
    return ruleParams;
  }

  public String getDescription() {
    return description;
  }

  public Long getSuccessorOf() {
    return successorOf;
  }

  public List<IncentiveScope> getScopes() {
    return List.copyOf(scopes);
  }

  /**
   * Maintainable attributes of a criterion.
   *
   * @param name name
   * @param incentiveType type (LOV INCENTIVE_TYPE)
   * @param valueBasis RATE, FIXED_AMOUNT or RULE
   * @param value value (percent or amount), null for a RULE
   * @param ruleParams rule parameters as JSON, null when none
   * @param description description
   * @param scopes products-matrix entries (at least one)
   * @param effectiveFrom first effective day
   * @param effectiveTo last effective day, null when open ended
   */
  public record Details(
      String name,
      String incentiveType,
      IncentiveValueBasis valueBasis,
      BigDecimal value,
      String ruleParams,
      String description,
      List<IncentiveScope> scopes,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {

    /** Defensive copy. */
    public Details {
      scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }
  }

  /**
   * What a transaction is matched on.
   *
   * @param productCode risk code
   * @param coverTypeCode cover type
   * @param segment market segment
   * @param channel source channel
   * @param insurerCode insurer party code
   */
  public record Facts(
      String productCode,
      String coverTypeCode,
      String segment,
      String channel,
      String insurerCode) {}
}
