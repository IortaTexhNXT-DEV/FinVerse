package com.iortatechnxt.brokerverse.reinsurance.api.dto;

import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyTerms;
import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create / update treaty request. {@code companyId} and {@code code} are immutable.
 *
 * @param companyId company
 * @param code treaty code
 * @param name name
 * @param treatyType QUOTA_SHARE, SURPLUS or XOL
 * @param businessLine line of business (BUSINESS_LINE dimension)
 * @param uwYear underwriting year of the programme
 * @param periodFrom period start
 * @param periodTo period end
 * @param currency statement currency (company base currency)
 * @param quotaSharePct quota share % (quota share)
 * @param treatyLimit quota share limit per risk, blank = unlimited (quota share)
 * @param retentionLimit retention line per risk (surplus)
 * @param lines number of lines (surplus)
 * @param levyPct premium tax / levy %
 * @param reserveInterestPct yearly interest on reserves withheld %
 * @param lossReservePct outstanding loss reserve withheld %
 * @param brokerCode reinsurance broker party code, blank when placed direct
 * @param participants reinsurers and shares
 * @param layers excess of loss layers
 */
public record TreatyRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Z0-9\\-]+") String code,
    @NotBlank @Size(max = 120) String name,
    @NotNull TreatyType treatyType,
    @NotBlank @Size(max = 20) String businessLine,
    @NotNull @Min(2000) @Max(2100) Integer uwYear,
    @NotNull LocalDate periodFrom,
    @NotNull LocalDate periodTo,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @DecimalMin("0") @DecimalMax("100") BigDecimal quotaSharePct,
    @Positive BigDecimal treatyLimit,
    @Positive BigDecimal retentionLimit,
    @Min(1) @Max(50) Integer lines,
    @DecimalMin("0") @DecimalMax("100") BigDecimal levyPct,
    @DecimalMin("0") @DecimalMax("100") BigDecimal reserveInterestPct,
    @DecimalMin("0") @DecimalMax("100") BigDecimal lossReservePct,
    @Size(max = 20) String brokerCode,
    @NotEmpty @Valid List<ParticipantRequest> participants,
    @Valid List<LayerRequest> layers) {

  /** Canonical constructor copying the lists. */
  public TreatyRequest {
    participants = participants == null ? List.of() : List.copyOf(participants);
    layers = layers == null ? List.of() : List.copyOf(layers);
  }

  /**
   * Domain terms.
   *
   * @return terms
   */
  public TreatyTerms toTerms() {
    return new TreatyTerms(
        name,
        treatyType,
        businessLine,
        uwYear,
        periodFrom,
        periodTo,
        currency,
        quotaSharePct,
        treatyLimit,
        retentionLimit,
        lines,
        levyPct,
        reserveInterestPct,
        lossReservePct);
  }

  /**
   * A reinsurer's participation.
   *
   * @param reinsurerCode reinsurer party code
   * @param sharePct share %
   * @param commissionPct ceding commission %
   * @param profitCommissionPct profit commission %
   * @param premiumReservePct premium reserve retained %
   */
  public record ParticipantRequest(
      @NotBlank @Size(max = 20) String reinsurerCode,
      @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal sharePct,
      @DecimalMin("0") @DecimalMax("100") BigDecimal commissionPct,
      @DecimalMin("0") @DecimalMax("100") BigDecimal profitCommissionPct,
      @DecimalMin("0") @DecimalMax("100") BigDecimal premiumReservePct) {}

  /**
   * An excess of loss layer.
   *
   * @param priority priority (deductible) per claim
   * @param limit limit per claim
   * @param minDepositPremium minimum and deposit premium
   * @param reinstatements number of reinstatements
   */
  public record LayerRequest(
      @NotNull @DecimalMin("0") BigDecimal priority,
      @NotNull @Positive BigDecimal limit,
      @DecimalMin("0") BigDecimal minDepositPremium,
      @Min(0) @Max(10) Integer reinstatements) {}
}
