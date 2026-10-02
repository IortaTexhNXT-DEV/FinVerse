package com.iortatechnxt.brokerverse.claims.api.dto;

import com.iortatechnxt.brokerverse.claims.service.PolicyCover;
import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.brokerverse.underwriting.service.RiskSnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Policy looked up on the claim notification form.
 *
 * @param policyId policy id
 * @param policyNo policy number
 * @param status policy status
 * @param productCode product code
 * @param productName product name
 * @param businessLine class
 * @param customerCode policyholder code
 * @param customerName policyholder name
 * @param insuredName insured
 * @param periodFrom period start
 * @param periodTo period end
 * @param currency currency
 * @param sharePct company share %
 * @param coinsurerCode coinsurer, null when not coinsured
 * @param coinsuranceLeader whether the company leads the coinsurance
 * @param inForce whether the policy covers the loss date
 * @param risks insured risks
 */
public record PolicyCoverResponse(
    Long policyId,
    String policyNo,
    String status,
    String productCode,
    String productName,
    String businessLine,
    String customerCode,
    String customerName,
    String insuredName,
    LocalDate periodFrom,
    LocalDate periodTo,
    String currency,
    BigDecimal sharePct,
    String coinsurerCode,
    boolean coinsuranceLeader,
    boolean inForce,
    List<Risk> risks) {

  /**
   * Maps a policy cover.
   *
   * @param c cover
   * @return response
   */
  public static PolicyCoverResponse from(PolicyCover c) {
    PolicySnapshot p = c.policy();
    return new PolicyCoverResponse(
        p.id(),
        p.policyNo(),
        p.status().name(),
        p.productCode(),
        p.productName(),
        p.businessLine(),
        p.customerCode(),
        p.customerName(),
        p.insuredName(),
        p.periodFrom(),
        p.periodTo(),
        p.currency(),
        p.sharePct(),
        p.coinsurerCode(),
        p.coinsuranceLeader(),
        c.inForce(),
        c.risks().stream().map(Risk::from).toList());
  }

  /**
   * An insured risk.
   *
   * @param lineNo risk line
   * @param description description
   * @param sumInsured sum insured at 100 %
   */
  public record Risk(int lineNo, String description, BigDecimal sumInsured) {

    static Risk from(RiskSnapshot r) {
      return new Risk(r.lineNo(), r.description(), r.sumInsured());
    }
  }
}
