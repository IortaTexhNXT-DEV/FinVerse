package com.iortatechnxt.brokerverse.issuance.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Policy numbers confirmed at review (BRNB.074, one per policy year BRNB.112).
 *
 * @param policyNumbers policy numbers
 * @param issueDate issue date; today when empty
 */
public record ConfirmPolicyRequest(
    @NotEmpty @Size(max = 10) List<@Size(max = 60) String> policyNumbers, LocalDate issueDate) {}
