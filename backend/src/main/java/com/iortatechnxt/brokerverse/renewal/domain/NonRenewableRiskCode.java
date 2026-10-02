package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A risk code that is not renewable (BRRN.009): renewals on it are tagged Not for Renewal by the
 * system and receive the Not for Renewal Letter. Maintained with maker-checker; never deleted, but
 * end-dated.
 */
@Entity
@Table(name = "rnw_non_renewable_risk_code")
public class NonRenewableRiskCode extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "risk_code", nullable = false, length = 20)
  private String riskCode;

  @Column(name = "line_code", length = 30)
  private String lineCode;

  @Column(nullable = false, length = 200)
  private String reason;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  protected NonRenewableRiskCode() {}

  /**
   * Creates a code pending authorization.
   *
   * @param companyId company
   * @param data code data
   */
  public NonRenewableRiskCode(Long companyId, Data data) {
    this.companyId = companyId;
    apply(data);
  }

  /**
   * Changes the code; it must be authorized again.
   *
   * @param data code data
   */
  public void update(Data data) {
    apply(data);
    markModified();
  }

  private void apply(Data data) {
    if (data.effectiveTo() != null && data.effectiveTo().isBefore(data.effectiveFrom())) {
      throw new BusinessRuleException(
          "LOV_EFFECTIVITY_INVALID", "The effective-to date is before the effective-from date");
    }
    this.riskCode = data.riskCode();
    this.lineCode = data.lineCode();
    this.reason = data.reason();
    this.effectiveFrom = data.effectiveFrom();
    this.effectiveTo = data.effectiveTo();
  }

  /**
   * Whether the code applies to a risk on a date.
   *
   * @param code risk code
   * @param line product line
   * @param date date
   * @return true when authorized, in force and matching
   */
  public boolean appliesTo(String code, String line, LocalDate date) {
    return isActive()
        && riskCode.equals(code)
        && (lineCode == null || lineCode.equals(line))
        && inForce(date);
  }

  private boolean inForce(LocalDate date) {
    return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRiskCode() {
    return riskCode;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getReason() {
    return reason;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  /**
   * Data of a code.
   *
   * @param riskCode risk code
   * @param lineCode product line, null for any
   * @param reason reason
   * @param effectiveFrom effective from
   * @param effectiveTo effective to, null for open
   */
  public record Data(
      String riskCode,
      String lineCode,
      String reason,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {}
}
