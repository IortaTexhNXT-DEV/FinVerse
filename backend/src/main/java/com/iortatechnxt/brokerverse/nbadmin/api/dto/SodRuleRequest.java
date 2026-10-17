package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * New separation-of-duties rule.
 *
 * @param profileA first group profile (or permission)
 * @param profileB second group profile (or permission)
 * @param description why one user may not hold both
 * @param kind PROFILES (default) or PERMISSIONS (conflicting permission combination)
 */
public record SodRuleRequest(
    @NotBlank @Size(max = 40) String profileA,
    @NotBlank @Size(max = 40) String profileB,
    @NotBlank @Size(max = 500) String description,
    SodRuleKind kind) {}
