package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.service.InsurerPageService.LinkedProduct;
import java.util.List;

/**
 * An insurer with its branches (LGT) and commission rates.
 *
 * @param insurer profile
 * @param branches branches
 * @param commissions commission rates
 * @param contacts contact persons
 * @param bankAccounts bank accounts
 * @param products products linked to the insurer
 */
public record InsurerDetailResponse(
    InsurerResponse insurer,
    List<BranchResponse> branches,
    List<CommissionResponse> commissions,
    List<InsurerPageDtos.ContactResponse> contacts,
    List<InsurerPageDtos.BankAccountResponse> bankAccounts,
    List<LinkedProduct> products) {}
