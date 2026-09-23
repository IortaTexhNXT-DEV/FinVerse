package com.iortatechnxt.finverse.underwriting;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.domain.PremiumInput;
import com.iortatechnxt.finverse.underwriting.domain.TaxRates;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PremiumBreakdownTest {

  private static final TaxRates FIRE_TAXES =
      new TaxRates(
          new BigDecimal("12.5"),
          new BigDecimal("12"),
          new BigDecimal("0.75"),
          new BigDecimal("2"),
          BigDecimal.ZERO);

  private static PremiumInput input(
      String gross, String share, boolean leader, String commission, String fee) {
    return new PremiumInput(
        new BigDecimal("10000000"),
        new BigDecimal(gross),
        new BigDecimal("10"),
        new BigDecimal("5"),
        new BigDecimal(share),
        leader,
        FIRE_TAXES,
        new BigDecimal(fee),
        new BigDecimal(commission),
        new BigDecimal("10"));
  }

  @Test
  void directPolicyAtFullShare() {
    PremiumBreakdown b = PremiumBreakdown.calculate(input("100000", "100", false, "20", "250"));

    assertThat(b.getDiscountAmount()).isEqualByComparingTo("10000.00");
    assertThat(b.getLoadingAmount()).isEqualByComparingTo("5000.00");
    assertThat(b.getNetPremium()).isEqualByComparingTo("95000.00");
    assertThat(b.getOurNetPremium()).isEqualByComparingTo("95000.00");
    assertThat(b.getBilledPremium()).isEqualByComparingTo("95000.00");
    assertThat(b.getDst()).isEqualByComparingTo("11875.00");
    assertThat(b.getVat()).isEqualByComparingTo("11400.00");
    assertThat(b.getLgt()).isEqualByComparingTo("712.50");
    assertThat(b.getFst()).isEqualByComparingTo("1900.00");
    assertThat(b.getPolicyFee()).isEqualByComparingTo("250.00");
    assertThat(b.taxesAndCharges()).isEqualByComparingTo("26137.50");
    assertThat(b.getTotalDue()).isEqualByComparingTo("121137.50");
    assertThat(b.getCommission()).isEqualByComparingTo("19000.00");
    assertThat(b.getWithholdingTax()).isEqualByComparingTo("1900.00");
    assertThat(b.getNetCommission()).isEqualByComparingTo("17100.00");
    assertThat(b.getOurSumInsured()).isEqualByComparingTo("10000000.00");
    assertThat(b.isFinancial()).isTrue();
  }

  @Test
  void coinsuranceFollowerBillsOnlyItsShare() {
    PremiumBreakdown b = PremiumBreakdown.calculate(input("100000", "60", false, "0", "0"));

    assertThat(b.getOurGrossPremium()).isEqualByComparingTo("60000.00");
    assertThat(b.getOurDiscount()).isEqualByComparingTo("6000.00");
    assertThat(b.getOurLoading()).isEqualByComparingTo("3000.00");
    assertThat(b.getOurNetPremium()).isEqualByComparingTo("57000.00");
    assertThat(b.getCoinsurerPremium()).isEqualByComparingTo("0.00");
    assertThat(b.getBilledPremium()).isEqualByComparingTo("57000.00");
    assertThat(b.getOurSumInsured()).isEqualByComparingTo("6000000.00");
    assertThat(b.getCommission()).isEqualByComparingTo("0.00");
  }

  @Test
  void coinsuranceLeaderBillsTheWholePremiumButTaxesOnlyItsShare() {
    PremiumBreakdown b = PremiumBreakdown.calculate(input("100000", "60", true, "0", "0"));

    assertThat(b.getCoinsurerPremium()).isEqualByComparingTo("38000.00");
    assertThat(b.getBilledPremium()).isEqualByComparingTo("95000.00");
    assertThat(b.getDst()).isEqualByComparingTo("7125.00");
    assertThat(b.getTotalDue()).isEqualByComparingTo(b.getBilledPremium().add(b.taxesAndCharges()));
  }

  @Test
  void returnPremiumIsNegativeThroughout() {
    PremiumBreakdown b = PremiumBreakdown.calculate(input("-50000", "100", false, "20", "0"));

    assertThat(b.getNetPremium()).isEqualByComparingTo("-47500.00");
    assertThat(b.getDst()).isEqualByComparingTo("-5937.50");
    assertThat(b.getTotalDue()).isNegative();
    assertThat(b.getCommission()).isEqualByComparingTo("-9500.00");
    assertThat(b.getNetCommission()).isEqualByComparingTo("-8550.00");
  }

  @Test
  void emptyBreakdownIsNotFinancialAndNullTaxesMeanNone() {
    assertThat(new PremiumBreakdown().isFinancial()).isFalse();
    PremiumBreakdown b =
        PremiumBreakdown.calculate(
            new PremiumInput(
                BigDecimal.ONE,
                new BigDecimal("1000"),
                null,
                null,
                new BigDecimal("100"),
                false,
                null,
                null,
                null,
                null));
    assertThat(b.getTotalDue()).isEqualByComparingTo("1000.00");
    assertThat(TaxRates.none().dst()).isZero();
  }
}
