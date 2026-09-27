package com.iortatechnxt.brokerverse.placement.domain;

import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A hold cover requested from the insurer while the placement completes (BRNB.072) and its
 * confirmation by the insurer with reference and date (BRNB.103). Expiry is monitored by the
 * HOLD_COVER_EXPIRY job.
 */
@Entity
@Table(name = "plc_hold_cover")
public class HoldCover extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "insurer_code", nullable = false, length = 30)
  private String insurerCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private HoldCoverStatus status = HoldCoverStatus.REQUESTED;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "expiry_date", nullable = false)
  private LocalDate expiryDate;

  @Column(name = "insurer_ref", length = 60)
  private String insurerRef;

  @Column(name = "confirmed_on")
  private LocalDate confirmedOn;

  @Column(name = "alerted_on")
  private LocalDate alertedOn;

  protected HoldCover() {}

  /**
   * Creates a requested hold cover.
   *
   * @param companyId company
   * @param accountId account
   * @param arn Account Reference Number
   * @param insurerCode insurer asked
   * @param period start and expiry dates
   */
  public HoldCover(
      Long companyId, Long accountId, String arn, String insurerCode, CoverPeriod period) {
    this.companyId = companyId;
    this.accountId = accountId;
    this.arn = arn;
    this.insurerCode = insurerCode;
    this.startDate = period.start();
    this.expiryDate = period.expiry();
  }

  /**
   * Records the insurer's confirmation (BRNB.103).
   *
   * @param confirmingInsurer insurer that confirmed
   * @param reference insurer reference
   * @param date confirmation date
   * @param expiry expiry date confirmed by the insurer, null to keep the requested one
   */
  public void confirm(
      String confirmingInsurer, String reference, LocalDate date, LocalDate expiry) {
    requireOpen();
    this.status = HoldCoverStatus.CONFIRMED;
    this.insurerCode = confirmingInsurer;
    this.insurerRef = reference;
    this.confirmedOn = date;
    if (expiry != null) {
      if (expiry.isBefore(startDate)) {
        throw new BusinessRuleException(
            "HOLD_COVER_DATES",
            "The hold cover cannot expire before it starts (" + startDate + ")");
      }
      this.expiryDate = expiry;
      this.alertedOn = null;
    }
  }

  /**
   * Records that the insurer declined the hold cover.
   *
   * @param reference insurer reference, may be null
   */
  public void decline(String reference) {
    requireOpen();
    this.status = HoldCoverStatus.DECLINED;
    this.insurerRef = reference;
  }

  /** Marks the hold cover as lapsed without a policy. */
  public void expire() {
    this.status = HoldCoverStatus.EXPIRED;
  }

  /**
   * Records that Processing was alerted of the coming expiry.
   *
   * @param date alert date
   */
  public void markAlerted(LocalDate date) {
    this.alertedOn = date;
  }

  /**
   * Whether the hold cover is requested or confirmed.
   *
   * @return true while in force or pending
   */
  public boolean isOpen() {
    return status == HoldCoverStatus.REQUESTED || status == HoldCoverStatus.CONFIRMED;
  }

  private void requireOpen() {
    if (!isOpen()) {
      throw new BusinessRuleException(
          "HOLD_COVER_CLOSED", "The hold cover of " + arn + " is " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getArn() {
    return arn;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public HoldCoverStatus getStatus() {
    return status;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public String getInsurerRef() {
    return insurerRef;
  }

  public LocalDate getConfirmedOn() {
    return confirmedOn;
  }

  public LocalDate getAlertedOn() {
    return alertedOn;
  }

  /**
   * Hold cover period.
   *
   * @param start first day covered
   * @param expiry last day covered
   */
  public record CoverPeriod(LocalDate start, LocalDate expiry) {}
}
