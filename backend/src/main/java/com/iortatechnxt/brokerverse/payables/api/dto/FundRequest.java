package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.payables.service.FundCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Create / update petty cash fund request.
 *
 * @param companyId company
 * @param branchId branch
 * @param code code (immutable)
 * @param name name
 * @param custodian custodian
 * @param glAccountCode petty cash GL account
 * @param replenishBankAccountId replenishment bank account
 * @param imprestAmount imprest amount
 */
public record FundRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9\\-]+") String code,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 120) String custodian,
    @NotBlank @Size(max = 30) String glAccountCode,
    @NotNull Long replenishBankAccountId,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal imprestAmount) {

  /**
   * Converts to the service command.
   *
   * @return command
   */
  public FundCommand toCommand() {
    return new FundCommand(
        companyId,
        branchId,
        code,
        name,
        custodian,
        glAccountCode,
        replenishBankAccountId,
        imprestAmount);
  }
}
