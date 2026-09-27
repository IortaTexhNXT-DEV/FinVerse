package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The renewal hand-off of a masterlist record (BRIDSP-23, 25, 32; design section 4.4): pending
 * until the Renewal module takes it, then the renewal reference, the insurer assigned, the renewal
 * account and its hold cover as Submitted Policies follows them, the re-assignments and the outcome
 * read from the Renewal module.
 */
@Entity
@Table(name = "sbm_renewal")
public class SbmRenewal extends BaseEntity {

  /** Hand-off recorded, Renewal not connected. */
  public static final String PENDING = "PENDING";

  /** Taken by the Renewal module. */
  public static final String HANDED_OFF = "HANDED_OFF";

  /** Refused by the Renewal module. */
  public static final String REFUSED = "REFUSED";

  /** Renewal in progress. */
  public static final String IN_PROGRESS = "IN_PROGRESS";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(name = "handoff_status", nullable = false, length = 20)
  private String handoffStatus;

  @Column(nullable = false)
  private boolean manual;

  @Column(name = "renewal_ref", length = 40)
  private String renewalRef;

  @Column(name = "insurer_assigned", length = 30)
  private String insurerAssigned;

  @Column(name = "ra_template", length = 20)
  private String raTemplate;

  @Column(length = 30)
  private String arn;

  @Column(name = "hold_cover_on")
  private LocalDate holdCoverOn;

  @Column(name = "insurer_accepted_on")
  private LocalDate insurerAcceptedOn;

  @Column(name = "reassign_count", nullable = false)
  private int reassignCount;

  @Column(nullable = false, length = 20)
  private String outcome = IN_PROGRESS;

  @Column(name = "decline_reason", length = 250)
  private String declineReason;

  @Column(name = "handed_off_at", nullable = false)
  private Instant handedOffAt;

  @Column(length = 500)
  private String message;

  @Column(name = "alerted_on")
  private LocalDate alertedOn;

  protected SbmRenewal() {}

  /**
   * A hand-off.
   *
   * @param companyId company
   * @param policyId record
   * @param terms insurer assigned, RA template and manual flag
   * @param at time
   */
  public SbmRenewal(Long companyId, Long policyId, Terms terms, Instant at) {
    this.companyId = companyId;
    this.policyId = policyId;
    this.insurerAssigned = terms.insurer();
    this.raTemplate = terms.raTemplate();
    this.manual = terms.manual();
    this.handedOffAt = at;
    this.handoffStatus = PENDING;
  }

  /**
   * The answer of the Renewal module (or of the pending default).
   *
   * @param status PENDING, HANDED_OFF or REFUSED
   * @param reference renewal reference, may be null
   * @param account renewal account, may be null
   * @param text message
   */
  public void answered(String status, String reference, String account, String text) {
    this.handoffStatus = status;
    this.renewalRef = reference;
    if (account != null) {
      this.arn = account;
    }
    this.message = text;
  }

  /**
   * The renewal account is known.
   *
   * @param account ARN
   */
  public void account(String account) {
    this.arn = account;
  }

  /**
   * The hold cover of the renewal account as read from placement.
   *
   * @param requestedOn request date, may be null
   * @param acceptedOn insurer acceptance date, may be null
   */
  public void holdCover(LocalDate requestedOn, LocalDate acceptedOn) {
    this.holdCoverOn = requestedOn;
    this.insurerAcceptedOn = acceptedOn;
  }

  /**
   * The insurer was re-assigned.
   *
   * @param insurer new insurer
   */
  public void reassigned(String insurer) {
    this.insurerAssigned = insurer;
    this.reassignCount++;
    this.insurerAcceptedOn = null;
    this.alertedOn = null;
  }

  /**
   * Closes the follow-up with the outcome of the renewal.
   *
   * @param value RENEWED, DECLINED, EXPIRED or LOST
   * @param reason reason, may be null
   */
  public void close(String value, String reason) {
    this.outcome = value;
    this.declineReason = reason;
  }

  /**
   * An alert was raised for the hand-off on a date (once per condition).
   *
   * @param date date
   */
  public void alerted(LocalDate date) {
    this.alertedOn = date;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getPolicyId() {
    return policyId;
  }

  public String getHandoffStatus() {
    return handoffStatus;
  }

  public boolean isManual() {
    return manual;
  }

  public String getRenewalRef() {
    return renewalRef;
  }

  public String getInsurerAssigned() {
    return insurerAssigned;
  }

  public String getRaTemplate() {
    return raTemplate;
  }

  public String getArn() {
    return arn;
  }

  public LocalDate getHoldCoverOn() {
    return holdCoverOn;
  }

  public LocalDate getInsurerAcceptedOn() {
    return insurerAcceptedOn;
  }

  public int getReassignCount() {
    return reassignCount;
  }

  public String getOutcome() {
    return outcome;
  }

  public String getDeclineReason() {
    return declineReason;
  }

  public Instant getHandedOffAt() {
    return handedOffAt;
  }

  public String getMessage() {
    return message;
  }

  public LocalDate getAlertedOn() {
    return alertedOn;
  }

  /**
   * The terms of a hand-off.
   *
   * @param insurer insurer assigned, may be null
   * @param raTemplate RA template
   * @param manual Renew with BDOI
   */
  public record Terms(String insurer, String raTemplate, boolean manual) {}
}
