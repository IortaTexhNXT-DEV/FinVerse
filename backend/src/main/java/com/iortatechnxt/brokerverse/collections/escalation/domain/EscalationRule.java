package com.iortatechnxt.brokerverse.collections.escalation.domain;

import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Basis;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An escalation rule (BRCLXN.049, CQ14): a basis and threshold, optional filters on the account
 * (segment, sales unit, product line, outstanding range), the level that receives the escalation,
 * its SLA and whether to notify. Maker-checker master data: a new or changed rule applies once
 * another user authorized it (MASTER_AUTHORIZE).
 */
@Entity
@Table(name = "clx_escalation_rule")
public class EscalationRule extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Basis basis;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal threshold;

  @Column(length = 40)
  private String segment;

  @Column(name = "sales_unit", length = 20)
  private String salesUnit;

  @Column(name = "product_line", length = 30)
  private String productLine;

  @Column(name = "amount_from", precision = 19, scale = 2)
  private BigDecimal amountFrom;

  @Column(name = "amount_to", precision = 19, scale = 2)
  private BigDecimal amountTo;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_level", nullable = false, length = 20)
  private TargetLevel targetLevel;

  @Column(name = "target_username", length = 50)
  private String targetUsername;

  @Column(name = "reason_code", nullable = false, length = 40)
  private String reasonCode;

  @Column(name = "sla_hours", nullable = false)
  private int slaHours;

  @Column(nullable = false)
  private boolean notify;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  protected EscalationRule() {}

  /**
   * A new rule, pending authorization.
   *
   * @param companyId company
   * @param code rule code
   * @param terms what the rule checks and where it escalates
   */
  public EscalationRule(Long companyId, String code, Terms terms) {
    this.companyId = companyId;
    this.code = code;
    apply(terms);
  }

  /**
   * Changes the rule; it must be authorized again.
   *
   * @param terms new terms
   */
  public void change(Terms terms) {
    apply(terms);
    markModified();
  }

  private void apply(Terms t) {
    validate(t);
    Filters f = t.filters();
    this.name = t.name();
    this.basis = t.basis();
    this.threshold = t.threshold();
    this.segment = f.segment();
    this.salesUnit = f.salesUnit();
    this.productLine = f.productLine();
    this.amountFrom = f.amountFrom();
    this.amountTo = f.amountTo();
    this.targetLevel = t.targetLevel();
    this.targetUsername = t.targetUsername();
    this.reasonCode = t.reasonCode();
    this.slaHours = t.slaHours();
    this.notify = t.notifyTarget();
    this.effectiveFrom = t.effectiveFrom();
    this.effectiveTo = t.effectiveTo();
  }

  private static void validate(Terms t) {
    if (t.threshold().signum() <= 0 || t.slaHours() <= 0) {
      throw new BusinessRuleException(
          "CLX_RULE_INVALID", "The threshold and the SLA hours must be positive");
    }
    if (t.targetLevel() == TargetLevel.USER && t.targetUsername() == null) {
      throw new BusinessRuleException(
          "CLX_RULE_TARGET", "Name the user who receives the escalations of this rule");
    }
    validateRanges(t);
  }

  private static void validateRanges(Terms t) {
    if (t.effectiveTo() != null && t.effectiveTo().isBefore(t.effectiveFrom())) {
      throw new BusinessRuleException(
          "CLX_RULE_DATES", "The rule cannot end before it becomes effective");
    }
    Filters f = t.filters();
    if (f.amountFrom() != null
        && f.amountTo() != null
        && f.amountTo().compareTo(f.amountFrom()) < 0) {
      throw new BusinessRuleException("CLX_RULE_AMOUNTS", "The amount range ends below its start");
    }
  }

  /**
   * Whether the rule applies on a date: authorized, active and effective.
   *
   * @param date business date
   * @return true when it applies
   */
  public boolean appliesOn(LocalDate date) {
    return isActive()
        && !effectiveFrom.isAfter(date)
        && (effectiveTo == null || !effectiveTo.isBefore(date));
  }

  /**
   * The rule's account filters.
   *
   * @return filters
   */
  public Filters filters() {
    return new Filters(segment, salesUnit, productLine, amountFrom, amountTo);
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

  public Basis getBasis() {
    return basis;
  }

  public BigDecimal getThreshold() {
    return threshold;
  }

  public TargetLevel getTargetLevel() {
    return targetLevel;
  }

  public String getTargetUsername() {
    return targetUsername;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public int getSlaHours() {
    return slaHours;
  }

  public boolean isNotify() {
    return notify;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  /**
   * Which accounts a rule looks at; empty filters match every account.
   *
   * @param segment market segment
   * @param salesUnit sales unit
   * @param productLine product line
   * @param amountFrom lowest outstanding
   * @param amountTo highest outstanding
   */
  public record Filters(
      String segment,
      String salesUnit,
      String productLine,
      BigDecimal amountFrom,
      BigDecimal amountTo) {}

  /**
   * The terms of a rule.
   *
   * @param name name
   * @param basis basis
   * @param threshold days, count or amount
   * @param filters account filters
   * @param targetLevel who receives the escalation
   * @param targetUsername designated user, may be null (the stage's queue)
   * @param reasonCode escalation reason (LOV CLX_ESCALATION_REASON)
   * @param slaHours hours the receiving level has to act (CLX_ESCALATION_OVERDUE)
   * @param notifyTarget whether to notify the target and the handler
   * @param effectiveFrom first day
   * @param effectiveTo last day, may be null
   */
  public record Terms(
      String name,
      Basis basis,
      BigDecimal threshold,
      Filters filters,
      TargetLevel targetLevel,
      String targetUsername,
      String reasonCode,
      int slaHours,
      boolean notifyTarget,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {}
}
