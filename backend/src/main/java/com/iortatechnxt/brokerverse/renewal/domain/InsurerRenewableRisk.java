package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A risk code an insurer renews (insurer renewable list, Annex BRRN.020 SC-10; FR-RN-020). An
 * insurer with rows renews only the risk codes listed; an insurer without rows has given no list.
 * Effective-dated, maker-checker; maintained in Renewal Setup.
 */
@Entity
@Table(name = "rnw_insurer_renewable_risk")
public class InsurerRenewableRisk extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "risk_code", nullable = false, length = 20, updatable = false)
  private String riskCode;

  @Column(length = 200)
  private String remarks;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  protected InsurerRenewableRisk() {}

  /**
   * Creates a row pending authorization.
   *
   * @param companyId company
   * @param data insurer, risk code, remarks and dates
   */
  public InsurerRenewableRisk(Long companyId, Data data) {
    this.companyId = companyId;
    this.insurerCode = data.insurerCode();
    this.riskCode = data.riskCode();
    apply(data);
  }

  /**
   * Changes the remarks and dates; the row must be authorized again.
   *
   * @param data new values (insurer and risk code are kept)
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
    this.remarks = data.remarks();
    this.effectiveFrom = data.effectiveFrom();
    this.effectiveTo = data.effectiveTo();
  }

  /**
   * Whether the row is authorized and in force on a date.
   *
   * @param date date
   * @return true when it counts
   */
  public boolean inForce(LocalDate date) {
    return isActive()
        && !date.isBefore(effectiveFrom)
        && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getRiskCode() {
    return riskCode;
  }

  public String getRemarks() {
    return remarks;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  /**
   * Values of a row.
   *
   * @param insurerCode insurer party code
   * @param riskCode risk code the insurer renews
   * @param remarks remarks, may be null
   * @param effectiveFrom first day
   * @param effectiveTo last day, null when open
   */
  public record Data(
      String insurerCode,
      String riskCode,
      String remarks,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {}
}
