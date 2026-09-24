package com.iortatechnxt.brokerverse.lov.api.dto;

import com.iortatechnxt.brokerverse.lov.domain.LovDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * New or changed list value.
 *
 * @param code code (ignored on update)
 * @param label label
 * @param sortOrder order
 * @param parentCode optional parent value
 * @param effectiveFrom first valid date
 * @param effectiveTo optional last valid date
 */
public record LovValueRequest(
    @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Z0-9_]+", message = "use A-Z, 0-9 and _")
        String code,
    @NotBlank @Size(max = 200) String label,
    @PositiveOrZero int sortOrder,
    @Size(max = 40) String parentCode,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo) {

  /**
   * Maintainable attributes.
   *
   * @return details
   */
  public LovDetails details() {
    String parent = parentCode == null || parentCode.isBlank() ? null : parentCode;
    return new LovDetails(label.trim(), sortOrder, parent, effectiveFrom, effectiveTo);
  }
}
