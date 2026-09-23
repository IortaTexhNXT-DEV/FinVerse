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

/**
 * One reinsurer's share of a claim movement (base currency): of a reserve change (reinsurers' share
 * of outstanding claims), of a payment (recovery due) or of a salvage recovery (negative). Owned by
 * {@link RiClaimMovement}.
 */
@Entity
@Table(name = "ri_claim_share")
public class RiClaimShare {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movement_id")
  private RiClaimMovement movement;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private RiLayer layer;

  @Column(name = "treaty_id")
  private Long treatyId;

  @Column(name = "layer_no")
  private Integer layerNo;

  @Column(name = "placement_id")
  private Long placementId;

  @Column(name = "party_id", nullable = false)
  private Long partyId;

  @Column(name = "party_code", nullable = false, length = 20)
  private String partyCode;

  @Column(name = "share_pct", nullable = false, precision = 19, scale = 8)
  private BigDecimal sharePct;

  @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal baseAmount;

  @Column(name = "posting_ref", length = 100)
  private String postingRef;

  /** For JPA. */
  protected RiClaimShare() {}

  /**
   * Creates a share.
   *
   * @param movement owning movement
   * @param who layer, contract and reinsurer
   * @param layerNo excess of loss layer number, null for proportional shares
   * @param sharePct share of the movement, %
   * @param baseAmount signed base currency amount
   */
  public RiClaimShare(
      RiClaimMovement movement,
      Participation who,
      Integer layerNo,
      BigDecimal sharePct,
      BigDecimal baseAmount) {
    this.movement = movement;
    this.layer = who.layer();
    this.treatyId = who.treatyId();
    this.placementId = who.placementId();
    this.partyId = who.partyId();
    this.partyCode = who.partyCode();
    this.layerNo = layerNo;
    this.sharePct = sharePct;
    this.baseAmount = Money.round(baseAmount);
  }

  /**
   * Key of the contract ("T12", "F5", "T3L1" for an excess of loss layer).
   *
   * @return key
   */
  public String contractKey() {
    String key = treatyId != null ? "T" + treatyId : "F" + placementId;
    return layerNo == null ? key : key + "L" + layerNo;
  }

  /**
   * Records the accounting key under which this share was posted.
   *
   * @param ref source reference
   */
  public void markPosted(String ref) {
    this.postingRef = ref;
  }

  public Long getId() {
    return id;
  }

  public RiClaimMovement getMovement() {
    return movement;
  }

  public RiLayer getLayer() {
    return layer;
  }

  public Long getTreatyId() {
    return treatyId;
  }

  public Integer getLayerNo() {
    return layerNo;
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

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public String getPostingRef() {
    return postingRef;
  }
}
