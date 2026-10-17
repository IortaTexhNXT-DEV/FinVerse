package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * The client's acceptance of a renewal (BRRN.040, BRRN.038 AC 3): method, evidence (attachment or
 * the payment gate's reference), date, source and the acknowledgement of a financial impact.
 */
@Entity
@Table(name = "rnw_acceptance")
public class RenewalAcceptance extends BaseEntity {

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private AcceptanceMethod method;

  @Column(name = "attachment_id", updatable = false)
  private Long attachmentId;

  @Column(name = "evidence_ref", length = 120, updatable = false)
  private String evidenceRef;

  @Column(name = "accepted_on", nullable = false, updatable = false)
  private LocalDate acceptedOn;

  @Column(nullable = false, length = 20, updatable = false)
  private String source;

  @Column(name = "financial_impact_ack", nullable = false, updatable = false)
  private boolean financialImpactAck;

  @Column(length = 200, updatable = false)
  private String remarks;

  protected RenewalAcceptance() {}

  /**
   * Records an acceptance.
   *
   * @param candidateId renewal
   * @param method method
   * @param evidence attachment or reference, date and remarks
   * @param source USER, SYSTEM or UPLOAD
   * @param financialImpactAck whether a financial impact was acknowledged
   */
  public RenewalAcceptance(
      Long candidateId,
      AcceptanceMethod method,
      Evidence evidence,
      String source,
      boolean financialImpactAck) {
    this.candidateId = candidateId;
    this.method = method;
    this.attachmentId = evidence.attachmentId();
    this.evidenceRef = evidence.reference();
    this.acceptedOn = evidence.acceptedOn();
    this.remarks = evidence.remarks();
    this.source = source;
    this.financialImpactAck = financialImpactAck;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public AcceptanceMethod getMethod() {
    return method;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getEvidenceRef() {
    return evidenceRef;
  }

  public LocalDate getAcceptedOn() {
    return acceptedOn;
  }

  public String getSource() {
    return source;
  }

  public boolean isFinancialImpactAck() {
    return financialImpactAck;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * Evidence of an acceptance.
   *
   * @param attachmentId attachment (e-mail or signed RA), may be null
   * @param reference other evidence (payment reference, e-mail list row), may be null
   * @param acceptedOn date of the acceptance
   * @param remarks remarks
   */
  public record Evidence(
      Long attachmentId, String reference, LocalDate acceptedOn, String remarks) {}
}
