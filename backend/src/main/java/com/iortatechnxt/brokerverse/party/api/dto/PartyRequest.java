package com.iortatechnxt.brokerverse.party.api.dto;

import com.iortatechnxt.brokerverse.party.domain.PartyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Create / update party request. {@code companyId}, {@code code} and {@code partyType} are
 * immutable after creation.
 *
 * @param companyId company
 * @param code party code
 * @param name name
 * @param partyType type
 * @param taxId tax identification number
 * @param address address
 * @param email email
 * @param phone phone
 * @param defaultCurrency default currency
 * @param creditDays credit terms in days
 * @param commissionRate default commission % (intermediaries)
 * @param withholdingTaxRate withholding tax %
 * @param licenceNo Insurance Commission licence no. (intermediaries)
 * @param bankName bank name
 * @param bankAccountNo bank account number
 * @param branchId servicing branch
 */
public record PartyRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9\\-]+") String code,
    @NotBlank @Size(max = 200) String name,
    @NotNull PartyType partyType,
    @Size(max = 30) String taxId,
    @Size(max = 300) String address,
    @Email @Size(max = 120) String email,
    @Size(max = 40) String phone,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String defaultCurrency,
    @Min(0) @Max(365) int creditDays,
    @DecimalMin("0") @DecimalMax("100") BigDecimal commissionRate,
    @DecimalMin("0") @DecimalMax("100") BigDecimal withholdingTaxRate,
    @Size(max = 40) String licenceNo,
    @Size(max = 120) String bankName,
    @Size(max = 40) String bankAccountNo,
    Long branchId) {}
