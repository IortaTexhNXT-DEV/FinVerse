package com.iortatechnxt.brokerverse.claims.api.dto;

import com.iortatechnxt.brokerverse.claims.domain.ClaimPartyRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A party to involve in a claim.
 *
 * @param role role
 * @param partyCode party code
 */
public record ClaimPartyRequest(
    @NotNull ClaimPartyRole role, @NotBlank @Size(max = 30) String partyCode) {}
