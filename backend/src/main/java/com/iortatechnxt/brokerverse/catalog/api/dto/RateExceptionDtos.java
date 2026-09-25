package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.RateOverride;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery.Purpose;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Request and response bodies of rate-scheme exceptions (BRPM.007). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class RateExceptionDtos {

  private RateExceptionDtos() {}

  /**
   * A request to price one transaction on another version or rate.
   *
   * @param productCode risk code
   * @param purpose purpose, NEW_BUSINESS when empty
   * @param requestedVersionNo non-current version wanted
   * @param requestedRate item rate wanted in percent
   * @param transactionRef quotation number or ARN
   * @param reason justification
   * @param validUntil last day it may be used, 30 days when empty
   */
  public record ExceptionRequest(
      @NotBlank @Size(max = 20) String productCode,
      Purpose purpose,
      @Positive Integer requestedVersionNo,
      @DecimalMin("0") @DecimalMax("100") BigDecimal requestedRate,
      @NotBlank @Size(max = 40) String transactionRef,
      @NotBlank @Size(max = 1000) String reason,
      LocalDate validUntil) {

    /**
     * The domain request.
     *
     * @return request
     */
    public RateOverride.Request toRequest() {
      return new RateOverride.Request(
          productCode.strip(),
          purpose == null ? Purpose.NEW_BUSINESS.name() : purpose.name(),
          requestedVersionNo,
          requestedRate,
          transactionRef.strip(),
          reason.strip(),
          validUntil);
    }
  }

  /**
   * A rate-scheme exception.
   *
   * @param id id
   * @param referenceNo reference (the rate override reference)
   * @param productCode risk code
   * @param purpose purpose
   * @param requestedVersionNo version
   * @param requestedRate rate
   * @param transactionRef quotation number or ARN
   * @param reason justification
   * @param validUntil last day it may be used
   * @param recordStatus PENDING_AUTHORIZATION, ACTIVE (approved) or INACTIVE (rejected)
   * @param requestedBy requester
   * @param requestedAt when
   * @param authorizedBy approver
   */
  public record ExceptionResponse(
      Long id,
      String referenceNo,
      String productCode,
      String purpose,
      Integer requestedVersionNo,
      BigDecimal requestedRate,
      String transactionRef,
      String reason,
      LocalDate validUntil,
      RecordStatus recordStatus,
      String requestedBy,
      Instant requestedAt,
      String authorizedBy) {

    /**
     * Maps an entity.
     *
     * @param e entity
     * @return response
     */
    public static ExceptionResponse from(RateOverride e) {
      return new ExceptionResponse(
          e.getId(),
          e.getReferenceNo(),
          e.getProductCode(),
          e.getPurpose(),
          e.getRequestedVersionNo(),
          e.getRequestedRate(),
          e.getTransactionRef(),
          e.getReason(),
          e.getValidUntil(),
          e.getRecordStatus(),
          e.getCreatedBy(),
          e.getCreatedAt(),
          e.getAuthorizedBy());
    }
  }
}
