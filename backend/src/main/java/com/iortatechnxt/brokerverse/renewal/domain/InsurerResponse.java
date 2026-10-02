package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An insurer's response to a renewal (BRD 3.009.6; BRRN.035): append-only. The latest valid
 * response drives the renewal; a mismatched row is kept but never progresses it; a late or
 * conflicting response blocks straight-through progress.
 */
@Entity(name = "RnwInsurerResponse")
@Table(name = "rnw_insurer_response")
public class InsurerResponse extends BaseEntity {

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(name = "batch_id", updatable = false)
  private Long batchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private InsurerResponseCode response;

  @Column(name = "insurer_ref", length = 60, updatable = false)
  private String insurerRef;

  @Column(name = "revised_premium", precision = 19, scale = 2, updatable = false)
  private BigDecimal revisedPremium;

  @Column(name = "revised_sum_insured", precision = 19, scale = 2, updatable = false)
  private BigDecimal revisedSumInsured;

  @Column(name = "revised_rate", precision = 19, scale = 8, updatable = false)
  private BigDecimal revisedRate;

  @Column(length = 1000, updatable = false)
  private String terms;

  @Column(name = "received_on", nullable = false, updatable = false)
  private LocalDate receivedOn;

  @Column(nullable = false, length = 20, updatable = false)
  private String source;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_outcome", nullable = false, length = 20, updatable = false)
  private MatchOutcome matchOutcome;

  @Column(name = "latest_valid", nullable = false)
  private boolean latestValid;

  @Column(nullable = false, updatable = false)
  private boolean late;

  @Column(nullable = false)
  private boolean conflicting;

  @Column(name = "job_no", length = 40, updatable = false)
  private String jobNo;

  @Column(name = "row_no", updatable = false)
  private Integer rowNo;

  @Column(length = 200, updatable = false)
  private String remarks;

  protected InsurerResponse() {}

  /**
   * Records a response.
   *
   * @param candidateId candidate
   * @param batchId batch, null for a manual response outside a batch
   * @param content response code, insurer reference, revised values, terms and date
   * @param origin source, match, lateness, job and row
   */
  public InsurerResponse(Long candidateId, Long batchId, Content content, Origin origin) {
    this.candidateId = candidateId;
    this.batchId = batchId;
    this.response = content.response();
    this.insurerRef = content.insurerRef();
    this.revisedPremium = content.revisedPremium();
    this.revisedSumInsured = content.revisedSumInsured();
    this.revisedRate = content.revisedRate();
    this.terms = content.terms();
    this.receivedOn = content.receivedOn();
    this.remarks = content.remarks();
    this.source = origin.source();
    this.matchOutcome = origin.match();
    this.late = origin.late();
    this.jobNo = origin.jobNo();
    this.rowNo = origin.rowNo();
  }

  /**
   * Makes the response the latest valid one, or not.
   *
   * @param valid latest valid
   */
  public void markLatestValid(boolean valid) {
    this.latestValid = valid;
  }

  /** Flags the response as conflicting with another one of the same file. */
  public void markConflicting() {
    this.conflicting = true;
    this.latestValid = false;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public InsurerResponseCode getResponse() {
    return response;
  }

  public String getInsurerRef() {
    return insurerRef;
  }

  public BigDecimal getRevisedPremium() {
    return revisedPremium;
  }

  public BigDecimal getRevisedSumInsured() {
    return revisedSumInsured;
  }

  public BigDecimal getRevisedRate() {
    return revisedRate;
  }

  public String getTerms() {
    return terms;
  }

  public LocalDate getReceivedOn() {
    return receivedOn;
  }

  public String getSource() {
    return source;
  }

  public MatchOutcome getMatchOutcome() {
    return matchOutcome;
  }

  public boolean isLatestValid() {
    return latestValid;
  }

  public boolean isLate() {
    return late;
  }

  public boolean isConflicting() {
    return conflicting;
  }

  public String getJobNo() {
    return jobNo;
  }

  public Integer getRowNo() {
    return rowNo;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * What the insurer answered.
   *
   * @param response response code
   * @param insurerRef insurer reference
   * @param revisedPremium revised premium (Revise)
   * @param revisedSumInsured revised sum insured (Revise)
   * @param revisedRate revised rate (Revise)
   * @param terms revised terms
   * @param receivedOn date received
   * @param remarks remarks
   */
  public record Content(
      InsurerResponseCode response,
      String insurerRef,
      BigDecimal revisedPremium,
      BigDecimal revisedSumInsured,
      BigDecimal revisedRate,
      String terms,
      LocalDate receivedOn,
      String remarks) {}

  /**
   * Where the response came from.
   *
   * @param source UPLOAD or MANUAL
   * @param match match outcome
   * @param late received after the reply date
   * @param jobNo upload job, null for a manual response
   * @param rowNo row of the file, null for a manual response
   */
  public record Origin(
      String source, MatchOutcome match, boolean late, String jobNo, Integer rowNo) {}
}
