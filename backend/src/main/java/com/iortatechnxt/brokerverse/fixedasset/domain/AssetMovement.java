package com.iortatechnxt.brokerverse.fixedasset.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An addition, disposal or inter-branch transfer of a fixed asset, with the values moved and the
 * journal that accounted for it. Immutable history feeding the asset movement schedule.
 */
@Entity
@Table(name = "fa_movement")
public class AssetMovement extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "asset_id", nullable = false)
  private FixedAsset asset;

  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type", nullable = false, length = 20)
  private MovementType movementType;

  @Column(name = "movement_date", nullable = false)
  private LocalDate movementDate;

  @Column(name = "from_branch_id")
  private Long fromBranchId;

  @Column(name = "to_branch_id")
  private Long toBranchId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal cost;

  @Column(name = "accumulated_depreciation", nullable = false, precision = 19, scale = 2)
  private BigDecimal accumulatedDepreciation;

  @Column(name = "net_book_value", nullable = false, precision = 19, scale = 2)
  private BigDecimal netBookValue;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal proceeds = BigDecimal.ZERO;

  @Column(name = "gain_loss", nullable = false, precision = 19, scale = 2)
  private BigDecimal gainLoss = BigDecimal.ZERO;

  @Column(length = 60)
  private String reference;

  @Column(length = 250)
  private String remarks;

  @Column(name = "batch_no", length = 40)
  private String batchNo;

  protected AssetMovement() {}

  /**
   * Records a movement at the asset's current values.
   *
   * @param asset asset
   * @param type movement type
   * @param date movement date
   * @param fromBranchId sending (or owning) branch
   * @param toBranchId receiving branch (transfers only)
   */
  public AssetMovement(
      FixedAsset asset, MovementType type, LocalDate date, Long fromBranchId, Long toBranchId) {
    this.companyId = asset.getCompanyId();
    this.asset = asset;
    this.movementType = type;
    this.movementDate = date;
    this.fromBranchId = fromBranchId;
    this.toBranchId = toBranchId;
    this.cost = asset.getAcquisitionCost();
    this.accumulatedDepreciation = asset.getAccumulatedDepreciation();
    this.netBookValue = asset.netBookValue();
  }

  /**
   * Records sale proceeds and the resulting gain (positive) or loss (negative).
   *
   * @param saleProceeds proceeds
   * @param result gain or loss
   */
  public void settle(BigDecimal saleProceeds, BigDecimal result) {
    this.proceeds = saleProceeds;
    this.gainLoss = result;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public FixedAsset getAsset() {
    return asset;
  }

  public MovementType getMovementType() {
    return movementType;
  }

  public LocalDate getMovementDate() {
    return movementDate;
  }

  public Long getFromBranchId() {
    return fromBranchId;
  }

  public Long getToBranchId() {
    return toBranchId;
  }

  public BigDecimal getCost() {
    return cost;
  }

  public BigDecimal getAccumulatedDepreciation() {
    return accumulatedDepreciation;
  }

  public BigDecimal getNetBookValue() {
    return netBookValue;
  }

  public BigDecimal getProceeds() {
    return proceeds;
  }

  public BigDecimal getGainLoss() {
    return gainLoss;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public void setBatchNo(String batchNo) {
    this.batchNo = batchNo;
  }
}
