package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A charge billed with the premium besides the taxes (BDOI inputs TX-Q04, template PM-04 Charges):
 * CTPL COCAF / LTO authentication fee, notarial fee, policy or documentation fee. It applies to a
 * product, to every product of a line, or to every product when both are blank; it is a fixed
 * amount per policy or a rate of the net premium, with its VAT treatment and the GL account it is
 * credited to. Effective-dated rows with maker-checker; the premium calculator adds them only while
 * the parameter {@code OTHER_CHARGES_ENABLED} is on.
 */
@Entity
@Table(name = "cat_other_charge")
public class OtherCharge extends EffectiveDatedRecord {

  @Column(name = "charge_code", nullable = false, length = 30, updatable = false)
  private String chargeCode;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "line_code", length = 30, updatable = false)
  private String lineCode;

  @Column(name = "product_code", length = 30, updatable = false)
  private String productCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private ChargeBasis basis;

  @Column(nullable = false, precision = 19, scale = 8)
  private BigDecimal value;

  @Enumerated(EnumType.STRING)
  @Column(name = "vat_treatment", nullable = false, length = 12)
  private ChargeVatTreatment vatTreatment;

  @Column(name = "gl_account_code", nullable = false, length = 30)
  private String glAccountCode;

  protected OtherCharge() {}

  /**
   * Creates a charge row pending authorization.
   *
   * @param chargeCode code of the charge
   * @param scope line and product it applies to
   * @param terms name, basis, value, VAT treatment, GL account and dates
   */
  public OtherCharge(String chargeCode, Scope scope, Terms terms) {
    super(terms.effectiveFrom(), terms.effectiveTo());
    this.chargeCode = chargeCode;
    this.lineCode = scope.lineCode();
    this.productCode = scope.productCode();
    apply(terms);
  }

  /**
   * Changes the terms; the row must be authorized again.
   *
   * @param terms new terms
   */
  public void update(Terms terms) {
    setEffectivity(terms.effectiveFrom(), terms.effectiveTo());
    apply(terms);
    markModified();
  }

  private void apply(Terms terms) {
    if (terms.value() == null || terms.value().signum() < 0) {
      throw new BusinessRuleException(
          "CHARGE_VALUE_INVALID", "Enter the amount or rate of the charge, zero or more");
    }
    if (terms.basis() == ChargeBasis.RATE) {
      requirePercent(terms.value(), "The rate of the charge");
    }
    this.name = terms.name();
    this.basis = terms.basis();
    this.value = terms.value();
    this.vatTreatment =
        terms.vatTreatment() == null ? ChargeVatTreatment.VATABLE : terms.vatTreatment();
    this.glAccountCode = terms.glAccountCode();
  }

  /**
   * Whether the row applies to a product of a line.
   *
   * @param product product code
   * @param line line code
   * @return true for the product, its line or every product
   */
  public boolean appliesTo(String product, String line) {
    boolean productMatches = productCode == null || productCode.equals(product);
    boolean lineMatches = lineCode == null || lineCode.equals(line);
    return productMatches && lineMatches;
  }

  /**
   * How specific the row is: product rows win over line rows, which win over rows for every
   * product.
   *
   * @return 2 for a product, 1 for a line, 0 for every product
   */
  public int specificity() {
    if (productCode != null) {
      return 2;
    }
    return lineCode == null ? 0 : 1;
  }

  @Override
  public String catalogReference() {
    return chargeCode + " " + scopeLabel() + " " + getEffectiveFrom();
  }

  @Override
  public String catalogDescription() {
    return name
        + (basis == ChargeBasis.RATE
            ? " " + value.stripTrailingZeros().toPlainString() + " %"
            : " " + value.stripTrailingZeros().toPlainString());
  }

  private String scopeLabel() {
    if (productCode != null) {
      return productCode;
    }
    return lineCode == null ? "ALL" : lineCode;
  }

  public String getChargeCode() {
    return chargeCode;
  }

  public String getName() {
    return name;
  }

  public String getLineCode() {
    return lineCode;
  }

  public String getProductCode() {
    return productCode;
  }

  public ChargeBasis getBasis() {
    return basis;
  }

  public BigDecimal getValue() {
    return value;
  }

  public ChargeVatTreatment getVatTreatment() {
    return vatTreatment;
  }

  public String getGlAccountCode() {
    return glAccountCode;
  }

  /**
   * Line and product a charge applies to; both blank for every product.
   *
   * @param lineCode product line, null for every line
   * @param productCode product, null for every product of the line
   */
  public record Scope(String lineCode, String productCode) {}

  /**
   * Terms of a charge row.
   *
   * @param name name shown on the premium breakdown and the invoice
   * @param basis fixed amount or rate of the net premium
   * @param value amount, or rate in percent
   * @param vatTreatment VAT treatment
   * @param glAccountCode GL account credited with the charge
   * @param effectiveFrom first day
   * @param effectiveTo last day, null when open
   */
  public record Terms(
      String name,
      ChargeBasis basis,
      BigDecimal value,
      ChargeVatTreatment vatTreatment,
      String glAccountCode,
      LocalDate effectiveFrom,
      LocalDate effectiveTo) {}
}
