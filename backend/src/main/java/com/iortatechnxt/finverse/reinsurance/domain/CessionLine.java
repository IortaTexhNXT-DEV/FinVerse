package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.util.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One share of one risk in a cession: the retention, a treaty participant's share, a facultative
 * participant's share or the unplaced facultative requirement. Amounts in the policy currency, with
 * the premium and commission also in the base currency (accounting and statements). Owned by {@link
 * Cession}.
 */
@Entity
@Table(name = "ri_cession_line")
public class CessionLine {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cession_id")
  private Cession cession;

  @Column(name = "risk_id", nullable = false)
  private Long riskId;

  @Column(name = "risk_line_no", nullable = false)
  private int riskLineNo;

  @Column(name = "risk_description", nullable = false, length = 300)
  private String riskDescription;

  @Column(name = "risk_si", nullable = false, precision = 19, scale = 2)
  private BigDecimal riskSi;

  @Column(name = "risk_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal riskPremium;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private RiLayer layer;

  @Column(name = "treaty_id")
  private Long treatyId;

  @Column(name = "placement_id")
  private Long placementId;

  @Column(name = "party_id")
  private Long partyId;

  @Column(name = "party_code", length = 20)
  private String partyCode;

  @Column(name = "share_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal sharePct;

  @Column(name = "sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal sumInsured;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal premium;

  @Column(name = "commission_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal commissionPct;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal commission;

  @Column(name = "base_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal basePremium;

  @Column(name = "base_commission", nullable = false, precision = 19, scale = 2)
  private BigDecimal baseCommission;

  @Column(name = "posting_ref", length = 100)
  private String postingRef;

  /** For JPA. */
  protected CessionLine() {}

  /**
   * Creates a line; the commission is the premium times the participation's commission %.
   *
   * @param cession owning cession
   * @param risk risk allocated
   * @param who who takes the share
   * @param sumInsured sum insured of the share
   * @param premium premium of the share
   */
  public CessionLine(
      Cession cession, RiskRef risk, Participation who, BigDecimal sumInsured, BigDecimal premium) {
    this.cession = cession;
    this.riskId = risk.riskId();
    this.riskLineNo = risk.lineNo();
    this.riskDescription = risk.description();
    this.riskSi = Money.round(risk.ourSi());
    this.riskPremium = Money.round(risk.ourPremium());
    this.layer = who.layer();
    this.treatyId = who.treatyId();
    this.placementId = who.placementId();
    this.partyId = who.partyId();
    this.partyCode = who.partyCode();
    this.sumInsured = Money.round(sumInsured);
    this.premium = Money.round(premium);
    this.sharePct =
        riskSi.signum() == 0
            ? BigDecimal.ZERO
            : this.sumInsured
                .multiply(HUNDRED)
                .divide(riskSi, Money.RATE_SCALE, RoundingMode.HALF_EVEN);
    this.commissionPct = Money.nz(who.commissionPct());
    this.commission =
        Money.round(
            this.premium.multiply(commissionPct).divide(HUNDRED, Money.RATE_SCALE, Money.ROUNDING));
    this.basePremium = cession.toBase(this.premium);
    this.baseCommission = cession.toBase(this.commission);
  }

  /**
   * Who takes this share.
   *
   * @return participation
   */
  public Participation participation() {
    return new Participation(layer, treatyId, placementId, partyId, partyCode, commissionPct);
  }

  /**
   * The risk allocated.
   *
   * @return risk reference
   */
  public RiskRef risk() {
    return new RiskRef(riskId, riskLineNo, riskDescription, riskSi, riskPremium);
  }

  /**
   * Records the accounting key under which this share was posted.
   *
   * @param ref source reference of the accounting event and open item
   */
  public void markPosted(String ref) {
    this.postingRef = ref;
  }

  public Long getId() {
    return id;
  }

  public Cession getCession() {
    return cession;
  }

  public Long getRiskId() {
    return riskId;
  }

  public int getRiskLineNo() {
    return riskLineNo;
  }

  public String getRiskDescription() {
    return riskDescription;
  }

  public BigDecimal getRiskSi() {
    return riskSi;
  }

  public BigDecimal getRiskPremium() {
    return riskPremium;
  }

  public RiLayer getLayer() {
    return layer;
  }

  public Long getTreatyId() {
    return treatyId;
  }

  public Long getPlacementId() {
    return placementId;
  }

  public Long getPartyId() {
    return partyId;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public BigDecimal getSharePct() {
    return sharePct;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public BigDecimal getCommissionPct() {
    return commissionPct;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public BigDecimal getBasePremium() {
    return basePremium;
  }

  public BigDecimal getBaseCommission() {
    return baseCommission;
  }

  public String getPostingRef() {
    return postingRef;
  }
}
