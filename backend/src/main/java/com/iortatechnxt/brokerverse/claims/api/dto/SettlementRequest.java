package com.iortatechnxt.brokerverse.claims.api.dto;

import com.iortatechnxt.brokerverse.claims.domain.CostType;
import com.iortatechnxt.brokerverse.claims.domain.SettlementType;
import com.iortatechnxt.brokerverse.claims.service.SettlementCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * New settlement, at 100 %.
 *
 * @param payeeCode party to pay
 * @param costType loss or expense
 * @param settlementType partial or final
 * @param assessedAmount assessed amount before deductions
 * @param deductible policy deductible
 * @param excess excess
 * @param narration narration
 */
public record SettlementRequest(
    @NotBlank @Size(max = 30) String payeeCode,
    @NotNull CostType costType,
    @NotNull SettlementType settlementType,
    @NotNull @Positive BigDecimal assessedAmount,
    @PositiveOrZero BigDecimal deductible,
    @PositiveOrZero BigDecimal excess,
    @NotBlank @Size(max = 300) String narration) {

  /**
   * Service command.
   *
   * @return command
   */
  public SettlementCommand toCommand() {
    return new SettlementCommand(
        payeeCode, costType, settlementType, assessedAmount, deductible, excess, narration);
  }
}
