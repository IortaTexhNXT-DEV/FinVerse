package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

/**
 * The hold cover of a renewal and its closing letter (Walkthrough addendum BRRN.037, 042, rules
 * R37-HC-01 to 12; FR-RN-082, 083, 086): the status and confirmed end of the hold cover of the
 * renewal account, which make the effective expiry date, the closing letter the renewal is routed
 * to when it reaches that date unrenewed (NAL to Operations when a Renewal Advice was sent, NRL to
 * the Marketing AO otherwise) and the closing letter sent. A NAL and an NRL exclude each other.
 */
@Embeddable
public class CandidateExpiry {

  /** Closing letter routed to Operations: No Advice Letter. */
  public static final String NAL = "NAL";

  /** Closing letter routed to the Marketing AO: Non-Renewal Letter (the Not for Renewal Letter). */
  public static final String NRL = "NRL";

  @Enumerated(EnumType.STRING)
  @Column(name = "hold_cover_status", length = 20)
  private HoldCoverStatus holdCoverStatus;

  @Column(name = "hold_cover_until")
  private LocalDate holdCoverUntil;

  @Column(name = "closing_route", length = 10)
  private String closingRoute;

  @Column(name = "closing_letter", length = 10)
  private String closingLetter;

  /**
   * Records the hold cover of the renewal account.
   *
   * @param status status, null when none
   * @param until end of the hold cover, kept only while it is confirmed
   */
  public void holdCover(HoldCoverStatus status, LocalDate until) {
    this.holdCoverStatus = status;
    this.holdCoverUntil = status == HoldCoverStatus.CONFIRMED ? until : null;
  }

  /**
   * The effective expiry date (R37-HC-01 to 04): the end of a confirmed hold cover, otherwise the
   * policy expiry date.
   *
   * @param policyExpiry expiry of the expiring policy
   * @return effective expiry date
   */
  public LocalDate effectiveExpiry(LocalDate policyExpiry) {
    return holdCoverUntil != null && holdCoverUntil.isAfter(policyExpiry)
        ? holdCoverUntil
        : policyExpiry;
  }

  /**
   * Routes the renewal to its closing letter at the effective expiry date.
   *
   * @param route {@link #NAL} or {@link #NRL}
   */
  public void route(String route) {
    this.closingRoute = route;
  }

  /**
   * Records the closing letter sent.
   *
   * @param letter NAL or NFR
   */
  public void letterSent(String letter) {
    this.closingLetter = letter;
  }

  public HoldCoverStatus getHoldCoverStatus() {
    return holdCoverStatus;
  }

  public LocalDate getHoldCoverUntil() {
    return holdCoverUntil;
  }

  public String getClosingRoute() {
    return closingRoute;
  }

  public String getClosingLetter() {
    return closingLetter;
  }
}
