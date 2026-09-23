package com.iortatechnxt.finverse.consolidation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creates an inter-company relationship (company pair and their due-from / due-to accounts).
 *
 * @param companyAId first company
 * @param aDueFromAccount first company's receivable from the second
 * @param aDueToAccount first company's payable to the second
 * @param companyBId second company
 * @param bDueFromAccount second company's receivable from the first
 * @param bDueToAccount second company's payable to the first
 */
public record RelationshipRequest(
    @NotNull Long companyAId,
    @NotBlank @Size(max = 30) String aDueFromAccount,
    @NotBlank @Size(max = 30) String aDueToAccount,
    @NotNull Long companyBId,
    @NotBlank @Size(max = 30) String bDueFromAccount,
    @NotBlank @Size(max = 30) String bDueToAccount) {}
