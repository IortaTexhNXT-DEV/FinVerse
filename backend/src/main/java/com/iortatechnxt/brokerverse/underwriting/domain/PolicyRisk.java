package com.iortatechnxt.brokerverse.underwriting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One insured risk (section) of a policy. Owned by {@link Policy}. */
@Entity
@Table(name = "uw_policy_risk")
public class PolicyRisk {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "policy_id")
  private Policy policy;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(nullable = false, length = 300)
  private String description;

  @Column(name = "sum_insured", nullable = false, precision = 19, scale = 2)
  private BigDecimal sumInsured;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal rate;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal premium;

  @Column(length = 120)
  private String occupation;

  @Column(name = "accumulation_zone", length = 40)
  private String accumulationZone;

  @Column(name = "vessel_name", length = 120)
  private String vesselName;

  @Column(name = "voyage_from", length = 80)
  private String voyageFrom;

  @Column(name = "voyage_to", length = 80)
  private String voyageTo;

  @Column(name = "sail_date")
  private LocalDate sailDate;

  @Column(name = "bl_no", length = 40)
  private String blNo;

  @Column(name = "bl_date")
  private LocalDate blDate;

  @Column(name = "lc_no", length = 40)
  private String lcNo;

  @Column(name = "bank_name", length = 120)
  private String bankName;

  @Column(name = "valuation_basis", length = 120)
  private String valuationBasis;

  protected PolicyRisk() {}

  /**
   * Creates a risk line.
   *
   * @param policy owning policy
   * @param lineNo line number
   * @param v values
   */
  PolicyRisk(Policy policy, int lineNo, RiskValues v) {
    this.policy = policy;
    this.lineNo = lineNo;
    this.description = v.description();
    this.sumInsured = v.sumInsured();
    this.rate = v.rate();
    this.premium = v.premium();
    this.occupation = v.occupation();
    this.accumulationZone = v.accumulationZone();
    MarineDetails m = v.marine();
    if (m != null) {
      this.vesselName = m.vesselName();
      this.voyageFrom = m.voyageFrom();
      this.voyageTo = m.voyageTo();
      this.sailDate = m.sailDate();
      this.blNo = m.blNo();
      this.blDate = m.blDate();
      this.lcNo = m.lcNo();
      this.bankName = m.bankName();
      this.valuationBasis = m.valuationBasis();
    }
  }

  /**
   * Shipment details.
   *
   * @return marine details, null when the risk is not a shipment
   */
  public MarineDetails marine() {
    if (vesselName == null && lcNo == null && blNo == null) {
      return null;
    }
    return new MarineDetails(
        vesselName, voyageFrom, voyageTo, sailDate, blNo, blDate, lcNo, bankName, valuationBasis);
  }

  /**
   * Values of this risk (used when copying risks to a renewal or certificate).
   *
   * @return values
   */
  public RiskValues values() {
    return new RiskValues(
        description, sumInsured, rate, premium, occupation, accumulationZone, marine());
  }

  public Long getId() {
    return id;
  }

  public Policy getPolicy() {
    return policy;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getSumInsured() {
    return sumInsured;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public BigDecimal getPremium() {
    return premium;
  }

  public String getOccupation() {
    return occupation;
  }

  public String getAccumulationZone() {
    return accumulationZone;
  }
}
