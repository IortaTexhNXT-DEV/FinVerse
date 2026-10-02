package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.SalesLevel;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit.UnitDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * New or changed sales unit.
 *
 * @param companyId company
 * @param level level (ignored on update)
 * @param code code (ignored on update)
 * @param name name
 * @param parentCode parent unit
 * @param costCenter default cost center
 */
public record SalesUnitRequest(
    @NotNull Long companyId,
    @NotNull SalesLevel level,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9_]+", message = "use A-Z, 0-9 and _")
        String code,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 20) String parentCode,
    @Size(max = 20) String costCenter) {

  /**
   * Unit attributes.
   *
   * @return details
   */
  public UnitDetails details() {
    return new UnitDetails(
        name.trim(),
        parentCode == null || parentCode.isBlank() ? null : parentCode,
        costCenter == null || costCenter.isBlank() ? null : costCenter);
  }
}
