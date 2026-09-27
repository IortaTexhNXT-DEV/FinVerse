package com.iortatechnxt.brokerverse.eb.confirmation.api.dto;

import com.iortatechnxt.brokerverse.eb.confirmation.service.ConfirmationInput;
import com.iortatechnxt.brokerverse.eb.domain.EbClientConfirmation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the client confirmation API (FR-EB-046). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class ConfirmationDtos {

  private ConfirmationDtos() {}

  /**
   * The confirmation as recorded (JSON field of the multipart request).
   *
   * @param channel EMAIL or SIGNED_DOCUMENT
   * @param confirmedOn date
   * @param remarks remarks
   * @param choices chosen proposal per programme line
   */
  public record ConfirmationRequest(
      String channel,
      LocalDate confirmedOn,
      String remarks,
      List<ConfirmationInput.Choice> choices) {}

  /**
   * A reason.
   *
   * @param reason why
   */
  public record VoidRequest(String reason) {}

  /**
   * A confirmation.
   *
   * @param id id
   * @param cycleId cycle
   * @param comparativeId comparative presented
   * @param channel channel
   * @param confirmedOn date
   * @param evidenceAttachmentId evidence
   * @param status ACTIVE or VOIDED
   * @param voidReason why voided
   * @param remarks remarks
   * @param recordedBy user
   * @param recordedAt time
   * @param lines chosen proposals
   */
  public record ConfirmationResponse(
      Long id,
      Long cycleId,
      Long comparativeId,
      String channel,
      LocalDate confirmedOn,
      Long evidenceAttachmentId,
      String status,
      String voidReason,
      String remarks,
      String recordedBy,
      Instant recordedAt,
      List<LineResponse> lines) {

    /**
     * Maps a confirmation.
     *
     * @param c confirmation
     * @return response
     */
    public static ConfirmationResponse from(EbClientConfirmation c) {
      return new ConfirmationResponse(
          c.getId(),
          c.getCycleId(),
          c.getComparativeId(),
          c.getChannel(),
          c.getConfirmedOn(),
          c.getEvidenceAttachmentId(),
          c.getStatus(),
          c.getVoidReason(),
          c.getRemarks(),
          c.getCreatedBy(),
          c.getCreatedAt(),
          c.getLines().stream().map(LineResponse::from).toList());
    }
  }

  /**
   * A confirmed line.
   *
   * @param lineNo programme line
   * @param benefitLine benefit line
   * @param proposalId chosen proposal
   * @param insurerCode insurer
   * @param annualPremium annual premium
   * @param sumInsured TSI
   * @param accountArn account created at placement
   */
  public record LineResponse(
      int lineNo,
      String benefitLine,
      Long proposalId,
      String insurerCode,
      BigDecimal annualPremium,
      BigDecimal sumInsured,
      String accountArn) {

    static LineResponse from(EbClientConfirmation.Line l) {
      return new LineResponse(
          l.getLineNo(),
          l.getBenefitLine(),
          l.getProposalId(),
          l.getInsurerCode(),
          l.getAnnualPremium(),
          l.getSumInsured(),
          l.getAccountArn());
    }
  }
}
