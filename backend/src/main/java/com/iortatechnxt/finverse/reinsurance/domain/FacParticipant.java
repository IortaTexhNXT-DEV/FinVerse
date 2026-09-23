package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A reinsurer's line on a facultative slip: its share of the facultative requirement, the sum
 * insured and premium it accepts and the commission it allows. Owned by {@link FacPlacement}.
 */
@Embeddable
public class FacParticipant {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @ManyToOne(optional = false)
  @JoinColumn(name = "party_id")
  private Party party;

  @Column(name = "share_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal sharePct;

  @Column(name = "commission_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal commissionPct;

  @Column(name = "sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal sumInsured = Money.zero();

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal premium = Money.zero();

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal commission = Money.zero();

  @Column(name = "posting_ref", length = 100)
  private String postingRef;

  /** For JPA. */
  protected FacParticipant() {}

  /**
   * Creates a participant line.
   *
   * @param lineNo line number
   * @param party reinsurer
   * @param sharePct share of the facultative requirement, %
   * @param commissionPct commission allowed, %
   */
  public FacParticipant(int lineNo, Party party, BigDecimal sharePct, BigDecimal commissionPct) {
    this.lineNo = lineNo;
    this.party = party;
    this.sharePct = sharePct;
    this.commissionPct = Money.nz(commissionPct);
  }

  /**
   * Fixes the accepted amounts when the placement is approved.
   *
   * @param si accepted sum insured
   * @param acceptedPremium accepted premium
   * @param ref accounting source reference of the cession to this participant
   */
  void accept(BigDecimal si, BigDecimal acceptedPremium, String ref) {
    this.sumInsured = Money.round(si);
    this.premium = Money.round(acceptedPremium);
    this.commission =
        Money.round(
            premium
                .multiply(commissionPct)
                .divide(HUNDRED, Money.RATE_SCALE, RoundingMode.HALF_EVEN));
    this.postingRef = ref;
  }

  public int getLineNo() {
    return lineNo;
  }

  public Party getParty() {
    return party;
  }

  public BigDecimal getSharePct() {
    return sharePct;
  }

  public BigDecimal getCommissionPct() {
    return commissionPct;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public String getPostingRef() {
    return postingRef;
  }
}
