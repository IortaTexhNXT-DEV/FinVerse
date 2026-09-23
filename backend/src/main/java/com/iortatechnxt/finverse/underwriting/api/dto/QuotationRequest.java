package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create / update (draft) quotation request.
 *
 * @param companyId company
 * @param branchId branch
 * @param productId product
 * @param customerCode client party code
 * @param insuredName insured name
 * @param sourceType channel
 * @param intermediaryCode agent / broker code
 * @param issueDate quotation date
 * @param validityDays validity in days
 * @param periodFrom proposed cover start
 * @param periodTo proposed cover end
 * @param currency currency
 * @param sharePct company share %
 * @param commissionRate brokerage / commission %
 * @param iteration figures (first iteration on create; ignored on update)
 */
public record QuotationRequest(
    @NotNull Long companyId,
    @NotNull Long branchId,
    @NotNull Long productId,
    @NotBlank @Size(max = 30) String customerCode,
    @NotBlank @Size(max = 200) String insuredName,
    @NotNull SourceType sourceType,
    @Size(max = 30) String intermediaryCode,
    @NotNull LocalDate issueDate,
    @Min(1) @Max(365) int validityDays,
    @NotNull LocalDate periodFrom,
    @NotNull LocalDate periodTo,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal sharePct,
    @DecimalMin("0") @DecimalMax("100") BigDecimal commissionRate,
    @Valid IterationRequest iteration) {}
