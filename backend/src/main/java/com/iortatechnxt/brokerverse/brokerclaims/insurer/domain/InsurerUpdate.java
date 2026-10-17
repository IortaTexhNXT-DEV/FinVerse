package com.iortatechnxt.brokerverse.brokerclaims.insurer.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A communication received from an insurer about a claim (BRCLM.041; CLAIMS_BROKING_DESIGN 5.2):
 * date, source ({@code BCL_UPDATE_SOURCE}), the insurer's reference, remarks and the attachments
 * that carry the insurer's document, optionally tied to one insurer line. Insert-only: a wrong
 * update is never edited; a correcting update refers to it (FR-CM-022).
 */
@Entity
@Table(name = "bcl_insurer_update")
public class InsurerUpdate extends BaseEntity {

  private static final String SEPARATOR = ",";

  @Column(name = "claim_id", nullable = false, updatable = false)
  private Long claimId;

  @Column(name = "insurer_claim_id", updatable = false)
  private Long insurerClaimId;

  @Column(name = "update_date", nullable = false, updatable = false)
  private LocalDate updateDate;

  @Column(nullable = false, length = 40, updatable = false)
  private String source;

  @Column(length = 100, updatable = false)
  private String reference;

  @Column(nullable = false, length = 2000, updatable = false)
  private String remarks;

  @Column(name = "attachment_ids", length = 500, updatable = false)
  private String attachmentIds;

  @Column(name = "corrects_update_id", updatable = false)
  private Long correctsUpdateId;

  @Column(name = "upload_ref", length = 40, updatable = false)
  private String uploadRef;

  @Column(name = "recorded_by", nullable = false, length = 50, updatable = false)
  private String recordedBy;

  @Column(name = "recorded_at", nullable = false, updatable = false)
  private Instant recordedAt;

  protected InsurerUpdate() {}

  /**
   * Records an update.
   *
   * @param claimId claim
   * @param content what the insurer communicated
   * @param recordedBy user
   * @param recordedAt time
   */
  public InsurerUpdate(Long claimId, Content content, String recordedBy, Instant recordedAt) {
    if (content.updateDate() == null || content.updateDate().isAfter(content.today())) {
      throw new BusinessRuleException(
          "BCL_UPDATE_DATE", "Enter an update date that is not in the future");
    }
    if (content.remarks() == null || content.remarks().isBlank()) {
      throw new BusinessRuleException("BCL_REMARKS_REQUIRED", "Enter the remarks");
    }
    this.claimId = claimId;
    this.insurerClaimId = content.insurerClaimId();
    this.updateDate = content.updateDate();
    this.source = content.source();
    this.reference = blankToNull(content.reference());
    this.remarks = content.remarks().strip();
    this.attachmentIds =
        content.attachmentIds().isEmpty()
            ? null
            : content.attachmentIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(SEPARATOR));
    this.correctsUpdateId = content.correctsUpdateId();
    this.uploadRef = content.uploadRef();
    this.recordedBy = recordedBy;
    this.recordedAt = recordedAt;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  public Long getClaimId() {
    return claimId;
  }

  public Long getInsurerClaimId() {
    return insurerClaimId;
  }

  public LocalDate getUpdateDate() {
    return updateDate;
  }

  public String getSource() {
    return source;
  }

  public String getReference() {
    return reference;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * Attachments of the update.
   *
   * @return attachment ids
   */
  public List<Long> getAttachmentIdList() {
    return attachmentIds == null
        ? List.of()
        : Arrays.stream(attachmentIds.split(SEPARATOR)).map(Long::valueOf).toList();
  }

  public Long getCorrectsUpdateId() {
    return correctsUpdateId;
  }

  public String getUploadRef() {
    return uploadRef;
  }

  public String getRecordedBy() {
    return recordedBy;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  /**
   * What an insurer communicated.
   *
   * @param insurerClaimId insurer line, may be null
   * @param updateDate date of the update (not in the future)
   * @param source source ({@code BCL_UPDATE_SOURCE})
   * @param reference insurer's reference, may be null
   * @param remarks remarks
   * @param attachmentIds attachments of the claim carrying the insurer's document
   * @param correctsUpdateId update this one corrects, may be null
   * @param uploadRef bulk upload number when loaded from a file, may be null
   * @param today business date
   */
  public record Content(
      Long insurerClaimId,
      LocalDate updateDate,
      String source,
      String reference,
      String remarks,
      List<Long> attachmentIds,
      Long correctsUpdateId,
      String uploadRef,
      LocalDate today) {

    /** Defensive copy. */
    public Content {
      attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }
  }
}
