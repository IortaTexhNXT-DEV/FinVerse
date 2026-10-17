package com.iortatechnxt.brokerverse.commission.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.Hibernate;

/**
 * A BIR withholding tax certificate of an insurer tagged to the ORs issued for its commissions and
 * submitted digitally to Comptrollership (CMRID.010/015, workflow {@code OPS_BIR_CERT}): form,
 * number, period and tax withheld, the ORs it covers, the scans (attachments) and the decision
 * (acknowledged, or rejected with a reason and resubmitted).
 */
@Entity
@Table(name = "cmr_certificate")
public class CertificateSubmission extends BaseEntity {

  /** Workflow of the submissions. */
  public static final String WORKFLOW = "OPS_BIR_CERT";

  /** Stage waiting for Comptrollership. */
  public static final String SUBMITTED = "SUBMITTED";

  /** Rejected, to resubmit. */
  public static final String REJECTED = "REJECTED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "submission_no", nullable = false, length = 30, updatable = false)
  private String submissionNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "certificate_form", nullable = false, length = 20)
  private String certificateForm;

  @Column(name = "certificate_no", nullable = false, length = 60)
  private String certificateNo;

  @Column(name = "period_from", nullable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false)
  private LocalDate periodTo;

  @Column(name = "tax_withheld", nullable = false, precision = 19, scale = 2)
  private BigDecimal taxWithheld;

  @Column(nullable = false, length = 40)
  private String stage;

  @Column(name = "reject_reason", length = 500)
  private String rejectReason;

  @Column(name = "submitted_count", nullable = false)
  private int submittedCount;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @ElementCollection
  @CollectionTable(name = "cmr_certificate_or", joinColumns = @JoinColumn(name = "certificate_id"))
  @OrderColumn(name = "or_index")
  private final List<OrLink> receipts = new ArrayList<>();

  protected CertificateSubmission() {}

  /**
   * A new submission.
   *
   * @param companyId company
   * @param submissionNo submission number
   * @param insurerCode insurer that issued the certificate
   * @param certificate certificate facts and ORs
   */
  public CertificateSubmission(
      Long companyId, String submissionNo, String insurerCode, Certificate certificate) {
    this.companyId = companyId;
    this.submissionNo = submissionNo;
    this.insurerCode = insurerCode;
    this.stage = SUBMITTED;
    this.submittedCount = 1;
    replace(certificate);
  }

  /**
   * Replaces the certificate facts (resubmission after a rejection).
   *
   * @param c certificate facts and ORs
   */
  public final void replace(Certificate c) {
    this.certificateForm = c.form();
    this.certificateNo = c.number();
    this.periodFrom = c.periodFrom();
    this.periodTo = c.periodTo();
    this.taxWithheld = c.taxWithheld();
    this.receipts.clear();
    this.receipts.addAll(c.receipts());
  }

  /** Counts a resubmission. */
  public void resubmitted() {
    this.submittedCount++;
    this.rejectReason = null;
  }

  /**
   * Records Comptrollership's decision.
   *
   * @param reason reason of a rejection, null when acknowledged
   * @param at time
   * @param by user
   */
  public void decided(String reason, Instant at, String by) {
    this.rejectReason = reason;
    this.decidedAt = at;
    this.decidedBy = by;
  }

  /**
   * Mirrors the work case stage.
   *
   * @param stageCode stage
   */
  public void mirrorStage(String stageCode) {
    this.stage = stageCode;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSubmissionNo() {
    return submissionNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getCertificateForm() {
    return certificateForm;
  }

  public String getCertificateNo() {
    return certificateNo;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public BigDecimal getTaxWithheld() {
    return taxWithheld;
  }

  public String getStage() {
    return stage;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public int getSubmittedCount() {
    return submittedCount;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  /** Loads the ORs inside the transaction, for lists read outside it. */
  public void loadReceipts() {
    Hibernate.initialize(receipts);
  }

  public List<OrLink> getReceipts() {
    return Collections.unmodifiableList(receipts);
  }

  /**
   * An OR covered by the certificate.
   *
   * @param orNo official receipt number
   * @param amount commission amount of the OR
   */
  @Embeddable
  public record OrLink(
      @Column(name = "or_no", nullable = false, length = 40) String orNo,
      @Column(nullable = false, precision = 19, scale = 2) BigDecimal amount) {}

  /**
   * Certificate facts.
   *
   * @param form BIR form (e.g. 2307)
   * @param number certificate number
   * @param periodFrom period start
   * @param periodTo period end
   * @param taxWithheld tax withheld
   * @param receipts ORs covered
   */
  public record Certificate(
      String form,
      String number,
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal taxWithheld,
      List<OrLink> receipts) {

    /** Defensive copy. */
    public Certificate {
      receipts = receipts == null ? List.of() : List.copyOf(receipts);
    }
  }
}
