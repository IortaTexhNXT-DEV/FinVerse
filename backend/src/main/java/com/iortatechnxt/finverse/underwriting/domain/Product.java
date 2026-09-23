package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Insurance product (class of business) with its commission, earning basis and tax configuration.
 * Maintained under maker-checker control: a product can be used only once authorized.
 */
@Entity
@Table(name = "uw_product")
public class Product extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false, length = 20)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "business_line", nullable = false, length = 20)
  private String businessLine;

  @Column(name = "default_commission_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal defaultCommissionRate;

  @Enumerated(EnumType.STRING)
  @Column(name = "upr_basis", nullable = false, length = 20)
  private UprBasis uprBasis;

  @Column(name = "dst_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal dstRate;

  @Column(name = "vat_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal vatRate;

  @Column(name = "lgt_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal lgtRate;

  @Column(name = "fst_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal fstRate;

  @Column(name = "premium_tax_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal premiumTaxRate;

  @Column(name = "policy_fee", nullable = false, precision = 19, scale = 2)
  private BigDecimal policyFee;

  @Column(name = "open_cover_allowed", nullable = false)
  private boolean openCoverAllowed;

  protected Product() {}

  /**
   * Creates a product pending authorization.
   *
   * @param companyId company
   * @param code product code
   * @param terms attributes
   */
  public Product(Long companyId, String code, ProductTerms terms) {
    this.companyId = companyId;
    this.code = code;
    apply(terms);
  }

  /**
   * Changes the product; it must be re-authorized before further use.
   *
   * @param terms new attributes
   */
  public void update(ProductTerms terms) {
    apply(terms);
    markModified();
  }

  private void apply(ProductTerms t) {
    this.name = t.name();
    this.businessLine = t.businessLine();
    this.defaultCommissionRate = t.defaultCommissionRate();
    this.uprBasis = t.uprBasis();
    this.dstRate = t.taxes().dst();
    this.vatRate = t.taxes().vat();
    this.lgtRate = t.taxes().lgt();
    this.fstRate = t.taxes().fst();
    this.premiumTaxRate = t.taxes().premiumTax();
    this.policyFee = t.policyFee();
    this.openCoverAllowed = t.openCoverAllowed();
  }

  /**
   * Tax configuration.
   *
   * @return rates
   */
  public TaxRates taxRates() {
    return new TaxRates(dstRate, vatRate, lgtRate, fstRate, premiumTaxRate);
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

  public String getBusinessLine() {
    return businessLine;
  }

  public BigDecimal getDefaultCommissionRate() {
    return defaultCommissionRate;
  }

  public UprBasis getUprBasis() {
    return uprBasis;
  }

  public BigDecimal getPolicyFee() {
    return policyFee;
  }

  public boolean isOpenCoverAllowed() {
    return openCoverAllowed;
  }
}
