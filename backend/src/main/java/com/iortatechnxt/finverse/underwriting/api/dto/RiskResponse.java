package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.underwriting.domain.MarineDetails;
import com.iortatechnxt.finverse.underwriting.domain.PolicyRisk;
import java.math.BigDecimal;

/**
 * Risk view.
 *
 * @param lineNo line number
 * @param description description
 * @param sumInsured sum insured 100 %
 * @param rate rate %
 * @param premium gross premium 100 %
 * @param occupation occupation
 * @param accumulationZone accumulation zone
 * @param marine shipment details (marine), null otherwise
 */
public record RiskResponse(
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
   * @return response
   */
  public static RiskResponse from(PolicyRisk r) {
    return new RiskResponse(
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
