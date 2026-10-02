package com.iortatechnxt.brokerverse.quotation.api.dto;

import com.iortatechnxt.brokerverse.quotation.domain.QuotationRequest;
import com.iortatechnxt.brokerverse.quotation.domain.RequestStatus;
import com.iortatechnxt.brokerverse.quotation.service.IncomingQuotationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request and response bodies of the quotation request inbox (BRNB.041). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class QuotationRequestDtos {

  private QuotationRequestDtos() {}

  /**
   * A request captured by Marketing from an e-mail.
   *
   * @param companyId company
   * @param channel source channel
   * @param externalRef reference in the source system
   * @param clientCode existing client or prospect code
   * @param prospectName prospect name when no client
   * @param prospectEmail prospect e-mail
   * @param prospectMobile prospect mobile
   * @param productCode requested product
   * @param marketSegment market segment
   * @param requestedCover requested cover
   */
  public record IntakeRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 40) String channel,
      @Size(max = 60) String externalRef,
      @Size(max = 30) String clientCode,
      @Size(max = 250) String prospectName,
      @Size(max = 120) String prospectEmail,
      @Size(max = 30) String prospectMobile,
      @Size(max = 20) String productCode,
      @Size(max = 40) String marketSegment,
      @NotBlank @Size(max = 2000) String requestedCover) {

    /**
     * As an incoming request.
     *
     * @return request
     */
    public IncomingQuotationRequest incoming() {
      return new IncomingQuotationRequest(
          channel,
          externalRef,
          null,
          clientCode,
          prospectName,
          prospectEmail,
          prospectMobile,
          productCode,
          marketSegment,
          requestedCover);
    }
  }

  /**
   * Reason for closing a request.
   *
   * @param reason reason
   */
  public record CloseRequest(@NotBlank @Size(max = 300) String reason) {}

  /**
   * A quotation request.
   *
   * @param id id
   * @param requestNo request number
   * @param channel source channel
   * @param externalRef reference in the source system
   * @param receivedAt received
   * @param clientId client
   * @param prospectName prospect name
   * @param prospectEmail prospect e-mail
   * @param prospectMobile prospect mobile
   * @param productCode requested product
   * @param marketSegment market segment
   * @param requestedCover requested cover
   * @param status status
   * @param quotationId quotation created
   * @param closeReason reason for closing
   * @param createdBy captured by
   */
  public record RequestResponse(
      Long id,
      String requestNo,
      String channel,
      String externalRef,
      Instant receivedAt,
      Long clientId,
      String prospectName,
      String prospectEmail,
      String prospectMobile,
      String productCode,
      String marketSegment,
      String requestedCover,
      RequestStatus status,
      Long quotationId,
      String closeReason,
      String createdBy) {

    /**
     * Maps a request.
     *
     * @param r request
     * @return response
     */
    public static RequestResponse from(QuotationRequest r) {
      return new RequestResponse(
          r.getId(),
          r.getRequestNo(),
          r.getChannel(),
          r.getExternalRef(),
          r.getReceivedAt(),
          r.getClientId(),
          r.getProspectName(),
          r.getProspectEmail(),
          r.getProspectMobile(),
          r.getProductCode(),
          r.getMarketSegment(),
          r.getRequestedCover(),
          r.getStatus(),
          r.getQuotationId(),
          r.getCloseReason(),
          r.getCreatedBy());
    }
  }
}
