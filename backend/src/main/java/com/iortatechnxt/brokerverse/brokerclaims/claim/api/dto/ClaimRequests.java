package com.iortatechnxt.brokerverse.brokerclaims.claim.api.dto;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService.NewClaim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimSource;
import com.iortatechnxt.brokerverse.brokerclaims.domain.LossDetails;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.InsurerLineRequest;
import com.iortatechnxt.brokerverse.brokerclaims.location.api.dto.LocationDtos.LocationPickRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request bodies of the claim record (FR-CL-011/012/030/033). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ClaimRequests {

  private ClaimRequests() {}

  /**
   * Loss data of a claim (p.24-25). Blank mandatory fields are refused by the service with the FRS
   * messages.
   *
   * @param lossDate date of loss
   * @param reportedDate reported date (ignored on an amendment)
   * @param lossNature nature of loss
   * @param claimType claim type
   * @param lossDescription description
   * @param lossPlace place of loss
   * @param catastropheCode catastrophe code
   * @param catastropheEvent event name
   * @param claimAmount claim amount
   * @param deductible deductible
   * @param initialReserve initial loss reserve
   */
  public record LossRequest(
      LocalDate lossDate,
      LocalDate reportedDate,
      String lossNature,
      String claimType,
      @Size(max = 2000) String lossDescription,
      @Size(max = 500) String lossPlace,
      String catastropheCode,
      @Size(max = 200) String catastropheEvent,
      BigDecimal claimAmount,
      BigDecimal deductible,
      BigDecimal initialReserve) {

    /**
     * The loss data.
     *
     * @return loss
     */
    public LossDetails.Loss toLoss() {
      return new LossDetails.Loss(
          lossDate,
          reportedDate,
          lossNature,
          claimType,
          lossDescription,
          lossPlace,
          catastropheCode,
          catastropheEvent);
    }

    /**
     * The amounts.
     *
     * @return amounts
     */
    public LossDetails.Amounts toAmounts() {
      return new LossDetails.Amounts(claimAmount, deductible, initialReserve);
    }
  }

  /**
   * A claim to record.
   *
   * @param companyId company
   * @param arn cover, blank refused with "Select the cover of the claim"
   * @param policyYear policy year
   * @param source BDOI notice or insurer-reported
   * @param initialStatus first status (newly filed with or without complete documents)
   * @param loss loss data
   * @param locations locations of the cover
   * @param insurers insurer lines; the invoice shares when empty
   * @param confirmOutsidePeriod confirmation of a loss date outside the cover period
   * @param confirmReuse confirmation of an insurer claim number found on another claim
   */
  public record RecordClaimRequest(
      @NotNull Long companyId,
      String arn,
      int policyYear,
      ClaimSource source,
      String initialStatus,
      @NotNull @Valid LossRequest loss,
      List<@Valid LocationPickRequest> locations,
      List<@Valid InsurerLineRequest> insurers,
      boolean confirmOutsidePeriod,
      boolean confirmReuse) {

    /**
     * The service input.
     *
     * @return new claim
     */
    public NewClaim toClaim() {
      return new NewClaim(
          arn,
          policyYear < 1 ? 1 : policyYear,
          source,
          initialStatus,
          loss.toLoss(),
          loss.toAmounts(),
          locations == null
              ? List.of()
              : locations.stream().map(LocationPickRequest::toPick).toList(),
          insurers == null ? List.of() : insurers.stream().map(InsurerLineRequest::toLine).toList(),
          confirmOutsidePeriod,
          confirmReuse);
    }
  }

  /**
   * A reported date correction (FR-CL-012).
   *
   * @param reportedDate new date
   * @param reason reason ({@code BCL_OVERRIDE_REASON})
   * @param remark remark
   */
  public record ReportedDateRequest(
      @NotNull LocalDate reportedDate, String reason, @Size(max = 500) String remark) {}

  /**
   * A claimant override (FR-CL-030).
   *
   * @param claimantName claimant
   * @param reason reason ({@code BCL_OVERRIDE_REASON})
   */
  public record ClaimantRequest(@Size(max = 200) String claimantName, String reason) {}

  /**
   * The claims authorization code (FR-CL-016).
   *
   * @param evidenceAttachmentId insurer payment evidence of a direct-payment cover
   */
  public record AuthorizeRequest(Long evidenceAttachmentId) {}
}
