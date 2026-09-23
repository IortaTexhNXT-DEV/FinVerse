package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.underwriting.domain.EndorsementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Endorsement request. Amounts are at 100 % and entered positive; the endorsement type gives the
 * sign (REFUND returns premium). CANCELLATION computes the pro-rata (1/365) return premium itself;
 * RENEWAL takes the renewal premium (default: the original gross premium) and the new period.
 *
 * @param type endorsement type
 * @param issueDate issue date
 * @param effectiveDate effective date of the change
 * @param description description of the change
 * @param grossPremium additional / return / renewal gross premium at 100 %
 * @param sumInsuredChange change in sum insured at 100 % (renewal: new sum insured)
 * @param newPeriodFrom renewed period start (renewal)
 * @param newPeriodTo renewed period end (renewal)
 */
public record EndorsementRequest(
    @NotNull EndorsementType type,
    @NotNull LocalDate issueDate,
    @NotNull LocalDate effectiveDate,
    @NotBlank @Size(max = 500) String description,
    @DecimalMin("0") BigDecimal grossPremium,
    @DecimalMin("0") BigDecimal sumInsuredChange,
    LocalDate newPeriodFrom,
    LocalDate newPeriodTo) {}
