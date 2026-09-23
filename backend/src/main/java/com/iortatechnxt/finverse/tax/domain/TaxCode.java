package com.iortatechnxt.finverse.tax.domain;

import com.iortatechnxt.finverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tax code master (maker-checker): a tax with its rate in percent and the GL account the tax is
 * booked to. Creditable withholding codes carry the BIR alphanumeric tax code (ATC), the payee
 * class it applies to and the nature of income printed on BIR Form 2307 and the QAP.
 *
 * <p>The GL account is the reconciliation control of the worksheets: the posted movement of the
 * account in a period is compared with the sub-ledger documents. Rates here drive the worksheets'
 * rate checks only; the tax actually charged comes from the product (premiums) or the supplier
 * (invoices).
 */
@Entity
@Table(name = "tax_code")
public class TaxCode extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false, length = 20)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "tax_type", nullable = false, length = 20)
  private TaxType taxType;

  @Column(length = 10)
  private String atc;

  @Enumerated(EnumType.STRING)
  @Column(name = "payee_class", length = 12)
  private PayeeClass payeeClass;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal rate;

  @Column(name = "gl_account_code", nullable = false, length = 30)
  private String glAccountCode;

  @Column(name = "income_nature", length = 200)
  private String incomeNature;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  protected TaxCode() {}

  /**
   * Creates a tax code (pending authorization).
   *
   * @param companyId company
   * @param code code (the ATC for withholding codes)
   * @param taxType tax type
   */
  public TaxCode(Long companyId, String code, TaxType taxType) {
    this.companyId = companyId;
    this.code = code;
    this.taxType = taxType;
  }

  /**
   * Whether the code applies on a date.
   *
   * @param date date
   * @return true inside the effective range
   */
  public boolean isEffectiveOn(LocalDate date) {
    return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
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

  public TaxType getTaxType() {
    return taxType;
  }

  public void setTaxType(TaxType taxType) {
    this.taxType = taxType;
  }

  public String getAtc() {
    return atc;
  }

  public void setAtc(String atc) {
    this.atc = atc;
  }

  public PayeeClass getPayeeClass() {
    return payeeClass;
  }

  public void setPayeeClass(PayeeClass payeeClass) {
    this.payeeClass = payeeClass;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public void setRate(BigDecimal rate) {
    this.rate = rate;
  }

  public String getGlAccountCode() {
    return glAccountCode;
  }

  public void setGlAccountCode(String glAccountCode) {
    this.glAccountCode = glAccountCode;
  }

  public String getIncomeNature() {
    return incomeNature;
  }

  public void setIncomeNature(String incomeNature) {
    this.incomeNature = incomeNature;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public void setEffectiveTo(LocalDate effectiveTo) {
    this.effectiveTo = effectiveTo;
  }
}
