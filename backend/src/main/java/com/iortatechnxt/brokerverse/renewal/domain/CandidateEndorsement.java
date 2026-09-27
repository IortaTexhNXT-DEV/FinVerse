package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * An endorsement of the expiring invoice family linked to a renewal (BRRN.032 AC 3): the
 * endorsement or request number, where it comes from (booking or Adjustment) and its status when it
 * was linked.
 */
@Entity
@Table(name = "rnw_candidate_endorsement")
public class CandidateEndorsement extends BaseEntity {

  /** An endorsement invoice booked on the family. */
  public static final String BOOKING = "BOOKING";

  /** An endorsement request of Adjustment. */
  public static final String ADJUSTMENT = "ADJUSTMENT";

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Column(nullable = false, length = 40, updatable = false)
  private String reference;

  @Column(nullable = false, length = 20, updatable = false)
  private String source;

  @Column(name = "status_at_link", nullable = false, length = 30)
  private String statusAtLink;

  @Column(name = "effective_date")
  private LocalDate effectiveDate;

  protected CandidateEndorsement() {}

  /**
   * Links an endorsement.
   *
   * @param candidateId candidate
   * @param reference endorsement invoice or request number
   * @param source BOOKING or ADJUSTMENT
   * @param status status when linked
   * @param effectiveDate effective date of the endorsement, may be null
   */
  public CandidateEndorsement(
      Long candidateId, String reference, String source, String status, LocalDate effectiveDate) {
    this.candidateId = candidateId;
    this.reference = reference;
    this.source = source;
    this.statusAtLink = status;
    this.effectiveDate = effectiveDate;
  }

  /**
   * Updates the status seen on the latest evaluation.
   *
   * @param status status
   */
  public void seen(String status) {
    this.statusAtLink = status;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public String getReference() {
    return reference;
  }

  public String getSource() {
    return source;
  }

  public String getStatusAtLink() {
    return statusAtLink;
  }

  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }
}
