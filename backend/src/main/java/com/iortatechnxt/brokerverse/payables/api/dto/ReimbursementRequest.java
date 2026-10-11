package com.iortatechnxt.brokerverse.payables.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Reimbursement claim request.
 *
 * @param date claim date
 * @param disbursementIds vouchers to claim (empty = all unclaimed approved vouchers)
 * @param narration narration
 */
public record ReimbursementRequest(
    @NotNull LocalDate date,
    @Size(max = 500) List<Long> disbursementIds,
    @Size(max = 200) String narration) {

  /** Canonical constructor copying the ids. */
  public ReimbursementRequest {
    disbursementIds = disbursementIds == null ? List.of() : List.copyOf(disbursementIds);
  }
}
