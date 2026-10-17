package com.iortatechnxt.brokerverse.fixedasset.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Fixed asset category: the GL accounts an asset posts to and its default depreciation policy
 * (method, useful life, residual value %). Maintained under maker-checker control.
 */
@Entity
@Table(name = "fa_category")
public class AssetCategory extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false, length = 20)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "asset_account", nullable = false, length = 30)
  private String assetAccount;

  @Column(name = "accumulated_depreciation_account", nullable = false, length = 30)
  private String accumulatedDepreciationAccount;

  @Column(name = "depreciation_expense_account", nullable = false, length = 30)
  private String depreciationExpenseAccount;

  @Enumerated(EnumType.STRING)
  @Column(name = "depreciation_method", nullable = false, length = 20)
  private DepreciationMethod depreciationMethod;

  @Column(name = "useful_life_months", nullable = false)
  private int usefulLifeMonths;

  @Column(name = "residual_percent", nullable = false, precision = 7, scale = 4)
  private BigDecimal residualPercent = BigDecimal.ZERO;

  protected AssetCategory() {}

  /**
   * Creates a category (pending authorization).
   *
   * @param companyId company
   * @param code category code
   * @param name name
   */
  public AssetCategory(Long companyId, String code, String name) {
    this.companyId = companyId;
    this.code = code;
    this.name = name;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAssetAccount() {
    return assetAccount;
  }

  public void setAssetAccount(String assetAccount) {
    this.assetAccount = assetAccount;
  }

  public String getAccumulatedDepreciationAccount() {
    return accumulatedDepreciationAccount;
  }

  public void setAccumulatedDepreciationAccount(String accumulatedDepreciationAccount) {
    this.accumulatedDepreciationAccount = accumulatedDepreciationAccount;
  }

  public String getDepreciationExpenseAccount() {
    return depreciationExpenseAccount;
  }

  public void setDepreciationExpenseAccount(String depreciationExpenseAccount) {
    this.depreciationExpenseAccount = depreciationExpenseAccount;
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

  public BigDecimal getResidualPercent() {
    return residualPercent;
  }

  public void setResidualPercent(BigDecimal residualPercent) {
    this.residualPercent = residualPercent;
  }
}
