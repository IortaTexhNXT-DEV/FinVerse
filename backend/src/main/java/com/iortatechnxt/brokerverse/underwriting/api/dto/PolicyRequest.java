package com.iortatechnxt.brokerverse.underwriting.api.dto;

import com.iortatechnxt.brokerverse.underwriting.domain.BusinessType;
import com.iortatechnxt.brokerverse.underwriting.domain.SourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create / update (draft) policy request, also used for the premium preview.
 *
 * @param companyId company
 * @param branchId issuing branch
 * @param productId product
 * @param customerCode client party code
 * @param insuredName insured name
 * @param sourceType DIRECT / AGENT / BROKER
 * @param intermediaryCode agent or broker party code
 * @param issueDate issue date
 * @param periodFrom cover start
 * @param periodTo cover end
 * @param currency currency
 * @param businessType DIRECT / DIRECT_WITH_COINSURANCE
 * @param sharePct company share % (100 for direct business)
 * @param coinsurerCode coinsurer party code
 * @param coinsuranceLeader company leads the coinsurance (bills 100 %)
 * @param discountRate discount %
 * @param loadingRate loading %
 * @param commissionRate commission % override (default: intermediary, then product rate)
 * @param risks risks
 */
public record PolicyRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotNull Long productId,
    @NotBlank @Size(max = 30) String customerCode,
    @NotBlank @Size(max = 200) String insuredName,
    @NotNull SourceType sourceType,
    @Size(max = 30) String intermediaryCode,
    @NotNull LocalDate issueDate,
    @NotNull LocalDate periodFrom,
    @NotNull LocalDate periodTo,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull BusinessType businessType,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal sharePct,
    @Size(max = 30) String coinsurerCode,
    boolean coinsuranceLeader,
    @DecimalMin("0") @DecimalMax("100") BigDecimal discountRate,
    @DecimalMin("0") @DecimalMax("100") BigDecimal loadingRate,
    @DecimalMin("0") @DecimalMax("100") BigDecimal commissionRate,
    @NotEmpty @Valid List<RiskRequest> risks) {}
