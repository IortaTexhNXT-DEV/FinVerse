package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.ProductClass;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule.TsuCriteria;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * New or changed TSU routing rule (BRNB.098).
 *
 * @param code code (ignored on update)
 * @param description description
 * @param productClass product class
 * @param lineCode product line
 * @param minFleetUnits minimum vehicles
 * @param minLocations minimum locations
 * @param tsiAbove TSI threshold
 * @param endorsementType endorsement type
 * @param priority evaluation order
 */
public record TsuRuleRequest(
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9_]+", message = "use A-Z, 0-9 and _")
        String code,
    @NotBlank @Size(max = 200) String description,
    ProductClass productClass,
    String lineCode,
    @Positive Integer minFleetUnits,
    @Positive Integer minLocations,
    @DecimalMin("0") BigDecimal tsiAbove,
    @Size(max = 40) String endorsementType,
    @PositiveOrZero int priority) {

  /**
   * Criteria of the rule.
   *
   * @return criteria
   */
  public TsuCriteria criteria() {
    return new TsuCriteria(
        description.trim(),
        productClass,
        blankToNull(lineCode),
        minFleetUnits,
        minLocations,
        tsiAbove,
        blankToNull(endorsementType),
        priority);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
