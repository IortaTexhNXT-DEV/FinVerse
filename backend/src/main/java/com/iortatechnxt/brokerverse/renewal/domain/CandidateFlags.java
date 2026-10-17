package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * The flag chips of a renewal candidate (Workshop addendum; FRS section 6.2): set by the checks,
 * the Marketing workflow, the letters and the go-live take-over. Flags are shown as chips, never
 * inside the status pill.
 */
@Embeddable
public class CandidateFlags {

  @Column(name = "stp", nullable = false)
  private boolean stp;

  @Column(name = "nrns", nullable = false)
  private boolean nrns;

  @Column(name = "kyc_due", nullable = false)
  private boolean kycDue;

  @Column(name = "kyc_flagged_at")
  private Instant kycFlaggedAt;

  @Column(name = "claims_flag", nullable = false)
  private boolean claims;

  @Column(name = "endorsement_pending", nullable = false)
  private boolean endorsementPending;

  @Column(name = "outstanding_flag", nullable = false)
  private boolean outstanding;

  @Column(name = "transferred", nullable = false)
  private boolean transferred;

  @Column(name = "returned", nullable = false)
  private boolean returned;

  @Column(name = "urgent", nullable = false)
  private boolean urgent;

  @Column(name = "nfr_sent", nullable = false)
  private boolean nfrSent;

  /**
   * Sets the flags that come from the checks (BRRN.028 KYC with the time first identified,
   * BRRN.031/032 claims, endorsement and outstanding premium).
   *
   * @param kyc KYC review due
   * @param claimsFound open or paid claims on the expiring term
   * @param endorsement endorsement in progress
   * @param outstandingFound open premium above the threshold
   * @param now time of the evaluation
   */
  public void fromChecks(
      boolean kyc,
      boolean claimsFound,
      boolean endorsement,
      boolean outstandingFound,
      Instant now) {
    if (kyc && !kycDue) {
      kycFlaggedAt = now;
    } else if (!kyc) {
      kycFlaggedAt = null;
    }
    this.kycDue = kyc;
    this.claims = claimsFound;
    this.endorsementPending = endorsement;
    this.outstanding = outstandingFound;
  }

  public void setStp(boolean stp) {
    this.stp = stp;
  }

  public void setNrns(boolean nrns) {
    this.nrns = nrns;
  }

  public void setTransferred(boolean transferred) {
    this.transferred = transferred;
  }

  public void setReturned(boolean returned) {
    this.returned = returned;
  }

  public void setUrgent(boolean urgent) {
    this.urgent = urgent;
  }

  public void setNfrSent(boolean nfrSent) {
    this.nfrSent = nfrSent;
  }

  public boolean isStp() {
    return stp;
  }

  public boolean isNrns() {
    return nrns;
  }

  public boolean isKycDue() {
    return kycDue;
  }

  public Instant getKycFlaggedAt() {
    return kycFlaggedAt;
  }

  public boolean isClaims() {
    return claims;
  }

  public boolean isEndorsementPending() {
    return endorsementPending;
  }

  public boolean isOutstanding() {
    return outstanding;
  }

  public boolean isTransferred() {
    return transferred;
  }

  public boolean isReturned() {
    return returned;
  }

  public boolean isUrgent() {
    return urgent;
  }

  public boolean isNfrSent() {
    return nfrSent;
  }
}
