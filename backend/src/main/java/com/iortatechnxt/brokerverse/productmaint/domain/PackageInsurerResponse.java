package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The response of one insurer in one negotiation round (BRPM.013, PMADD04): the outcome (list
 * PKG_RESPONSE_OUTCOME, including the exception states), rate, minimum premium, terms per coverage
 * (JSON), conditions, validity and the response document. Every change raises the revision and is
 * kept in {@link PackageResponseHistory}; the last quotation slip e-mail is kept for the resend.
 */
@Entity
@Table(name = "pm_insurer_response")
public class PackageInsurerResponse extends BaseEntity {

  /** Outcome of a response not keyed in yet. */
  public static final String PENDING = "PENDING";

  @Column(name = "round_id", nullable = false, updatable = false)
  private Long roundId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "insurer_name", nullable = false, length = 200, updatable = false)
  private String insurerName;

  @Column(nullable = false, length = 30)
  private String outcome = PENDING;

  @Column(nullable = false)
  private int revision;

  @Column(precision = 19, scale = 8)
  private BigDecimal rate;

  @Column(name = "minimum_premium", precision = 19, scale = 2)
  private BigDecimal minimumPremium;

  @Column(nullable = false, columnDefinition = "text")
  private String terms = "[]";

  @Column(length = 2000)
  private String conditions;

  @Column(name = "valid_until")
  private LocalDate validUntil;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "response_document_id")
  private Long responseDocumentId;

  @Column(name = "responded_at")
  private Instant respondedAt;

  @Column(name = "last_message_id")
  private Long lastMessageId;

  @Column(nullable = false)
  private int sends;

  protected PackageInsurerResponse() {}

  /**
   * A pending response of an insurer approached with the round's quotation slip.
   *
   * @param roundId round
   * @param insurerCode insurer party code
   * @param insurerName insurer name
   */
  public PackageInsurerResponse(Long roundId, String insurerCode, String insurerName) {
    this.roundId = roundId;
    this.insurerCode = insurerCode;
    this.insurerName = insurerName;
  }

  /**
   * Records the terms keyed in by TSU.
   *
   * @param input outcome and terms
   * @param when time
   */
  public void record(ResponseInput input, Instant when) {
    this.outcome = input.outcome();
    this.rate = input.rate();
    this.minimumPremium = input.minimumPremium();
    this.terms = input.termsJson();
    this.conditions = input.conditions();
    this.validUntil = input.validUntil();
    this.remarks = input.remarks();
    this.respondedAt = when;
    revision++;
  }

  /**
   * Links the response document (attachment of the request).
   *
   * @param attachmentId attachment
   */
  public void attachDocument(Long attachmentId) {
    this.responseDocumentId = attachmentId;
    revision++;
  }

  /**
   * Records a (re)send of the quotation slip to this insurer.
   *
   * @param messageId outbox message
   */
  public void markSent(Long messageId) {
    this.lastMessageId = messageId;
    sends++;
  }

  /**
   * Whether the insurer gave a final outcome.
   *
   * @return true unless pending
   */
  public boolean isAnswered() {
    return !PENDING.equals(outcome);
  }

  public Long getRoundId() {
    return roundId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getInsurerName() {
    return insurerName;
  }

  public String getOutcome() {
    return outcome;
  }

  public int getRevision() {
    return revision;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public BigDecimal getMinimumPremium() {
    return minimumPremium;
  }

  public String getTerms() {
    return terms;
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

  public Long getResponseDocumentId() {
    return responseDocumentId;
  }

  public Instant getRespondedAt() {
    return respondedAt;
  }

  public Long getLastMessageId() {
    return lastMessageId;
  }

  public int getSends() {
    return sends;
  }

  /**
   * What TSU keys in for a response.
   *
   * @param outcome outcome code (list PKG_RESPONSE_OUTCOME)
   * @param rate rate in percent
   * @param minimumPremium minimum premium
   * @param termsJson terms per coverage as JSON
   * @param conditions conditions
   * @param validUntil validity
   * @param remarks remarks
   */
  public record ResponseInput(
      String outcome,
      BigDecimal rate,
      BigDecimal minimumPremium,
      String termsJson,
      String conditions,
      LocalDate validUntil,
      String remarks) {}
}
