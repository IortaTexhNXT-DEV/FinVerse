package com.iortatechnxt.brokerverse.nonpackage.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The response of one insurer to the quotation slip of a PRF (BRNB.009): premium, rate,
 * deductibles, conditions, validity, remarks and the response document; the recommended flag feeds
 * the comparative table (BRNB.010). Every change raises the revision and is kept in {@link
 * InsurerResponseHistory}.
 */
@Entity
@Table(name = "npk_insurer_response")
public class InsurerResponse extends BaseEntity {

  @Column(name = "proposal_id", nullable = false, updatable = false)
  private Long proposalId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "insurer_name", nullable = false, length = 200, updatable = false)
  private String insurerName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ResponseStatus status = ResponseStatus.PENDING;

  @Column(nullable = false)
  private int revision;

  @Column(precision = 19, scale = 2)
  private BigDecimal premium;

  @Column(precision = 19, scale = 8)
  private BigDecimal rate;

  @Column(length = 1000)
  private String deductibles;

  @Column(length = 2000)
  private String conditions;

  @Column(name = "valid_until")
  private LocalDate validUntil;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "document_id")
  private Long documentId;

  @Column(nullable = false)
  private boolean recommended;

  @Column(name = "responded_at")
  private Instant respondedAt;

  protected InsurerResponse() {}

  /**
   * A pending response of an insurer approached with the quotation slip.
   *
   * @param proposalId PRF
   * @param insurerCode insurer party code
   * @param insurerName insurer name
   */
  public InsurerResponse(Long proposalId, String insurerCode, String insurerName) {
    this.proposalId = proposalId;
    this.insurerCode = insurerCode;
    this.insurerName = insurerName;
  }

  /**
   * Records the terms keyed in by TSU.
   *
   * @param terms terms
   * @param when time
   */
  public void record(ResponseTerms terms, Instant when) {
    this.status = terms.status();
    this.premium = terms.premium();
    this.rate = terms.rate();
    this.deductibles = terms.deductibles();
    this.conditions = terms.conditions();
    this.validUntil = terms.validUntil();
    this.remarks = terms.remarks();
    this.respondedAt = when;
    if (status != ResponseStatus.RECEIVED) {
      this.recommended = false;
    }
    revision++;
  }

  /**
   * Links the response document (attachment of the PRF).
   *
   * @param attachmentId attachment
   */
  public void attachDocument(Long attachmentId) {
    this.documentId = attachmentId;
    revision++;
  }

  /**
   * Sets or clears the recommended flag.
   *
   * @param flag recommended
   */
  public void recommend(boolean flag) {
    if (this.recommended != flag) {
      this.recommended = flag;
      revision++;
    }
  }

  /**
   * The terms as recorded.
   *
   * @return terms
   */
  public ResponseTerms terms() {
    return new ResponseTerms(status, premium, rate, deductibles, conditions, validUntil, remarks);
  }

  public Long getProposalId() {
    return proposalId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getInsurerName() {
    return insurerName;
  }

  public ResponseStatus getStatus() {
    return status;
  }

  public int getRevision() {
    return revision;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public String getDeductibles() {
    return deductibles;
  }

  public String getConditions() {
    return conditions;
  }

  public LocalDate getValidUntil() {
    return validUntil;
  }

  public String getRemarks() {
    return remarks;
  }

  public Long getDocumentId() {
    return documentId;
  }

  public boolean isRecommended() {
    return recommended;
  }

  public Instant getRespondedAt() {
    return respondedAt;
  }
}
