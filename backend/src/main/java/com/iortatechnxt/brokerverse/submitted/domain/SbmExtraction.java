package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A document extraction (BRIDSP-02): the fields proposed from an uploaded policy document, for a
 * new record or an existing one. Nothing reaches the masterlist before a user confirms; a rejected
 * proposal keeps the document with the reason.
 */
@Entity
@Table(name = "sbm_extraction")
public class SbmExtraction extends BaseEntity {

  /** Status of an extraction. */
  public enum Status {
    /** Waiting for the user's confirmation. */
    PROPOSED,
    /** Confirmed; the fields were written to the record. */
    CONFIRMED,
    /** Rejected with a reason. */
    REJECTED,
    /** The document could not be read. */
    FAILED
  }

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "extraction_no", nullable = false, updatable = false, length = 30)
  private String extractionNo;

  @Column(name = "policy_id")
  private Long policyId;

  @Column(nullable = false, length = 30)
  private String segment;

  @Enumerated(EnumType.STRING)
  @Column(name = "business_type", nullable = false, length = 2)
  private SbmBusinessType businessType;

  @Column(name = "attachment_id", nullable = false, updatable = false)
  private Long attachmentId;

  @Column(name = "file_name", nullable = false, updatable = false, length = 255)
  private String fileName;

  @Column(nullable = false, updatable = false, length = 60)
  private String extractor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  @Column(nullable = false, updatable = false)
  private boolean readable;

  @Column(length = 500)
  private String note;

  @Column(name = "confirmed_by", length = 50)
  private String confirmedBy;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @Column(name = "reject_reason", length = 500)
  private String rejectReason;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "sbm_extraction_field", joinColumns = @JoinColumn(name = "extraction_id"))
  @MapKeyColumn(name = "field", length = 30)
  private Map<String, SbmExtractedValue> fields = new LinkedHashMap<>();

  protected SbmExtraction() {}

  /**
   * A new extraction.
   *
   * @param companyId company
   * @param extractionNo number
   * @param target record (null for a new record), segment and business type
   * @param document attachment, file name and extractor
   * @param proposal fields, readable flag and note
   */
  public SbmExtraction(
      Long companyId, String extractionNo, Target target, Document document, Proposal proposal) {
    this.companyId = companyId;
    this.extractionNo = extractionNo;
    this.policyId = target.policyId();
    this.segment = target.segment();
    this.businessType = target.businessType();
    this.attachmentId = document.attachmentId();
    this.fileName = document.fileName();
    this.extractor = document.extractor();
    this.readable = proposal.readable();
    this.note = proposal.note();
    this.fields.putAll(proposal.fields());
    this.status = proposal.readable() ? Status.PROPOSED : Status.FAILED;
  }

  /**
   * Confirms the proposal (the caller writes the fields to the record).
   *
   * @param policy the record written
   * @param by user
   * @param at time
   */
  public void confirm(Long policy, String by, Instant at) {
    requireOpen();
    this.policyId = policy;
    this.status = Status.CONFIRMED;
    this.confirmedBy = by;
    this.confirmedAt = at;
  }

  /**
   * Rejects the proposal.
   *
   * @param reason reason
   * @param by user
   * @param at time
   */
  public void reject(String reason, String by, Instant at) {
    requireOpen();
    this.status = Status.REJECTED;
    this.rejectReason = reason;
    this.confirmedBy = by;
    this.confirmedAt = at;
  }

  private void requireOpen() {
    if (status == Status.CONFIRMED || status == Status.REJECTED) {
      throw new BusinessRuleException(
          "SBM_EXTRACTION_DECIDED", "Extraction " + extractionNo + " is already decided");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getExtractionNo() {
    return extractionNo;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public String getSegment() {
    return segment;
  }

  public SbmBusinessType getBusinessType() {
    return businessType;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getExtractor() {
    return extractor;
  }

  public Status getStatus() {
    return status;
  }

  public boolean isReadable() {
    return readable;
  }

  public String getNote() {
    return note;
  }

  public String getConfirmedBy() {
    return confirmedBy;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  /**
   * The proposed fields.
   *
   * @return fields by name
   */
  public Map<String, SbmExtractedValue> getFields() {
    return Map.copyOf(fields);
  }

  /**
   * The record the document belongs to.
   *
   * @param policyId existing record, null for a new one
   * @param segment segment
   * @param businessType NB or RB
   */
  public record Target(Long policyId, String segment, SbmBusinessType businessType) {}

  /**
   * The document.
   *
   * @param attachmentId attachment
   * @param fileName file name
   * @param extractor extractor used
   */
  public record Document(Long attachmentId, String fileName, String extractor) {}

  /**
   * The proposal.
   *
   * @param fields fields by name
   * @param readable whether text was read
   * @param note note
   */
  public record Proposal(Map<String, SbmExtractedValue> fields, boolean readable, String note) {

    /** Defensive copy. */
    public Proposal {
      fields = Map.copyOf(fields);
    }
  }
}
