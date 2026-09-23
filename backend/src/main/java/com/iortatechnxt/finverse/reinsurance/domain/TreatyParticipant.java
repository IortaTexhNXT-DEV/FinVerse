package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

/** A reinsurer's share of a treaty and its commercial terms. Owned by {@link Treaty}. */
@Embeddable
public class TreatyParticipant {

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @ManyToOne(optional = false)
  @JoinColumn(name = "party_id")
  private Party party;

  @Column(name = "share_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal sharePct;

  @Column(name = "commission_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal commissionPct;

  @Column(name = "profit_commission_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal profitCommissionPct;

  @Column(name = "premium_reserve_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal premiumReservePct;

  /** For JPA. */
  protected TreatyParticipant() {}

  /**
   * Creates a participant.
   *
   * @param lineNo line number
   * @param party reinsurer
   * @param terms share and commission terms
   */
  public TreatyParticipant(int lineNo, Party party, ParticipantTerms terms) {
    this.lineNo = lineNo;
    this.party = party;
    this.sharePct = terms.sharePct();
    this.commissionPct = Money.nz(terms.commissionPct());
    this.profitCommissionPct = Money.nz(terms.profitCommissionPct());
    this.premiumReservePct = Money.nz(terms.premiumReservePct());
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

  public BigDecimal getProfitCommissionPct() {
    return profitCommissionPct;
  }

  public BigDecimal getPremiumReservePct() {
    return premiumReservePct;
  }

  /**
   * Terms of a participant.
   *
   * @param sharePct share of the treaty, %
   * @param commissionPct ceding commission, %
   * @param profitCommissionPct profit commission, %
   * @param premiumReservePct premium reserve retained, %
   */
  public record ParticipantTerms(
      BigDecimal sharePct,
      BigDecimal commissionPct,
      BigDecimal profitCommissionPct,
      BigDecimal premiumReservePct) {}
}
