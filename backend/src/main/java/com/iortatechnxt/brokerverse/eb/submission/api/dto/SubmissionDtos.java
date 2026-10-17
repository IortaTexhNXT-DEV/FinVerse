package com.iortatechnxt.brokerverse.eb.submission.api.dto;

import com.iortatechnxt.brokerverse.eb.domain.EbSubmission;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the submission API (FR-EB-034). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class SubmissionDtos {

  private SubmissionDtos() {}

  /**
   * A submission to send.
   *
   * @param programmeId programme
   * @param cycleId cycle, may be null
   * @param memberChangeId member change, may be null
   * @param processType process
   * @param insurerCode insurer
   * @param attachmentIds further documents
   * @param remarks remarks
   */
  public record SubmissionRequest(
      Long programmeId,
      Long cycleId,
      Long memberChangeId,
      String processType,
      String insurerCode,
      List<Long> attachmentIds,
      String remarks) {}

  /**
   * An acknowledgement date.
   *
   * @param date date acknowledged
   */
  public record AcknowledgeRequest(LocalDate date) {}

  /**
   * A submission.
   *
   * @param id id
   * @param cycleId cycle
   * @param memberChangeId member change
   * @param processType process
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param sentAt sent at
   * @param sentBy sent by
   * @param recipients recipients
   * @param acknowledgedOn acknowledged on
   * @param remarks remarks
   * @param documents documents sent
   */
  public record SubmissionResponse(
      Long id,
      Long cycleId,
      Long memberChangeId,
      String processType,
      String insurerCode,
      String insurerName,
      Instant sentAt,
      String sentBy,
      String recipients,
      LocalDate acknowledgedOn,
      String remarks,
      List<Document> documents) {

    /**
     * Maps a submission.
     *
     * @param s submission
     * @param insurerName insurer name
     * @return response
     */
    public static SubmissionResponse from(EbSubmission s, String insurerName) {
      return new SubmissionResponse(
          s.getId(),
          s.getCycleId(),
          s.getMemberChangeId(),
          s.getProcessType(),
          s.getInsurerCode(),
          insurerName,
          s.getSentAt(),
          s.getSentBy(),
          s.getRecipients(),
          s.getAcknowledgedOn(),
          s.getRemarks(),
          s.getDocuments().stream()
              .map(d -> new Document(d.getAttachmentId(), d.getDocumentType()))
              .toList());
    }
  }

  /**
   * A document sent.
   *
   * @param attachmentId stored file
   * @param documentType type
   */
  public record Document(Long attachmentId, String documentType) {}
}
