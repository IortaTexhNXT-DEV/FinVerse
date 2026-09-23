package com.iortatechnxt.finverse.receivables.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Manual reconciliation match of book entries with bank statement lines of equal total.
 *
 * @param companyId company
 * @param bankAccountCode GL bank account
 * @param ledgerEntryIds ledger entries on the bank account
 * @param statementLineIds bank statement lines
 */
public record ManualMatchRequest(
    @NotNull Long companyId,
    @NotBlank String bankAccountCode,
    @NotEmpty List<Long> ledgerEntryIds,
    @NotEmpty List<Long> statementLineIds) {

  /** Canonical constructor copying the lists. */
  public ManualMatchRequest {
    ledgerEntryIds = ledgerEntryIds == null ? List.of() : List.copyOf(ledgerEntryIds);
    statementLineIds = statementLineIds == null ? List.of() : List.copyOf(statementLineIds);
  }
}
