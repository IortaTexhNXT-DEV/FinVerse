package com.iortatechnxt.brokerverse.submitted.masterlist.api.dto;

import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmExtractedValue;
import com.iortatechnxt.brokerverse.submitted.domain.SbmExtraction;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import java.time.Instant;
import java.util.Map;

/** Records of the Extraction Review API (FRS FR-SP-002). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class ExtractionDtos {

  private ExtractionDtos() {}

  /**
   * An extraction with the data to confirm.
   *
   * @param id extraction id
   * @param extractionNo number
   * @param policyId record, null for a new one
   * @param segment segment
   * @param businessType NB or RB
   * @param attachmentId document
   * @param fileName file name
   * @param status status
   * @param readable whether text was read
   * @param note what is missing
   * @param fields proposed values with their confidence
   * @param proposed the data to confirm (record values overwritten by the proposals)
   * @param current the record's data before confirmation, null for a new record
   * @param decidedBy user who confirmed or rejected
   * @param decidedAt time
   * @param rejectReason reason of a rejection
   * @param createdAt upload time
   */
  public record ExtractionView(
      Long id,
      String extractionNo,
      Long policyId,
      String segment,
      SbmBusinessType businessType,
      Long attachmentId,
      String fileName,
      String status,
      boolean readable,
      String note,
      Map<String, SbmExtractedValue> fields,
      SbmPolicyData proposed,
      SbmPolicyData current,
      String decidedBy,
      Instant decidedAt,
      String rejectReason,
      Instant createdAt) {

    /**
     * Maps an extraction.
     *
     * @param x extraction
     * @param proposed data to confirm
     * @param current record data, may be null
     * @return view
     */
    public static ExtractionView from(
        SbmExtraction x, SbmPolicyData proposed, SbmPolicyData current) {
      return new ExtractionView(
          x.getId(),
          x.getExtractionNo(),
          x.getPolicyId(),
          x.getSegment(),
          x.getBusinessType(),
          x.getAttachmentId(),
          x.getFileName(),
          x.getStatus().name(),
          x.isReadable(),
          x.getNote(),
          x.getFields(),
          proposed,
          current,
          x.getConfirmedBy(),
          x.getConfirmedAt(),
          x.getRejectReason(),
          x.getCreatedAt());
    }
  }
}
