package com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto;

import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerReserveChange;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerUpdate;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService.NewLine;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerUpdateService.NewUpdate;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.LossAdviceService.Draft;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.LossAdviceService.Recipient;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.LossAdviceService.Sent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request and response bodies of the insurer side of a claim (BRCLM.018/023/024/041/043). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class InsurerDtos {

  private InsurerDtos() {}

  /**
   * An insurer line of a claim.
   *
   * @param id line
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param sharePct share
   * @param insurerClaimNo insurer claim number
   * @param reportedToInsurerOn date reported to the insurer
   * @param reserveAmount insurer reserve
   * @param settledAmount settled amount
   * @param adjusterCode adjuster
   * @param adjusterLabel adjuster label
   */
  public record InsurerLineResponse(
      Long id,
      String insurerCode,
      String insurerName,
      BigDecimal sharePct,
      String insurerClaimNo,
      LocalDate reportedToInsurerOn,
      BigDecimal reserveAmount,
      BigDecimal settledAmount,
      String adjusterCode,
      String adjusterLabel) {

    /**
     * Maps a line.
     *
     * @param l line
     * @param names insurer names by code
     * @param adjusters adjuster labels by code
     * @return response
     */
    public static InsurerLineResponse from(
        InsurerClaim l, Map<String, String> names, Map<String, String> adjusters) {
      return new InsurerLineResponse(
          l.getId(),
          l.getInsurerCode(),
          names.get(l.getInsurerCode()),
          l.getSharePct(),
          l.getInsurerClaimNo(),
          l.getReportedToInsurerOn(),
          l.getReserveAmount(),
          l.getSettledAmount(),
          l.getAdjusterCode(),
          l.getAdjusterCode() == null ? null : adjusters.get(l.getAdjusterCode()));
    }
  }

  /**
   * One reserve amendment.
   *
   * @param id id
   * @param insurerClaimId line
   * @param previousAmount previous reserve
   * @param newAmount new reserve
   * @param reason reason
   * @param changedBy user
   * @param changedAt time
   */
  public record ReserveChangeResponse(
      Long id,
      Long insurerClaimId,
      BigDecimal previousAmount,
      BigDecimal newAmount,
      String reason,
      String changedBy,
      Instant changedAt) {

    /**
     * Maps an amendment.
     *
     * @param r amendment
     * @return response
     */
    public static ReserveChangeResponse from(InsurerReserveChange r) {
      return new ReserveChangeResponse(
          r.getId(),
          r.getInsurerClaimId(),
          r.getPreviousAmount(),
          r.getNewAmount(),
          r.getReason(),
          r.getChangedBy(),
          r.getChangedAt());
    }
  }

  /**
   * An insurer update.
   *
   * @param id id
   * @param insurerClaimId line
   * @param updateDate date
   * @param source source
   * @param reference insurer reference
   * @param remarks remarks
   * @param attachmentIds attachments
   * @param correctsUpdateId corrected update
   * @param uploadRef bulk upload
   * @param recordedBy user
   * @param recordedAt time
   */
  public record InsurerUpdateResponse(
      Long id,
      Long insurerClaimId,
      LocalDate updateDate,
      String source,
      String reference,
      String remarks,
      List<Long> attachmentIds,
      Long correctsUpdateId,
      String uploadRef,
      String recordedBy,
      Instant recordedAt) {

    /**
     * Maps an update.
     *
     * @param u update
     * @return response
     */
    public static InsurerUpdateResponse from(InsurerUpdate u) {
      return new InsurerUpdateResponse(
          u.getId(),
          u.getInsurerClaimId(),
          u.getUpdateDate(),
          u.getSource(),
          u.getReference(),
          u.getRemarks(),
          u.getAttachmentIdList(),
          u.getCorrectsUpdateId(),
          u.getUploadRef(),
          u.getRecordedBy(),
          u.getRecordedAt());
    }
  }

  /**
   * An insurer line to add; blank insurer refused with "Select the insurer".
   *
   * @param insurerCode insurer
   * @param sharePct share in percent
   * @param insurerClaimNo insurer claim number
   * @param reportedToInsurerOn date reported to the insurer
   * @param reserveAmount initial reserve
   */
  public record InsurerLineRequest(
      String insurerCode,
      BigDecimal sharePct,
      @Size(max = 60) String insurerClaimNo,
      LocalDate reportedToInsurerOn,
      BigDecimal reserveAmount) {

    /**
     * The service input.
     *
     * @return line
     */
    public NewLine toLine() {
      return new NewLine(insurerCode, sharePct, insurerClaimNo, reportedToInsurerOn, reserveAmount);
    }
  }

  /**
   * An insurer claim number to record on a line.
   *
   * @param insurerClaimNo number
   * @param reportedToInsurerOn date reported to the insurer
   * @param confirmReuse confirmation of a number found on another claim
   */
  public record NumberRequest(
      @Size(max = 60) String insurerClaimNo, LocalDate reportedToInsurerOn, boolean confirmReuse) {}

  /**
   * A share.
   *
   * @param sharePct share in percent
   */
  public record ShareRequest(BigDecimal sharePct) {}

  /**
   * A reserve amendment.
   *
   * @param amount new reserve
   * @param reason reason
   */
  public record ReserveRequest(BigDecimal amount, @Size(max = 500) String reason) {}

  /**
   * An adjuster assignment.
   *
   * @param adjusterCode adjuster
   */
  public record AdjusterRequest(String adjusterCode) {}

  /**
   * An insurer update to record.
   *
   * @param insurerClaimId line
   * @param updateDate date
   * @param source source
   * @param reference insurer reference
   * @param remarks remarks
   * @param attachmentIds attachments of the claim
   * @param correctsUpdateId corrected update
   */
  public record UpdateRequest(
      Long insurerClaimId,
      LocalDate updateDate,
      String source,
      @Size(max = 100) String reference,
      @Size(max = 2000) String remarks,
      List<Long> attachmentIds,
      Long correctsUpdateId) {

    /**
     * The service input.
     *
     * @return update
     */
    public NewUpdate toUpdate() {
      return new NewUpdate(
          insurerClaimId,
          updateDate,
          source,
          reference,
          remarks,
          attachmentIds,
          correctsUpdateId,
          null);
    }
  }

  /**
   * The loss advice of one insurer, for review.
   *
   * @param insurerCode insurer
   * @param insurerName insurer name
   * @param suggestedTo proposed recipients
   * @param subject subject
   * @param body body
   */
  public record AdviceDraftResponse(
      String insurerCode,
      String insurerName,
      List<String> suggestedTo,
      String subject,
      String body) {

    /**
     * Maps a draft.
     *
     * @param d draft
     * @return response
     */
    public static AdviceDraftResponse from(Draft d) {
      return new AdviceDraftResponse(
          d.insurerCode(), d.insurerName(), d.suggestedTo(), d.subject(), d.body());
    }
  }

  /**
   * The recipients of one insurer's advice.
   *
   * @param insurerCode insurer
   * @param to recipients
   * @param cc copy recipients
   * @param subject subject, the template's when empty
   * @param body body, the template's when empty
   */
  public record AdviceRecipient(
      String insurerCode, List<String> to, List<String> cc, String subject, String body) {

    /**
     * The service input.
     *
     * @return recipient
     */
    public Recipient toRecipient() {
      return new Recipient(insurerCode, to, cc, subject, body);
    }
  }

  /**
   * The insurers to send the advice to.
   *
   * @param recipients insurers and recipients; empty refused with "Select at least one insurer"
   */
  public record AdviceRequest(List<@Valid AdviceRecipient> recipients) {}

  /**
   * One advice sent.
   *
   * @param insurerCode insurer
   * @param messageId outbox message
   * @param attachmentId stored claims report
   * @param fileName file name
   */
  public record AdviceSentResponse(
      String insurerCode, Long messageId, Long attachmentId, String fileName) {

    /**
     * Maps a sent advice.
     *
     * @param s sent
     * @return response
     */
    public static AdviceSentResponse from(Sent s) {
      return new AdviceSentResponse(s.insurerCode(), s.messageId(), s.attachmentId(), s.fileName());
    }
  }
}
