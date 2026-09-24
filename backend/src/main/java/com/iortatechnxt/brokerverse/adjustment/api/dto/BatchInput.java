package com.iortatechnxt.brokerverse.adjustment.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Requests to post as one batch (ADJID.006).
 *
 * @param companyId company
 * @param ids requests
 * @param remarks remarks, may be null
 */
public record BatchInput(
    @NotNull Long companyId, @NotEmpty List<Long> ids, @Size(max = 500) String remarks) {

  /** Defensive copy. */
  public BatchInput {
    ids = ids == null ? null : List.copyOf(ids);
  }
}
