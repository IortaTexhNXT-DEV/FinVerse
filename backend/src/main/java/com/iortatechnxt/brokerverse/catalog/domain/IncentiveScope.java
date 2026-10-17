package com.iortatechnxt.brokerverse.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * One entry of the products matrix an incentive criterion applies to (PMADD07; table {@code
 * cat_incentive_criteria_product}). Blank optional attributes match any value.
 *
 * @param productCode risk code (mandatory)
 * @param coverTypeCode cover type, null for any
 * @param marketSegment market segment, null for any
 * @param sourceChannel source channel, null for any
 * @param insurerCode insurer party code, null for any
 */
@Embeddable
public record IncentiveScope(
    @Column(name = "product_code", nullable = false, length = 20) String productCode,
    @Column(name = "cover_type_code", length = 30) String coverTypeCode,
    @Column(name = "market_segment", length = 40) String marketSegment,
    @Column(name = "source_channel", length = 40) String sourceChannel,
    @Column(name = "insurer_code", length = 30) String insurerCode) {

  /**
   * Whether a transaction falls within this entry.
   *
   * @param product risk code
   * @param coverType cover type
   * @param segment market segment
   * @param channel source channel
   * @param insurer insurer party code
   * @return true when every given attribute matches
   */
  public boolean matches(
      String product, String coverType, String segment, String channel, String insurer) {
    return productCode.equals(product)
        && any(coverTypeCode, coverType)
        && any(marketSegment, segment)
        && any(sourceChannel, channel)
        && any(insurerCode, insurer);
  }

  private static boolean any(String criterion, String value) {
    return criterion == null || criterion.isBlank() || Objects.equals(criterion, value);
  }
}
