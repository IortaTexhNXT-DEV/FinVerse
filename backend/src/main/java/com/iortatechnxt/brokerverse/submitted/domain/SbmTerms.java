package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Insurer, policy and period of a submitted policy (BRIDSP-04).
 *
 * @param insurerCode insurer party code
 * @param policyNo policy number
 * @param inceptionDate inception
 * @param expiryDate expiry
 * @param coverageDays days of cover
 * @param sumInsured amount insured
 * @param totalPremium total premium
 * @param currency currency
 */
@Embeddable
public record SbmTerms(
    @Column(name = "insurer_code", length = 30) String insurerCode,
    @Column(name = "policy_no", length = 60) String policyNo,
    @Column(name = "inception_date") LocalDate inceptionDate,
    @Column(name = "expiry_date", nullable = false) LocalDate expiryDate,
    @Column(name = "coverage_days") Integer coverageDays,
    @Column(name = "sum_insured", precision = 19, scale = 2) BigDecimal sumInsured,
    @Column(name = "total_premium", precision = 19, scale = 2) BigDecimal totalPremium,
    @Column(name = "currency", nullable = false, length = 3) String currency) {

  /**
   * The terms in a currency when they carry none (the base currency of the company).
   *
   * @param code currency
   * @return terms with a currency
   */
  public SbmTerms orInCurrency(String code) {
    if (currency != null) {
      return this;
    }
    return new SbmTerms(
        insurerCode,
        policyNo,
        inceptionDate,
        expiryDate,
        coverageDays,
        sumInsured,
        totalPremium,
        code);
  }

  /**
   * The terms with the days of cover computed from the period when they are not given.
   *
   * @return terms
   */
  public SbmTerms withCoverageDays() {
    if (coverageDays != null || inceptionDate == null) {
      return this;
    }
    int days = (int) ChronoUnit.DAYS.between(inceptionDate, expiryDate);
    return new SbmTerms(
        insurerCode, policyNo, inceptionDate, expiryDate, days, sumInsured, totalPremium, currency);
  }
}
