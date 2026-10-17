package com.iortatechnxt.brokerverse.fixedasset.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * A posted monthly depreciation run. At most one per company and period (unique key), which makes
 * the run idempotent: posting a period again returns the existing run.
 */
@Entity
@Table(name = "fa_depreciation_run")
public class DepreciationRun extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false, length = 7)
  private String period;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "asset_count", nullable = false)
  private int assetCount;

  @Column(name = "total_depreciation", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalDepreciation = BigDecimal.ZERO;

  @OneToMany(mappedBy = "run", cascade = CascadeType.ALL)
  private final List<DepreciationLine> lines = new ArrayList<>();

  protected DepreciationRun() {}

  /**
   * Starts a run.
   *
   * @param companyId company
   * @param period period (month)
   */
  public DepreciationRun(Long companyId, YearMonth period) {
    this.companyId = companyId;
    this.period = period.toString();
    this.periodEnd = period.atEndOfMonth();
  }

  /**
   * Adds a line and updates the totals.
   *
   * @param line line
   */
  public void addLine(DepreciationLine line) {
    lines.add(line);
    assetCount = lines.size();
    totalDepreciation = totalDepreciation.add(line.getAmount());
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getPeriod() {
    return period;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public int getAssetCount() {
    return assetCount;
  }

  public BigDecimal getTotalDepreciation() {
    return totalDepreciation;
  }

  public List<DepreciationLine> getLines() {
    return List.copyOf(lines);
  }
}
