package com.iortatechnxt.brokerverse.fixedasset.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Depreciation charged to one asset in a run, with its position after the charge. */
@Entity
@Table(name = "fa_depreciation_line")
public class DepreciationLine extends BaseEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "run_id", nullable = false)
  private DepreciationRun run;

  @ManyToOne(optional = false)
  @JoinColumn(name = "asset_id", nullable = false)
  private FixedAsset asset;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(nullable = false)
  private int months;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "accumulated_after", nullable = false, precision = 19, scale = 2)
  private BigDecimal accumulatedAfter;

  @Column(name = "net_book_value_after", nullable = false, precision = 19, scale = 2)
  private BigDecimal netBookValueAfter;

  @Column(name = "batch_no", length = 40)
  private String batchNo;

  protected DepreciationLine() {}

  /**
   * Creates a line from an asset after the charge has been applied to it.
   *
   * @param run run
   * @param asset asset (already charged)
   * @param months months charged
   * @param amount charge
   * @param batchNo journal that posted the charge
   */
  public DepreciationLine(
      DepreciationRun run, FixedAsset asset, int months, BigDecimal amount, String batchNo) {
    this.run = run;
    this.asset = asset;
    this.branchId = asset.getBranchId();
    this.months = months;
    this.amount = amount;
    this.accumulatedAfter = asset.getAccumulatedDepreciation();
    this.netBookValueAfter = asset.netBookValue();
    this.batchNo = batchNo;
  }

  public DepreciationRun getRun() {
    return run;
  }

  public FixedAsset getAsset() {
    return asset;
  }

  public Long getBranchId() {
    return branchId;
  }

  public int getMonths() {
    return months;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getAccumulatedAfter() {
    return accumulatedAfter;
  }

  public BigDecimal getNetBookValueAfter() {
    return netBookValueAfter;
  }

  public String getBatchNo() {
    return batchNo;
  }
}
