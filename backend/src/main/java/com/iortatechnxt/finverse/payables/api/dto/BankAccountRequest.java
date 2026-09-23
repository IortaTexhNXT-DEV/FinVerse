package com.iortatechnxt.finverse.payables.api.dto;

import com.iortatechnxt.finverse.payables.domain.NotificationFormat;
import com.iortatechnxt.finverse.payables.service.BankAccountCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update bank account request ({@code code} is immutable after creation).
 *
 * @param companyId company
 * @param code short code
 * @param name name
 * @param bankPartyCode bank party code (optional)
 * @param bankName bank name
 * @param accountNo account number
 * @param currency currency
 * @param glAccountCode bank GL account
 * @param pdcClearingAccountCode PDC issued clearing account (optional)
 * @param branchId owning branch (optional)
 * @param notificationFormat payment notification layout
 */
public record BankAccountRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9\\-]+") String code,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 30) String bankPartyCode,
    @NotBlank @Size(max = 120) String bankName,
    @NotBlank @Size(max = 40) String accountNo,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotBlank @Size(max = 30) String glAccountCode,
    @Size(max = 30) String pdcClearingAccountCode,
    Long branchId,
    NotificationFormat notificationFormat) {

  /**
   * Converts to the service command.
   *
   * @return command
   */
  public BankAccountCommand toCommand() {
    return new BankAccountCommand(
        companyId,
        code,
        name,
        bankPartyCode,
        bankName,
        accountNo,
        currency,
        glAccountCode,
        pdcClearingAccountCode,
        branchId,
        notificationFormat);
  }
}
