package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.AutoBookRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * An auto-book rule (BRNB.076), request and response.
 *
 * @param id id (ignored on input)
 * @param productCode product, blank for any
 * @param marketSegment segment, blank for any
 * @param enabled enabled
 * @param description description
 */
public record AutoBookRuleDto(
    Long id,
    @Size(max = 20) String productCode,
    @Size(max = 40) String marketSegment,
    boolean enabled,
    @NotBlank @Size(max = 300) String description) {

  /**
   * Maps a rule.
   *
   * @param r rule
   * @return DTO
   */
  public static AutoBookRuleDto from(AutoBookRule r) {
    return new AutoBookRuleDto(
        r.getId(), r.getProductCode(), r.getMarketSegment(), r.isEnabled(), r.getDescription());
  }

  /**
   * The criteria.
   *
   * @return criteria
   */
  public AutoBookRule.Criteria toCriteria() {
    return new AutoBookRule.Criteria(productCode, marketSegment, enabled, description);
  }
}
