package com.iortatechnxt.brokerverse.underwriting.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Marine open cover request.
 *
 * @param companyId company
 * @param branchId branch
 * @param productId marine product allowing open covers
 * @param customerCode client code
 * @param insuredName insured name
 * @param periodFrom start
 * @param periodTo end
 * @param currency currency
 * @param limitPerShipment limit per shipment
 * @param annualLimit total declarations limit
 * @param rate premium rate %
 * @param cargoDescription goods covered
 */
public record OpenCoverRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotNull Long productId,
    @NotBlank @Size(max = 30) String customerCode,
    @NotBlank @Size(max = 200) String insuredName,
    @NotNull LocalDate periodFrom,
    @NotNull LocalDate periodTo,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal limitPerShipment,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal annualLimit,
    @NotNull @DecimalMin("0") BigDecimal rate,
    @Size(max = 300) String cargoDescription) {}
