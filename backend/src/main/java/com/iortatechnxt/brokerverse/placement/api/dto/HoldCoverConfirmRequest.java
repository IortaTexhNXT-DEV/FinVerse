package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The insurer's hold cover confirmation (BRNB.103).
 *
 * @param insurerCode insurer that confirmed; the requested insurer when blank
 * @param reference insurer reference
 * @param confirmedOn confirmation date; today when empty
 * @param expiryDate expiry confirmed by the insurer; the requested expiry when empty
 */
public record HoldCoverConfirmRequest(
    @Size(max = 30) String insurerCode,
    @NotBlank @Size(max = 60) String reference,
    LocalDate confirmedOn,
    LocalDate expiryDate) {}
