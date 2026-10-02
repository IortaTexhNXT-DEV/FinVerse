package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * An assignment of a renewal to a Marketing AO or a Processing Officer (BRD 1.005, 3.005): from,
 * to, reason, user and time. The AO's list covers every renewal ever assigned to him (BRRN.011).
 */
@Entity
@Table(name = "rnw_assignment")
public class RenewalAssignment extends BaseEntity {

  /** Who the renewal is assigned to. */
  public enum Role {
    /** Marketing account officer. */
    AO,
    /** Processing officer. */
    PO
  }

  @Column(name = "candidate_id", nullable = false, updatable = false)
  private Long candidateId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private Role role;

  @Column(nullable = false, length = 50, updatable = false)
  private String username;

  @Column(length = 50, updatable = false)
  private String previous;

  @Column(name = "reason_code", length = 40, updatable = false)
  private String reasonCode;

  protected RenewalAssignment() {}

  /**
   * Records an assignment.
   *
   * @param candidateId candidate
   * @param role AO or PO
   * @param username new assignee
   * @param previous previous assignee, null when none
   * @param reasonCode reason of a re-assignment (list RNW_TRANSFER_REASON), may be null
   */
  public RenewalAssignment(
      Long candidateId, Role role, String username, String previous, String reasonCode) {
    this.candidateId = candidateId;
    this.role = role;
    this.username = username;
    this.previous = previous;
    this.reasonCode = reasonCode;
  }

  public Long getCandidateId() {
    return candidateId;
  }

  public Role getRole() {
    return role;
  }

  public String getUsername() {
    return username;
  }

  public String getPrevious() {
    return previous;
  }

  public String getReasonCode() {
    return reasonCode;
  }
}
