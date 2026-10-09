package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.ChargeBasis;
import com.iortatechnxt.brokerverse.catalog.domain.ChargeVatTreatment;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerTaxStatus;
import com.iortatechnxt.brokerverse.catalog.domain.RateCode;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.ChargeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The levies of the premium that depend on more than the product line (BDOI inputs TX-Q02, TX-Q04):
 * VAT or premium tax by the tax status of the insurer, and the other charges billed with the
 * premium while their billing is switched on.
 */
@Component
@Transactional(readOnly = true)
public class PremiumLevies {

  private final InsurerService insurers;
  private final RateResolver resolver;
  private final OtherChargeService otherCharges;

  /**
   * Creates the levies.
   *
   * @param insurers insurers (tax status)
   * @param resolver rate lookups
   * @param otherCharges other charges
   */
  public PremiumLevies(
      InsurerService insurers, RateResolver resolver, OtherChargeService otherCharges) {
    this.insurers = insurers;
    this.resolver = resolver;
    this.otherCharges = otherCharges;
  }

  /**
   * VAT or premium tax on the premium. Without a tax status of the insurer the rates of the line
   * apply. A VAT-registered insurer charges VAT and no premium tax, a non-VAT insurer premium tax
   * and no VAT: the rate of the line, or the rate for every line when the line carries none.
   *
   * @param companyId company
   * @param insurerCode insurer, null when none is chosen yet
   * @param line product line
   * @param date rating date
   * @return premium tax and VAT rates in percent
   */
  public TaxOnPremium taxOnPremium(
      Long companyId, String insurerCode, String line, LocalDate date) {
    InsurerTaxStatus status =
        insurerCode == null ? null : insurers.taxStatus(companyId, insurerCode);
    BigDecimal premiumTax =
        status == InsurerTaxStatus.VAT_REGISTERED
            ? BigDecimal.ZERO
            : levy(RateCode.PREMIUM_TAX, line, date, status);
    BigDecimal vat =
        status == InsurerTaxStatus.NON_VAT
            ? BigDecimal.ZERO
            : levy(RateCode.VAT_PREMIUM, line, date, status);
    return new TaxOnPremium(premiumTax, vat);
  }

  private BigDecimal levy(RateCode code, String line, LocalDate date, InsurerTaxStatus status) {
    BigDecimal lineRate = resolver.rate(code, line, date).orElse(BigDecimal.ZERO);
    if (status == null || lineRate.signum() > 0) {
      return lineRate;
    }
    return resolver.rate(code, null, date).orElse(BigDecimal.ZERO);
  }

  /**
   * The other charges of a product while they are billed (template PM-04 Charges).
   *
   * @param product product
   * @param date rating date
   * @return charges for the calculator
   */
  public List<ChargeRate> otherCharges(RiskProduct product, LocalDate date) {
    return otherCharges.applicable(product.getCode(), product.getLineCode(), date).stream()
        .map(
            c ->
                new ChargeRate(
                    c.getChargeCode(),
                    c.getName(),
                    c.getBasis() == ChargeBasis.RATE,
                    c.getValue(),
                    c.getVatTreatment() == ChargeVatTreatment.VATABLE))
        .toList();
  }

  /**
   * Premium tax and VAT rates of a premium.
   *
   * @param premiumTax premium tax rate in percent
   * @param vat VAT rate in percent
   */
  public record TaxOnPremium(BigDecimal premiumTax, BigDecimal vat) {}
}
