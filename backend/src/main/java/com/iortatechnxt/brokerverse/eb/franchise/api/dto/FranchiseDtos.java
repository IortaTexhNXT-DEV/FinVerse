package com.iortatechnxt.brokerverse.eb.franchise.api.dto;

import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the franchise API (FR-EB-032, 033). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class FranchiseDtos {

  private FranchiseDtos() {}

  /**
   * Insurers to request the franchise from.
   *
   * @param insurerCodes insurers
   */
  public record InsurersRequest(List<String> insurerCodes) {}

  /**
   * A franchise request.
   *
   * @param id id
   * @param franchiseNo number
   * @param cycleId cycle
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param status status
   * @param decision APPROVED or REJECTED, null before the decision
   * @param submittedAt sent at
   * @param dueDate decision due
   * @param decidedOn date of the insurer's reply
   * @param decidedBy user who recorded it
   * @param reasonCode rejection reason
   * @param remarks remarks
   * @param evidenceAttachmentId the insurer's reply
   * @param adviceDueDate client advice due
   * @param advisedAt client advised at
   */
  public record FranchiseResponse(
      Long id,
      String franchiseNo,
      Long cycleId,
      String insurerCode,
      String insurerName,
      String status,
      String decision,
      Instant submittedAt,
      LocalDate dueDate,
      LocalDate decidedOn,
      String decidedBy,
      String reasonCode,
      String remarks,
      Long evidenceAttachmentId,
      LocalDate adviceDueDate,
      Instant advisedAt) {

    /**
     * Maps a request.
     *
     * @param f request
     * @param insurerName insurer name
     * @return response
     */
    public static FranchiseResponse from(EbFranchiseRequest f, String insurerName) {
      return new FranchiseResponse(
          f.getId(),
          f.getFranchiseNo(),
          f.getCycleId(),
          f.getInsurerCode(),
          insurerName,
          f.getStatus().name(),
          f.getDecision() == null ? null : f.getDecision().name(),
          f.getSubmittedAt(),
          f.getDueDate(),
          f.getDecidedOn(),
          f.getDecidedBy(),
          f.getReasonCode(),
          f.getRemarks(),
          f.getEvidenceAttachmentId(),
          f.getAdviceDueDate(),
          f.getAdvisedAt());
    }
  }
}
