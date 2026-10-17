package com.iortatechnxt.brokerverse.submitted.renewal.api.dto;

import com.iortatechnxt.brokerverse.submitted.domain.SbmLetter;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPrintBatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewal;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Records of the renewal work list, letters and print batches API (FR-SP-060 to 066). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class RenewalDtos {

  private RenewalDtos() {}

  /**
   * A row of the renewal work list.
   *
   * @param id hand-off
   * @param policyId record
   * @param sbmNo masterlist number
   * @param assuredName assured
   * @param segment segment
   * @param expiringInsurer expiring insurer
   * @param expiryDate expiry
   * @param sumInsured sum insured
   * @param policyStatus record status
   * @param handoffStatus PENDING, HANDED_OFF or REFUSED
   * @param manual Renew with BDOI
   * @param insurerAssigned insurer assigned
   * @param raTemplate RA template
   * @param renewalRef renewal candidate
   * @param arn renewal account
   * @param holdCoverOn hold cover
   * @param insurerAcceptedOn insurer acceptance
   * @param reassignCount re-assignments
   * @param outcome outcome
   * @param declineReason decline reason
   * @param handedOffAt time
   * @param message answer of the Renewal module
   * @param handlerUsername handler
   * @param aoUsername Account Officer
   */
  public record RenewalRow(
      Long id,
      Long policyId,
      String sbmNo,
      String assuredName,
      String segment,
      String expiringInsurer,
      LocalDate expiryDate,
      BigDecimal sumInsured,
      String policyStatus,
      String handoffStatus,
      boolean manual,
      String insurerAssigned,
      String raTemplate,
      String renewalRef,
      String arn,
      LocalDate holdCoverOn,
      LocalDate insurerAcceptedOn,
      int reassignCount,
      String outcome,
      String declineReason,
      Instant handedOffAt,
      String message,
      String handlerUsername,
      String aoUsername) {

    /**
     * Maps a hand-off.
     *
     * @param r hand-off
     * @param p its record
     * @return row
     */
    public static RenewalRow from(SbmRenewal r, SbmPolicy p) {
      return new RenewalRow(
          r.getId(),
          p.getId(),
          p.getSbmNo(),
          p.getAssured().assuredName(),
          p.getSegment(),
          p.getTerms().insurerCode(),
          p.getTerms().expiryDate(),
          p.getTerms().sumInsured(),
          p.getStatus().name(),
          r.getHandoffStatus(),
          r.isManual(),
          r.getInsurerAssigned(),
          r.getRaTemplate(),
          r.getRenewalRef(),
          r.getArn(),
          r.getHoldCoverOn(),
          r.getInsurerAcceptedOn(),
          r.getReassignCount(),
          r.getOutcome(),
          r.getDeclineReason(),
          r.getHandedOffAt(),
          r.getMessage(),
          p.getHandlerUsername(),
          p.getAoUsername());
    }
  }

  /**
   * A re-assignment of the insurer.
   *
   * @param insurerCode new insurer
   * @param reasonCode reason
   */
  public record ReassignRequest(@NotBlank String insurerCode, @NotBlank String reasonCode) {}

  /**
   * The result of an expiry scan started by hand.
   *
   * @param handedOff records handed over
   * @param noInsurer records without an insurer rule
   * @param replayed pending hand-offs offered again
   */
  public record ScanView(int handedOff, int noInsurer, int replayed) {}

  /**
   * A letter.
   *
   * @param id id
   * @param letterNo number
   * @param policyId record
   * @param sbmNo masterlist number
   * @param letterType type
   * @param channel EMAIL or PRINT
   * @param status status
   * @param recipient recipient
   * @param templateCode template
   * @param templateVersion template version
   * @param storedFileId letter PDF
   * @param printBatchId print batch
   * @param error refusal
   * @param sentAt time
   */
  public record LetterView(
      Long id,
      String letterNo,
      Long policyId,
      String sbmNo,
      String letterType,
      String channel,
      String status,
      String recipient,
      String templateCode,
      Integer templateVersion,
      Long storedFileId,
      Long printBatchId,
      String error,
      Instant sentAt) {

    /**
     * Maps a letter.
     *
     * @param l letter
     * @param sbmNo masterlist number
     * @return view
     */
    public static LetterView from(SbmLetter l, String sbmNo) {
      return new LetterView(
          l.getId(),
          l.getLetterNo(),
          l.getPolicyId(),
          sbmNo,
          l.getLetterType(),
          l.getChannel(),
          l.getStatus(),
          l.getRecipient(),
          l.getTemplateCode(),
          l.getTemplateVersion(),
          l.getStoredFileId(),
          l.getPrintBatchId(),
          l.getError(),
          l.getSentAt());
    }
  }

  /**
   * A print batch.
   *
   * @param id id
   * @param batchNo number
   * @param source source
   * @param letterType letter type
   * @param batchDate date
   * @param letterCount letters
   * @param mergedFileId merged PDF
   * @param controlFileId control list
   * @param handedTo mail house
   * @param handedAt time
   */
  public record PrintBatchView(
      Long id,
      String batchNo,
      String source,
      String letterType,
      LocalDate batchDate,
      int letterCount,
      Long mergedFileId,
      Long controlFileId,
      String handedTo,
      Instant handedAt) {

    /**
     * Maps a batch.
     *
     * @param b batch
     * @return view
     */
    public static PrintBatchView from(SbmPrintBatch b) {
      return new PrintBatchView(
          b.getId(),
          b.getBatchNo(),
          b.getSource(),
          b.getLetterType(),
          b.getBatchDate(),
          b.getLetterCount(),
          b.getMergedFileId(),
          b.getControlFileId(),
          b.getHandedTo(),
          b.getHandedAt());
    }
  }

  /**
   * The result of a letter dispatch started by hand.
   *
   * @param letters letters sent
   * @param printBatches print batches handed over
   */
  public record DispatchView(int letters, int printBatches) {}
}
