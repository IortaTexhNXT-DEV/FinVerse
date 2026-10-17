package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The tags of a renewal account (FRRN.014.04, FRRN.014.05, FRRN.027): the details of an account For
 * Booking Only with its override approval, the Direct-to-Insurer Payment tag with its option and
 * Unit Head decision, the amortized premium (built-in billing) and the billing file it was billed
 * in.
 */
@Embeddable
public class CandidateTags {

  /** Override or Unit Head decision: waiting. */
  public static final String REQUESTED = "REQUESTED";

  /** Decision: approved. */
  public static final String APPROVED = "APPROVED";

  /** Decision: rejected. */
  public static final String REJECTED = "REJECTED";

  /** Direct-to-Insurer: with blanket approval. */
  public static final String BLANKET = "BLANKET";

  /** Direct-to-Insurer: routed to the Unit Head. */
  public static final String UNIT_HEAD = "UNIT_HEAD";

  /** Direct-to-Insurer Unit Head approval: waiting. */
  public static final String PENDING = "PENDING";

  @Column(name = "booking_only_policy_no", length = 60)
  private String policyNo;

  @Column(name = "booking_only_or_no", length = 40)
  private String orNo;

  @Column(name = "booking_only_override", length = 20)
  private String override;

  @Column(name = "booking_only_override_by", length = 50)
  private String overrideBy;

  @Column(name = "booking_only_override_reason", length = 1000)
  private String overrideReason;

  @Column(name = "dti_option", length = 20)
  private String dtiOption;

  @Column(name = "dti_status", length = 20)
  private String dtiStatus;

  @Column(name = "dti_decided_by", length = 50)
  private String dtiDecidedBy;

  @Column(name = "dti_reason", length = 1000)
  private String dtiReason;

  @Column(name = "amortized", nullable = false)
  private boolean amortized;

  @Column(name = "billing_file_id")
  private Long billingFileId;

  @Column(name = "lock_at_placement", nullable = false)
  private boolean lockAtPlacement;

  /**
   * Records the policy and official receipt of an account For Booking Only.
   *
   * @param policy policy number
   * @param or official receipt number
   */
  public void bookingOnly(String policy, String or) {
    this.policyNo = policy;
    this.orNo = or;
  }

  /**
   * Records an override step of an account For Booking Only.
   *
   * @param status REQUESTED, APPROVED or REJECTED, null to clear
   * @param user user
   * @param reason justification
   */
  public void override(String status, String user, String reason) {
    this.override = status;
    this.overrideBy = user;
    this.overrideReason = reason;
  }

  /**
   * Tags or untags the account Direct-to-Insurer Payment.
   *
   * @param option BLANKET or UNIT_HEAD, null to remove the tag
   * @param status PENDING, APPROVED or REJECTED
   */
  public void directToInsurer(String option, String status) {
    this.dtiOption = option;
    this.dtiStatus = status;
    this.dtiDecidedBy = null;
    this.dtiReason = null;
  }

  /**
   * Records the Unit Head's decision.
   *
   * @param status APPROVED or REJECTED
   * @param user Unit Head
   * @param reason rejection reason
   */
  public void dtiDecision(String status, String user, String reason) {
    this.dtiStatus = status;
    this.dtiDecidedBy = user;
    this.dtiReason = reason;
  }

  /**
   * Sets the amortized premium indicator.
   *
   * @param value whether the premium is amortized in the loan
   */
  public void amortized(boolean value) {
    this.amortized = value;
  }

  /**
   * Records the billing file of the account.
   *
   * @param id billing file
   */
  public void billed(Long id) {
    this.billingFileId = id;
  }

  /**
   * Defers the lock of the account to the sending of its placement (lock point PLACEMENT).
   *
   * @param value whether the lock waits for the placement
   */
  public void lockAtPlacement(boolean value) {
    this.lockAtPlacement = value;
  }

  public boolean isLockAtPlacement() {
    return lockAtPlacement;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getOrNo() {
    return orNo;
  }

  public String getOverride() {
    return override;
  }

  public String getOverrideBy() {
    return overrideBy;
  }

  public String getOverrideReason() {
    return overrideReason;
  }

  public String getDtiOption() {
    return dtiOption;
  }

  public String getDtiStatus() {
    return dtiStatus;
  }

  public String getDtiDecidedBy() {
    return dtiDecidedBy;
  }

  public String getDtiReason() {
    return dtiReason;
  }

  public boolean isAmortized() {
    return amortized;
  }

  public Long getBillingFileId() {
    return billingFileId;
  }
}
