package com.iortatechnxt.brokerverse.fixedasset.api.dto;

import com.iortatechnxt.brokerverse.fixedasset.domain.DepreciationMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Register (or, while pending, edit) a fixed asset. On update only the descriptive fields
 * (description, cost centre, supplier, location, custodian, settlement account) change.
 *
 * @param companyId company
 * @param branchId owning branch
 * @param categoryId category
 * @param tagNo asset tag number
 * @param description description
 * @param costCenter cost centre charged with the depreciation
 * @param supplierCode supplier party code
 * @param acquisitionDate acquisition (available for use) date
 * @param acquisitionCost cost
 * @param depreciationMethod method (defaults to the category's)
 * @param usefulLifeMonths useful life (defaults to the category's)
 * @param location location
 * @param custodian custodian
 * @param settlementAccount account credited on capitalization (bank or supplier payable); not used
 *     for take-on
 * @param takeOn true for an existing asset brought over from a previous register
 * @param capitalizationDate value date of the capitalization (take-on date for take-on assets;
 *     defaults to the acquisition date)
 * @param openingAccumulatedDepreciation take-on accumulated depreciation (computed when blank)
 * @param openingMonths take-on months already depreciated (computed when blank)
 */
public record FixedAssetRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotNull Long categoryId,
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9\\-/]+") String tagNo,
    @NotBlank @Size(max = 200) String description,
    @NotBlank @Size(max = 20) String costCenter,
    @Size(max = 30) String supplierCode,
    @NotNull LocalDate acquisitionDate,
    @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal acquisitionCost,
    DepreciationMethod depreciationMethod,
    @Min(1) @Max(1200) Integer usefulLifeMonths,
    @Size(max = 120) String location,
    @Size(max = 120) String custodian,
    @Size(max = 30) String settlementAccount,
    boolean takeOn,
    LocalDate capitalizationDate,
    @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal openingAccumulatedDepreciation,
    @Min(0) Integer openingMonths) {}
