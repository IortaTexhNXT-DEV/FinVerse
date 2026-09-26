package com.iortatechnxt.brokerverse.brokerclaims.insurer.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One insurer's side of a claim incident (BRCLM.023/029/043; CLAIMS_BROKING_DESIGN 5.2): the
 * insurer, its share, the insurer's own claim number and the date the loss was reported to it, the
 * insurer reserve and settled amount as reported by the insurer (information only, BDOI posts no
 * journal, Q44) and the adjuster the insurer appointed. A claim keeps one reference whatever the
 * number of lines (043 AC3); a line may wait for its number.
 */
@Entity
@Table(name = "bcl_insurer_claim")
public class InsurerClaim extends BaseEntity {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "claim_id", nullable = false, updatable = false)
  private Long claimId;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "share_pct", precision = 9, scale = 4)
  private BigDecimal sharePct;

  @Column(name = "insurer_claim_no", length = 60)
  private String insurerClaimNo;

  @Column(name = "reported_to_insurer_on")
  private LocalDate reportedToInsurerOn;

  @Column(name = "reserve_amount", precision = 19, scale = 2)
  private BigDecimal reserveAmount;

  @Column(name = "settled_amount", precision = 19, scale = 2)
  private BigDecimal settledAmount;

  @Column(name = "adjuster_code", length = 40)
  private String adjusterCode;

  protected InsurerClaim() {}

  /**
   * Adds an insurer to a claim.
   *
   * @param companyId company
   * @param claimId claim
   * @param insurerCode insurer party code
   * @param sharePct share in percent (from the invoice shares, editable), may be null
   * @param reserveAmount initial insurer reserve, may be null
   */
  public InsurerClaim(
      Long companyId, Long claimId, String insurerCode, BigDecimal sharePct, BigDecimal reserveAmount) {
    requireShare(sharePct);
    requireReserve(reserveAmount);
    this.companyId = companyId;
    this.claimId = claimId;
    this.insurerCode = insurerCode;
    this.sharePct = sharePct;
    this.reserveAmount = reserveAmount;
  }

  /**
   * Records the insurer's claim number and the date the loss was reported to the insurer
   * (BRCLM.043). Duplicates are checked by the service.
   *
   * @param number insurer claim number
   * @param reportedOn date reported to the insurer, may be null
   * @param today business date
   */
  public void number(String number, LocalDate reportedOn, LocalDate today) {
    if (reportedOn != null && reportedOn.isAfter(today)) {
      throw new BusinessRuleException("BCL_DATE_FUTURE", "The date cannot be in the future");
    }
    this.insurerClaimNo = number == null || number.isBlank() ? null : number.strip();
    this.reportedToInsurerOn = reportedOn;
  }

  /**
   * Changes the share of the insurer (043 R3).
   *
   * @param newShare share in percent
   */
  public void changeShare(BigDecimal newShare) {
    requireShare(newShare);
    this.sharePct = newShare;
  }

  /**
   * Amends the insurer reserve (BRCLM.023/024); the history row is written by the service.
   *
   * @param amount new reserve
   * @return the previous reserve, may be null
   */
  public BigDecimal amendReserve(BigDecimal amount) {
    if (amount == null) {
      throw new BusinessRuleException("BCL_RESERVE_REQUIRED", "Enter the new reserve");
    }
    requireReserve(amount);
    BigDecimal previous = this.reserveAmount;
    this.reserveAmount = amount;
    return previous;
  }

  /**
   * Records the amount the insurer settled on this line (settlement, wave CL1-B; BRCLM.029/030).
   *
   * @param amount settled amount
   */
  public void settled(BigDecimal amount) {
    if (amount != null && amount.signum() < 0) {
      throw new BusinessRuleException(
          "BCL_AMOUNT_NEGATIVE", "The settled amount cannot be negative");
    }
    this.settledAmount = amount;
  }

  /**
   * Sets the adjuster the insurer appointed on this line (BRCLM.018).
   *
   * @param code adjuster ({@code BCL_ADJUSTER})
   * @return the previous adjuster, may be null
   */
  public String assignAdjuster(String code) {
    String previous = this.adjusterCode;
    this.adjusterCode = code;
    return previous;
  }

  private static void requireShare(BigDecimal share) {
    if (share != null && (share.signum() <= 0 || share.compareTo(HUNDRED) > 0)) {
      throw new BusinessRuleException("BCL_SHARE_RANGE", "The share must be above 0 and at most 100");
    }
  }

  private static void requireReserve(BigDecimal amount) {
    if (amount != null && amount.signum() < 0) {
      throw new BusinessRuleException("BCL_RESERVE_NEGATIVE", "The reserve cannot be negative");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getClaimId() {
    return claimId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public BigDecimal getSharePct() {
    return sharePct;
  }

  public String getInsurerClaimNo() {
    return insurerClaimNo;
  }

  public LocalDate getReportedToInsurerOn() {
    return reportedToInsurerOn;
  }

  public BigDecimal getReserveAmount() {
    return reserveAmount;
  }

  public BigDecimal getSettledAmount() {
    return settledAmount;
  }

  public String getAdjusterCode() {
    return adjusterCode;
  }
}
