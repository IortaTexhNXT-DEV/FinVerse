package com.iortatechnxt.brokerverse.claims.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.party.domain.Party;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Local Purchase Order: authority given to a garage to repair a vehicle under a motor claim, own
 * damage (OD) or third party (TP). Net = gross − discount. An LPO is a commitment, not an
 * accounting entry: the garage is paid through a claim settlement.
 */
@Entity
@Table(name = "clm_lpo")
public class Lpo extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "claim_id")
  private Claim claim;

  @Column(name = "lpo_no", nullable = false, length = 40)
  private String lpoNo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "garage_party_id")
  private Party garage;

  @Enumerated(EnumType.STRING)
  @Column(name = "cover_type", nullable = false, length = 5)
  private LpoCover coverType;

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal grossAmount;

  @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal discountAmount;

  @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal netAmount;

  @Column(nullable = false, length = 300)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 15)
  private LpoStatus status = LpoStatus.ISSUED;

  @Column(name = "cancel_reason", length = 200)
  private String cancelReason;

  protected Lpo() {}

  /**
   * Issues an LPO.
   *
   * @param claim motor claim
   * @param lpoNo allocated number
   * @param terms garage, cover, date, amounts and description
   */
  public Lpo(Claim claim, String lpoNo, LpoTerms terms) {
    BigDecimal discount = Money.nz(terms.discount());
    BigDecimal net = terms.gross().subtract(discount);
    if (discount.signum() < 0 || net.signum() <= 0) {
      throw new BusinessRuleException(
          "INVALID_LPO_AMOUNT", "The discount must be positive and below the gross LPO amount");
    }
    this.claim = claim;
    this.lpoNo = lpoNo;
    this.garage = terms.garage();
    this.coverType = terms.cover();
    this.issueDate = terms.issueDate();
    this.grossAmount = terms.gross();
    this.discountAmount = discount;
    this.netAmount = net;
    this.description = terms.description();
  }

  /**
   * Cancels the LPO.
   *
   * @param reason reason
   */
  public void cancel(String reason) {
    if (status != LpoStatus.ISSUED) {
      throw new BusinessRuleException("INVALID_LPO_STATUS", "LPO " + lpoNo + " is " + status);
    }
    this.status = LpoStatus.CANCELLED;
    this.cancelReason = reason;
  }

  public Claim getClaim() {
    return claim;
  }

  public String getLpoNo() {
    return lpoNo;
  }

  public Party getGarage() {
    return garage;
  }

  public LpoCover getCoverType() {
    return coverType;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public BigDecimal getGrossAmount() {
    return grossAmount;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public BigDecimal getNetAmount() {
    return netAmount;
  }

  public String getDescription() {
    return description;
  }

  public LpoStatus getStatus() {
    return status;
  }

  public String getCancelReason() {
    return cancelReason;
  }
}
