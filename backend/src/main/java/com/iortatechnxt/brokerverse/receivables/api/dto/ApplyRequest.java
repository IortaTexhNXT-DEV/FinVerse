package com.iortatechnxt.brokerverse.receivables.api.dto;

import com.iortatechnxt.brokerverse.receivables.domain.AllocationMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Application of money held on account to debit notes.
 *
 * @param date application date
 * @param method MANUAL (use allocations) or FIFO
 * @param allocations manual allocations
 */
public record ApplyRequest(
    @NotNull LocalDate date,
    @NotNull AllocationMethod method,
    @Valid List<AllocationRequest> allocations) {

  /** Canonical constructor normalising the allocation list. */
  public ApplyRequest {
    allocations = allocations == null ? List.of() : List.copyOf(allocations);
  }
}
