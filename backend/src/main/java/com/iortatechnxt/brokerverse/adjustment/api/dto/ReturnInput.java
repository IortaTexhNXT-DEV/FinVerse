package com.iortatechnxt.brokerverse.adjustment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Requests to return with a reason (ADJID.005/007).
 *
 * @param ids requests
 * @param reasonCode reason (list ADJ_RETURN_REASON)
 * @param comment comment, may be null
 */
public record ReturnInput(
    @NotEmpty List<Long> ids,
    @NotBlank @Size(max = 40) String reasonCode,
    @Size(max = 1000) String comment) {

  /** Defensive copy. */
  public ReturnInput {
    ids = ids == null ? null : List.copyOf(ids);
  }
}
