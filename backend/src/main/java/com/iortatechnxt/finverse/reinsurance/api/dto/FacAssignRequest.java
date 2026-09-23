package com.iortatechnxt.finverse.reinsurance.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Participants of a facultative slip.
 *
 * @param participants reinsurers, shares of the facultative requirement and commission
 * @param remarks slip remarks
 */
public record FacAssignRequest(
    @NotEmpty @Valid List<Line> participants, @Size(max = 300) String remarks) {

  /** Canonical constructor copying the lines. */
  public FacAssignRequest {
    participants = participants == null ? List.of() : List.copyOf(participants);
  }

  /**
   * One reinsurer's line.
   *
   * @param reinsurerCode reinsurer party code
   * @param sharePct share of the facultative requirement %
   * @param commissionPct commission %
   */
  public record Line(
      @NotBlank @Size(max = 20) String reinsurerCode,
      @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal sharePct,
      @DecimalMin("0") @DecimalMax("100") BigDecimal commissionPct) {}
}
