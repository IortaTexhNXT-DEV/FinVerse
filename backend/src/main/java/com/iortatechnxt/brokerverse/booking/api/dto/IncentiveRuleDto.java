package com.iortatechnxt.brokerverse.booking.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.IncentiveRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * An incentive eligibility rule (BRNB.107), request and response.
 *
 * @param id id (ignored on input)
 * @param productCode product, blank for any
 * @param marketSegment segment, blank for any
 * @param sourceChannel channel, blank for any
 * @param periodFrom first booking date covered
 * @param periodTo last booking date covered, null for open-ended
 * @param active active
 * @param description description
 */
public record IncentiveRuleDto(
    Long id,
    @Size(max = 20) String productCode,
    @Size(max = 40) String marketSegment,
    @Size(max = 40) String sourceChannel,
    @NotNull LocalDate periodFrom,
    LocalDate periodTo,
    boolean active,
    @NotBlank @Size(max = 300) String description) {

  /**
   * Maps a rule.
   *
   * @param r rule
   * @return DTO
   */
  public static IncentiveRuleDto from(IncentiveRule r) {
    return new IncentiveRuleDto(
        r.getId(),
        r.getProductCode(),
        r.getMarketSegment(),
        r.getSourceChannel(),
        r.getPeriodFrom(),
        r.getPeriodTo(),
        r.isActive(),
        r.getDescription());
  }

  /**
   * The criteria.
   *
   * @return criteria
   */
  public IncentiveRule.Criteria toCriteria() {
    return new IncentiveRule.Criteria(
        productCode, marketSegment, sourceChannel, periodFrom, periodTo, active, description);
  }
}
