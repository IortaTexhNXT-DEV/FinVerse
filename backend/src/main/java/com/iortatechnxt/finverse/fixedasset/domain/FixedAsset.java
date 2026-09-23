package com.iortatechnxt.finverse.fixedasset.domain;

import com.iortatechnxt.finverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Fixed asset register entry.
 *
 * <p>Registered by a maker ({@link AssetStatus#PENDING_CAPITALIZATION}), capitalized by a checker
 * (posting the acquisition or, for assets taken over from a legacy register, the opening balance),
 * then depreciated monthly until fully depreciated or disposed. Depreciation starts in the month of
 * acquisition (full-month convention); a take-on asset continues from the month of its take-on date
 * with the opening accumulated depreciation and months already charged.
 */
@Entity
@Table(name = "fa_asset")
public class FixedAsset extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private AssetCategory category;

  @Column(name = "tag_no", nullable = false, length = 30)
  private String tagNo;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(name = "supplier_code", length = 30)
  private String supplierCode;

  @Column(name = "acquisition_date", nullable = false)
  private LocalDate acquisitionDate;

  @Column(name = "acquisition_cost", nullable = false, precision = 19, scale = 2)
  private BigDecimal acquisitionCost;

  @Column(name = "residual_value", nullable = false, precision = 19, scale = 2)
  private BigDecimal residualValue = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "depreciation_method", nullable = false, length = 20)
  private DepreciationMethod depreciationMethod;

  @Column(name = "useful_life_months", nullable = false)
  private int usefulLifeMonths;

  @Column(length = 120)
  private String location;

  @Column(length = 120)
  private String custodian;

  @Column(name = "settlement_account", length = 30)
  private String settlementAccount;

  @Column(name = "take_on", nullable = false)
  private boolean takeOn;

  @Column(name = "capitalization_date", nullable = false)
  private LocalDate capitalizationDate;

  @Column(name = "opening_accumulated_depreciation", nullable = false, precision = 19, scale = 2)
  private BigDecimal openingAccumulatedDepreciation = BigDecimal.ZERO;

  @Column(name = "opening_months", nullable = false)
  private int openingMonths;

  @Column(name = "accumulated_depreciation", nullable = false, precision = 19, scale = 2)
  private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

  @Column(name = "months_depreciated", nullable = false)
  private int monthsDepreciated;

  @Column(name = "last_depreciation_period", length = 7)
  private String lastDepreciationPeriod;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AssetStatus status = AssetStatus.PENDING_CAPITALIZATION;

  @Column(name = "capitalization_batch_no", length = 40)
  private String capitalizationBatchNo;

  @Column(name = "disposal_date")
  private LocalDate disposalDate;

  protected FixedAsset() {}

  /**
   * Registers an asset (pending capitalization).
   *
   * @param category category
   * @param branchId owning branch
   * @param tagNo asset tag number
   * @param acquisitionDate acquisition (available for use) date
   * @param acquisitionCost cost
   */
  public FixedAsset(
      AssetCategory category,
      Long branchId,
      String tagNo,
      LocalDate acquisitionDate,
      BigDecimal acquisitionCost) {
    this.companyId = category.getCompanyId();
    this.category = category;
    this.branchId = branchId;
    this.tagNo = tagNo;
    this.acquisitionDate = acquisitionDate;
    this.acquisitionCost = acquisitionCost;
    this.depreciationMethod = category.getDepreciationMethod();
    this.usefulLifeMonths = category.getUsefulLifeMonths();
    this.capitalizationDate = acquisitionDate;
  }

  /** Fails unless the asset is still awaiting capitalization (only then may it be edited). */
  public void requirePending() {
    if (status != AssetStatus.PENDING_CAPITALIZATION) {
      throw new BusinessRuleException(
          "ASSET_NOT_PENDING", "Asset " + tagNo + " is already capitalized");
    }
  }

  /**
   * Sets the take-on position (asset brought over from a previous register).
   *
   * @param takeOnDate date of the opening balance
   * @param accumulated accumulated depreciation at take-on
   * @param months months already depreciated at take-on
   */
  public void takeOnWith(LocalDate takeOnDate, BigDecimal accumulated, int months) {
    this.takeOn = true;
    this.capitalizationDate = takeOnDate;
    this.openingAccumulatedDepreciation = accumulated;
    this.openingMonths = months;
    this.accumulatedDepreciation = accumulated;
    this.monthsDepreciated = months;
  }

  /**
   * Capitalizes the asset (checker action, four eyes).
   *
   * @param checker authorizing user
   * @param when timestamp
   */
  public void capitalize(String checker, Instant when) {
    requirePending();
    authorize(checker, when);
    status = isFullyDepreciated() ? AssetStatus.FULLY_DEPRECIATED : AssetStatus.ACTIVE;
  }

  /**
   * Books a depreciation charge.
   *
   * @param period last month charged
   * @param months number of months charged (more than one when catching up)
   * @param amount charge
   */
  public void applyDepreciation(YearMonth period, int months, BigDecimal amount) {
    accumulatedDepreciation = accumulatedDepreciation.add(amount);
    monthsDepreciated += months;
    lastDepreciationPeriod = period.toString();
    if (isFullyDepreciated()) {
      status = AssetStatus.FULLY_DEPRECIATED;
    }
  }

  /**
   * Takes back depreciation charged for the month of disposal and later (none is charged in the
   * month of disposal): accumulated depreciation returns to its value at the end of {@code
   * lastMonth}.
   *
   * @param lastMonth last month that keeps its charge (the month before the disposal)
   * @param months number of monthly charges taken back
   * @param amount depreciation taken back
   */
  public void reverseDepreciation(YearMonth lastMonth, int months, BigDecimal amount) {
    requireInService();
    accumulatedDepreciation = accumulatedDepreciation.subtract(amount);
    monthsDepreciated -= months;
    lastDepreciationPeriod = lastMonth.toString();
    if (status == AssetStatus.FULLY_DEPRECIATED && !isFullyDepreciated()) {
      status = AssetStatus.ACTIVE;
    }
  }

  /**
   * Derecognizes the asset.
   *
   * @param date disposal date
   */
  public void dispose(LocalDate date) {
    requireInService();
    status = AssetStatus.DISPOSED;
    disposalDate = date;
  }

  /**
   * Moves the asset to another branch.
   *
   * @param toBranchId receiving branch
   * @param newLocation location at the receiving branch
   * @param newCustodian custodian at the receiving branch
   */
  public void transferTo(Long toBranchId, String newLocation, String newCustodian) {
    requireInService();
    if (toBranchId.equals(branchId)) {
      throw new BusinessRuleException("SAME_BRANCH", "Asset " + tagNo + " is already there");
    }
    branchId = toBranchId;
    location = newLocation;
    custodian = newCustodian;
    if (status == AssetStatus.ACTIVE) {
      status = AssetStatus.TRANSFERRED;
    }
  }

  /** Fails unless the asset is carried in the books. */
  public void requireInService() {
    if (!status.isInService()) {
      throw new BusinessRuleException(
          "ASSET_NOT_IN_SERVICE", "Asset " + tagNo + " is " + status + ", not in service");
    }
  }

  /**
   * Net book value (cost less accumulated depreciation).
   *
   * @return net book value
   */
  public BigDecimal netBookValue() {
    return acquisitionCost.subtract(accumulatedDepreciation);
  }

  /**
   * Whether net book value has reached the residual value.
   *
   * @return true when nothing is left to depreciate
   */
  public boolean isFullyDepreciated() {
    return netBookValue().compareTo(residualValue) <= 0;
  }

  /**
   * Last month already depreciated (the month before the first depreciation month when none).
   *
   * @return month
   */
  public YearMonth lastDepreciatedMonth() {
    if (lastDepreciationPeriod != null) {
      return YearMonth.parse(lastDepreciationPeriod);
    }
    YearMonth start = takeOn ? YearMonth.from(capitalizationDate) : YearMonth.from(acquisitionDate);
    return start.minusMonths(1);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public AssetCategory getCategory() {
    return category;
  }

  public String getTagNo() {
    return tagNo;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public void setCostCenter(String costCenter) {
    this.costCenter = costCenter;
  }

  public String getSupplierCode() {
    return supplierCode;
  }

  public void setSupplierCode(String supplierCode) {
    this.supplierCode = supplierCode;
  }

  public LocalDate getAcquisitionDate() {
    return acquisitionDate;
  }

  public BigDecimal getAcquisitionCost() {
    return acquisitionCost;
  }

  public BigDecimal getResidualValue() {
    return residualValue;
  }

  public void setResidualValue(BigDecimal residualValue) {
    this.residualValue = residualValue;
  }

  public DepreciationMethod getDepreciationMethod() {
    return depreciationMethod;
  }

  public void setDepreciationMethod(DepreciationMethod depreciationMethod) {
    this.depreciationMethod = depreciationMethod;
  }

  public int getUsefulLifeMonths() {
    return usefulLifeMonths;
  }

  public void setUsefulLifeMonths(int usefulLifeMonths) {
    this.usefulLifeMonths = usefulLifeMonths;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getCustodian() {
    return custodian;
  }

  public void setCustodian(String custodian) {
    this.custodian = custodian;
  }

  public String getSettlementAccount() {
    return settlementAccount;
  }

  public void setSettlementAccount(String settlementAccount) {
    this.settlementAccount = settlementAccount;
  }

  public boolean isTakeOn() {
    return takeOn;
  }

  public LocalDate getCapitalizationDate() {
    return capitalizationDate;
  }

  public void setCapitalizationDate(LocalDate capitalizationDate) {
    this.capitalizationDate = capitalizationDate;
  }

  public BigDecimal getOpeningAccumulatedDepreciation() {
    return openingAccumulatedDepreciation;
  }

  public int getOpeningMonths() {
    return openingMonths;
  }

  public BigDecimal getAccumulatedDepreciation() {
    return accumulatedDepreciation;
  }

  public int getMonthsDepreciated() {
    return monthsDepreciated;
  }

  public String getLastDepreciationPeriod() {
    return lastDepreciationPeriod;
  }

  public AssetStatus getStatus() {
    return status;
  }

  public String getCapitalizationBatchNo() {
    return capitalizationBatchNo;
  }

  public void setCapitalizationBatchNo(String capitalizationBatchNo) {
    this.capitalizationBatchNo = capitalizationBatchNo;
  }

  public LocalDate getDisposalDate() {
    return disposalDate;
  }
}
