package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One evaluation of the incentive indicator of a transaction (FR-NB-118, FR-RN-087 audit): the
 * trigger, the criteria matched, the endorsements considered, the result and the reason.
 * Insert-only.
 */
@Entity
@Table(name = "bkg_incentive_evaluation")
public class IncentiveEvaluation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 20, updatable = false)
  private IncentiveTrigger trigger;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private IncentiveStatus result;

  @Column(length = 500, updatable = false)
  private String criteria;

  @Column(length = 1000, updatable = false)
  private String endorsements;

  @Column(nullable = false, length = 300, updatable = false)
  private String reason;

  @Column(name = "evaluated_at", nullable = false, updatable = false)
  private Instant evaluatedAt;

  @Column(name = "evaluated_by", nullable = false, length = 50, updatable = false)
  private String evaluatedBy;

  protected IncentiveEvaluation() {}

  /**
   * Records an evaluation.
   *
   * @param companyId company
   * @param invoiceNo root invoice of the transaction
   * @param trigger trigger
   * @param outcome result, criteria, endorsements and reason
   * @param at time
   * @param by user or SYSTEM
   */
  public IncentiveEvaluation(
      Long companyId,
      String invoiceNo,
      IncentiveTrigger trigger,
      Outcome outcome,
      Instant at,
      String by) {
    this.companyId = companyId;
    this.invoiceNo = invoiceNo;
    this.trigger = trigger;
    this.result = outcome.result();
    this.criteria = outcome.criteria();
    this.endorsements = outcome.endorsements();
    this.reason = outcome.reason();
    this.evaluatedAt = at;
    this.evaluatedBy = by;
  }

  public Long getId() {
    return id;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public IncentiveTrigger getTrigger() {
    return trigger;
  }

  public IncentiveStatus getResult() {
    return result;
  }

  public String getCriteria() {
    return criteria;
  }

  public String getEndorsements() {
    return endorsements;
  }

  public String getReason() {
    return reason;
  }

  public Instant getEvaluatedAt() {
    return evaluatedAt;
  }

  public String getEvaluatedBy() {
    return evaluatedBy;
  }

  /**
   * The outcome of an evaluation.
   *
   * @param result indicator
   * @param criteria criteria codes matched, comma-separated, null when none
   * @param endorsements endorsement invoices considered, comma-separated, null when none
   * @param reason reason shown in the history
   */
  public record Outcome(
      IncentiveStatus result, String criteria, String endorsements, String reason) {}
}
