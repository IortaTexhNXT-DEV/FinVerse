package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything {@link PremiumCalculator} needs, with every rate already resolved (percentages).
 *
 * @param method Appendix A formula
 * @param items rated items (at least one)
 * @param rates taxes, LGT, commission and motor factors in percent
 * @param multiYear multi-year cover (motor: OD/Theft on the multi-year factor, BRNB.112)
 * @param period period basis and dates
 * @param minimumPremium minimum premium of the product (zero for none)
 * @param endorsement endorsement transaction (minimum premium never applies)
 * @param otherCharges other charges billed with the premium (none on an endorsement)
 */
public record PremiumRequest(
    RatingMethod method,
    List<RatedItem> items,
    RatingRates rates,
    boolean multiYear,
    Period period,
    BigDecimal minimumPremium,
    boolean endorsement,
    List<ChargeRate> otherCharges) {

  /** Defensive copy. */
  public PremiumRequest {
    items = items == null ? List.of() : List.copyOf(items);
    otherCharges = otherCharges == null ? List.of() : List.copyOf(otherCharges);
  }

  /**
   * A request without other charges.
   *
   * @param method formula
   * @param items rated items
   * @param rates resolved rates
   * @param multiYear multi-year motor cover
   * @param period period basis and dates
   * @param minimumPremium minimum premium, null for none
   * @param endorsement rating of an endorsement
   */
  public PremiumRequest(
      RatingMethod method,
      List<RatedItem> items,
      RatingRates rates,
      boolean multiYear,
      Period period,
      BigDecimal minimumPremium,
      boolean endorsement) {
    this(method, items, rates, multiYear, period, minimumPremium, endorsement, List.of());
  }

  /**
   * An other charge billed with the premium (template PM-04 Charges).
   *
   * @param code charge code
   * @param name name on the breakdown
   * @param rate true for a rate of the net premium, false for a fixed amount per policy
   * @param value rate in percent, or amount
   * @param vatable VAT is added to the charge
   */
  public record ChargeRate(
      String code, String name, boolean rate, BigDecimal value, boolean vatable) {}

  /**
   * One rated item: a vehicle, a location (sum of its items insured) or any other risk.
   *
   * @param label item label (plate number, address, description)
   * @param sumInsured sum insured
   * @param ratePercent premium rate in percent
   * @param biPremium motor excess bodily injury premium, null when none
   * @param pdPremium motor property damage premium, null when none
   */
  public record RatedItem(
      String label,
      BigDecimal sumInsured,
      BigDecimal ratePercent,
      BigDecimal biPremium,
      BigDecimal pdPremium) {

    /**
     * A non-motor item.
     *
     * @param label label
     * @param sumInsured sum insured
     * @param ratePercent rate in percent
     * @return item
     */
    public static RatedItem of(String label, BigDecimal sumInsured, BigDecimal ratePercent) {
      return new RatedItem(label, sumInsured, ratePercent, null, null);
    }
  }

  /**
   * Resolved rates, all in percent; null means zero.
   *
   * @param dst documentary stamp tax
   * @param premiumTax premium tax
   * @param vatPremium VAT on premium
   * @param fireServiceTax fire service tax
   * @param lgt local government tax of the insurer branch
   * @param commission commission rate
   * @param vatCommission VAT on commission
   * @param odAnnual motor OD/Theft coverage factor, annual cover
   * @param odMultiYear motor OD/Theft coverage factor, multi-year cover
   */
  public record RatingRates(
      BigDecimal dst,
      BigDecimal premiumTax,
      BigDecimal vatPremium,
      BigDecimal fireServiceTax,
      BigDecimal lgt,
      BigDecimal commission,
      BigDecimal vatCommission,
      BigDecimal odAnnual,
      BigDecimal odMultiYear) {}

  /**
   * Period adjustment.
   *
   * @param basis annual, pro-rata or short period
   * @param from period start (pro-rata)
   * @param to period end (pro-rata)
   * @param shortPeriodPercent percent of annual (short period)
   */
  public record Period(
      PeriodBasis basis, LocalDate from, LocalDate to, BigDecimal shortPeriodPercent) {

    /** A full annual premium. */
    public static final Period ANNUAL = new Period(PeriodBasis.ANNUAL, null, null, null);

    /**
     * Endorsement mode: the change is rated for the remaining term, from the endorsement's
     * effective date to the expiry, pro-rata (days) or short period (table percentage of the months
     * remaining).
     *
     * @param basis PRO_RATA or SHORT_PERIOD
     * @param effective endorsement effective date
     * @param expiry policy expiry
     * @param shortPeriodPercent short-period percentage (SHORT_PERIOD), null otherwise
     * @return period
     */
    public static Period remainingTerm(
        PeriodBasis basis, LocalDate effective, LocalDate expiry, BigDecimal shortPeriodPercent) {
      return new Period(basis, effective, expiry, shortPeriodPercent);
    }
  }
}
