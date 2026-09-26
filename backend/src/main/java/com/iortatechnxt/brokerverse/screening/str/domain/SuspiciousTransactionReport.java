package com.iortatechnxt.brokerverse.screening.str.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A suspicious transaction report of a screening case (SNSRP-705, 706; FR-SS-070 to 072): number,
 * template version, the subject snapshot taken when it was prepared (later client changes do not
 * alter it, FR-SS-070 R1), reason codes, status, the committee decision time, the extraction and
 * the AMLC filing reference. Template field values are {@link StrField} rows, transactions {@link
 * StrTransaction} rows.
 */
@Entity
@Table(name = "scr_str")
public class SuspiciousTransactionReport extends BaseEntity {

  private static final int MAX_SNAPSHOT = 4000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "case_id", nullable = false, updatable = false)
  private Long caseId;

  @Column(name = "str_no", nullable = false, length = 30, updatable = false)
  private String strNo;

  @Column(name = "template_version_id", updatable = false)
  private Long templateVersionId;

  @Column(name = "subject_code", nullable = false, length = 30, updatable = false)
  private String subjectCode;

  @Column(name = "subject_name", nullable = false, length = 300, updatable = false)
  private String subjectName;

  @Column(name = "subject_snapshot", nullable = false, length = MAX_SNAPSHOT, updatable = false)
  private String subjectSnapshot;

  @Column(name = "reason_codes", nullable = false, length = 500)
  private String reasonCodes = "";

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private StrStatus status;

  @Column(name = "committee_decided_at")
  private Instant committeeDecidedAt;

  @Column(name = "ready_by", length = 50)
  private String readyBy;

  @Column(name = "ready_at")
  private Instant readyAt;

  @Column(name = "extraction_id")
  private Long extractionId;

  @Column(name = "extracted_at")
  private Instant extractedAt;

  @Column(name = "amlc_reference", length = 60)
  private String amlcReference;

  @Column(name = "filed_on")
  private LocalDate filedOn;

  @Column(name = "filed_by", length = 50)
  private String filedBy;

  /** For JPA. */
  protected SuspiciousTransactionReport() {}

  /**
   * Prepares a DRAFT STR.
   *
   * @param strNo the STR number
   * @param companyId company
   * @param caseId the case
   * @param templateVersionId the STR template version, may be null
   * @param subject the subject snapshot
   */
  public SuspiciousTransactionReport(
      String strNo, Long companyId, Long caseId, Long templateVersionId, Subject subject) {
    this.strNo = strNo;
    this.companyId = companyId;
    this.caseId = caseId;
    this.templateVersionId = templateVersionId;
    this.subjectCode = subject.code();
    this.subjectName = subject.name();
    this.subjectSnapshot =
        subject.snapshot().length() > MAX_SNAPSHOT
            ? subject.snapshot().substring(0, MAX_SNAPSHOT)
            : subject.snapshot();
    this.status = StrStatus.DRAFT;
  }

  /**
   * Replaces the reason codes (DRAFT only).
   *
   * @param codes the codes
   */
  public void reasons(Set<String> codes) {
    requireDraft();
    this.reasonCodes = String.join(",", new LinkedHashSet<>(codes));
  }

  /**
   * The reason codes.
   *
   * @return codes in the order given
   */
  public Set<String> reasons() {
    return reasonCodes.isBlank()
        ? Set.of()
        : Arrays.stream(reasonCodes.split(","))
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /** Refuses a change once the STR is marked ready. */
  public void requireDraft() {
    if (status != StrStatus.DRAFT) {
      throw new BusinessRuleException(
          "SCR_STR_NOT_DRAFT", "STR " + strNo + " is " + status + " and can no longer be changed");
    }
  }

  /**
   * Marks the STR ready: APPROVED when the committee approved the STR, else FOR_APPROVAL.
   *
   * @param by user
   * @param at when
   * @param committeeApprovedAt the committee's APPROVE_STR decision time, null when none
   */
  public void ready(String by, Instant at, Instant committeeApprovedAt) {
    requireDraft();
    this.readyBy = by;
    this.readyAt = at;
    this.committeeDecidedAt = committeeApprovedAt;
    this.status = committeeApprovedAt == null ? StrStatus.FOR_APPROVAL : StrStatus.APPROVED;
  }

  /**
   * Records the extraction.
   *
   * @param extraction the extraction batch
   * @param at when
   */
  public void extracted(Long extraction, Instant at) {
    this.extractionId = extraction;
    this.extractedAt = at;
    this.status = StrStatus.EXTRACTED;
  }

  /**
   * Records the AMLC filing.
   *
   * @param reference the AMLC reference
   * @param on the filing date
   * @param by user
   */
  public void filed(String reference, LocalDate on, String by) {
    this.amlcReference = reference;
    this.filedOn = on;
    this.filedBy = by;
    this.status = StrStatus.FILED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getCaseId() {
    return caseId;
  }

  public String getStrNo() {
    return strNo;
  }

  public Long getTemplateVersionId() {
    return templateVersionId;
  }

  public String getSubjectCode() {
    return subjectCode;
  }

  public String getSubjectName() {
    return subjectName;
  }

  public String getSubjectSnapshot() {
    return subjectSnapshot;
  }

  public StrStatus getStatus() {
    return status;
  }

  public Instant getCommitteeDecidedAt() {
    return committeeDecidedAt;
  }

  public String getReadyBy() {
    return readyBy;
  }

  public Instant getReadyAt() {
    return readyAt;
  }

  public Long getExtractionId() {
    return extractionId;
  }

  public Instant getExtractedAt() {
    return extractedAt;
  }

  public String getAmlcReference() {
    return amlcReference;
  }

  public LocalDate getFiledOn() {
    return filedOn;
  }

  public String getFiledBy() {
    return filedBy;
  }

  /**
   * The subject of an STR as prepared from the client master.
   *
   * @param code client code
   * @param name client name
   * @param snapshot the client facts at preparation (text)
   */
  public record Subject(String code, String name, String snapshot) {}
}
