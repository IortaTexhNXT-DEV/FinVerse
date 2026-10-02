package com.iortatechnxt.brokerverse.underwriting.service;

import com.iortatechnxt.brokerverse.underwriting.domain.MarineDetails;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyRisk;
import java.math.BigDecimal;

/**
 * Immutable view of an insured risk for other modules (reinsurance allocation per risk, claims
 * registration against a risk, accumulation control).
 *
 * @param id risk id
 * @param policyId policy id
 * @param lineNo line number within the policy
 * @param description description
 * @param sumInsured sum insured at 100 %
 * @param rate premium rate %
 * @param premium gross premium at 100 %
 * @param occupation occupation
 * @param accumulationZone accumulation zone
 * @param marine shipment details, null for non-marine risks
 */
public record RiskSnapshot(
    Long id,
    Long policyId,
    int lineNo,
    String description,
    BigDecimal sumInsured,
    BigDecimal rate,
    BigDecimal premium,
    String occupation,
    String accumulationZone,
    MarineDetails marine) {

  /**
   * Maps a risk.
   *
   * @param r risk
   * @return snapshot
   */
  public static RiskSnapshot of(PolicyRisk r) {
    return new RiskSnapshot(
        r.getId(),
        r.getPolicy().getId(),
        r.getLineNo(),
        r.getDescription(),
        r.getSumInsured(),
        r.getRate(),
        r.getPremium(),
        r.getOccupation(),
        r.getAccumulationZone(),
        r.marine());
  }
}
