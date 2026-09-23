package com.iortatechnxt.finverse.receivables.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * New deposit slip.
 *
 * @param companyId company
 * @param branchId branch
 * @param bankAccountCode GL bank account the money is deposited in
 * @param slipDate slip date
 * @param receiptIds receipts to deposit (approved, undeposited, same bank account and currency)
 */
public record DepositSlipRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotBlank @Size(max = 30) String bankAccountCode,
    @NotNull LocalDate slipDate,
    @NotEmpty List<Long> receiptIds) {

  /** Canonical constructor copying the list. */
  public DepositSlipRequest {
    receiptIds = receiptIds == null ? List.of() : List.copyOf(receiptIds);
  }
}
