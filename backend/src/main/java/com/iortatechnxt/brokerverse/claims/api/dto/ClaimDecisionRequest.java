package com.iortatechnxt.brokerverse.claims.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Close, repudiate or withdraw decision.
 *
 * @param reason reason (mandatory)
 * @param accountingDate date of the reserve release; default today
 */
public record ClaimDecisionRequest(
    @NotBlank @Size(max = 200) String reason, LocalDate accountingDate) {}
