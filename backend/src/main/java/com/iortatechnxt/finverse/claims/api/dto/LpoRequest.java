package com.iortatechnxt.finverse.claims.api.dto;

import com.iortatechnxt.finverse.claims.domain.LpoCover;
import com.iortatechnxt.finverse.claims.service.LpoCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * New local purchase order.
 *
 * @param garageCode garage party
 * @param cover own damage or third party
 * @param issueDate issue date, null for today
 * @param gross gross repair amount
 * @param discount garage discount
 * @param description repair description
 */
public record LpoRequest(
    @NotBlank @Size(max = 30) String garageCode,
    @NotNull LpoCover cover,
    LocalDate issueDate,
    @NotNull @Positive BigDecimal gross,
    @PositiveOrZero BigDecimal discount,
    @NotBlank @Size(max = 300) String description) {

  /**
   * Service command.
   *
   * @return command
   */
  public LpoCommand toCommand() {
    return new LpoCommand(garageCode, cover, issueDate, gross, discount, description);
  }
}
