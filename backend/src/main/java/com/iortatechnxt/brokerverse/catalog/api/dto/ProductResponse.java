package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.PaymentGate;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.TsuInvolvement;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * A BDOI risk product.
 *
 * @param id id
 * @param code risk code
 * @param name name
 * @param lineCode product line
 * @param coverTypeCode cover type
 * @param packaged package product
 * @param fleetCapable fleet capable
 * @param marketSegments segments allowed (empty = all)
 * @param mortgageApplicable mortgage applicable
 * @param directPaymentEligible direct payment eligible
 * @param multiYearAllowed multi-year allowed
 * @param maxTermYears longest term in years
 * @param ffyEligible Free First Year eligible
 * @param paymentGate payment gate
 * @param defaultRate default premium rate %
 * @param defaultCommissionRate default commission %
 * @param minimumPremium minimum premium
 * @param maxSumInsured package TSI limit
 * @param tsuInvolvement TSU involvement
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record ProductResponse(
    Long id,
    String code,
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
    TsuInvolvement tsuInvolvement,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static ProductResponse from(RiskProduct e) {
    return new ProductResponse(
        e.getId(),
        e.getCode(),
        e.getName(),
        e.getLineCode(),
        e.getCoverTypeCode(),
        e.isPackaged(),
        e.isFleetCapable(),
        e.getMarketSegmentList(),
        e.isMortgageApplicable(),
        e.isDirectPaymentEligible(),
        e.isMultiYearAllowed(),
        e.getMaxTermYears(),
        e.isFfyEligible(),
        e.getPaymentGate(),
        e.getDefaultRate(),
        e.getDefaultCommissionRate(),
        e.getMinimumPremium(),
        e.getMaxSumInsured(),
        e.getTsuInvolvement(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
