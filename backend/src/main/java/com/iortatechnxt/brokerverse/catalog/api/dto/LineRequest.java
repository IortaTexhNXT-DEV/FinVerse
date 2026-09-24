package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.ProductLine.LineDetails;
import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * New or changed product line.
 *
 * @param code code (ignored on update)
 * @param name name
 * @param riskItemKind kind of risk items
 * @param ratingMethod Appendix A formula
 * @param sortOrder display order
 */
public record LineRequest(
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9_]+", message = "use A-Z, 0-9 and _")
        String code,
    @NotBlank @Size(max = 120) String name,
    @NotNull RiskItemKind riskItemKind,
    @NotNull RatingMethod ratingMethod,
    @PositiveOrZero int sortOrder) {

  /**
   * Maintainable attributes.
   *
   * @return details
   */
  public LineDetails details() {
    return new LineDetails(name.trim(), riskItemKind, ratingMethod, sortOrder);
  }
}
