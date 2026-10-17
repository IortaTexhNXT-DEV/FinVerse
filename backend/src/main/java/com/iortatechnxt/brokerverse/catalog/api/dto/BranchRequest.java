package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch.BranchDetails;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * New or changed insurer branch.
 *
 * @param code branch code (ignored on update)
 * @param name name
 * @param city city
 * @param lgtRate LGT rate %
 * @param placementEmail placement mailbox
 */
public record BranchRequest(
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9_]+", message = "use A-Z, 0-9 and _")
        String code,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 80) String city,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal lgtRate,
    @Email @Size(max = 120) String placementEmail) {

  /**
   * Branch attributes.
   *
   * @return details
   */
  public BranchDetails details() {
    return new BranchDetails(name.trim(), city, lgtRate, placementEmail);
  }
}
