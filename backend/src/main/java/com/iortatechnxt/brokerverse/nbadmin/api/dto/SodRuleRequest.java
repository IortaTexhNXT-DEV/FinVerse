package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * New separation-of-duties rule.
 *
 * @param profileA first group profile
 * @param profileB second group profile
 * @param description why one user may not hold both
 */
public record SodRuleRequest(
    @NotBlank @Size(max = 40) String profileA,
    @NotBlank @Size(max = 40) String profileB,
    @NotBlank @Size(max = 500) String description) {}
