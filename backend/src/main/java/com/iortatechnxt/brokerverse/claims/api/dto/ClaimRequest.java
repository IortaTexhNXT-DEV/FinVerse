package com.iortatechnxt.brokerverse.claims.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * First notification of loss (claim registration).
 *
 * @param companyId company
 * @param policyNo policy (or marine certificate) number
 * @param riskLineNo insured risk line of the policy, null when the whole policy is affected
 * @param lossDate date of loss (the policy must be in force)
 * @param reportedDate date the loss was notified
 * @param natureOfLoss nature of loss (e.g. Fire, Collision, Theft)
 * @param causeOfLoss cause of loss
 * @param lossLocation place of loss
 * @param description narrative
 * @param currency claim currency (must be the policy currency)
 * @param claimantCode claimant party, null for the policyholder
 * @param parties other involved parties
 * @param initialLossReserve initial loss estimate at 100 %, submitted for approval when positive
 * @param initialExpenseReserve initial expense estimate at 100 %, submitted when positive
 */
public record ClaimRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 40) String policyNo,
    @Positive Integer riskLineNo,
    @NotNull LocalDate lossDate,
    @NotNull LocalDate reportedDate,
    @NotBlank @Size(max = 60) String natureOfLoss,
    @NotBlank @Size(max = 120) String causeOfLoss,
    @NotBlank @Size(max = 200) String lossLocation,
    @NotBlank @Size(max = 1000) String description,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @Size(max = 30) String claimantCode,
    @Valid List<ClaimPartyRequest> parties,
    @PositiveOrZero BigDecimal initialLossReserve,
    @PositiveOrZero BigDecimal initialExpenseReserve) {

  /**
   * Involved parties, never null.
   *
   * @return parties
   */
  public List<ClaimPartyRequest> partiesOrEmpty() {
    return parties == null ? List.of() : parties;
  }
}
