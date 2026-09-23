package com.iortatechnxt.finverse.claims.api.dto;

import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.EstimateLine;
import com.iortatechnxt.finverse.claims.domain.EstimateSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request for a new estimate of one side and cost type.
 *
 * @param side payment or recovery
 * @param costType loss or expense (recoveries: loss)
 * @param newEstimate new estimate at 100 %
 * @param reason reason for the change
 */
public record ReserveRequest(
    @NotNull EstimateSide side,
    @NotNull CostType costType,
    @NotNull @PositiveOrZero BigDecimal newEstimate,
    @NotBlank @Size(max = 200) String reason) {

  /**
   * Estimate line of the request.
   *
   * @return line
   */
  public EstimateLine toLine() {
    return new EstimateLine(side, costType, newEstimate);
  }
}
