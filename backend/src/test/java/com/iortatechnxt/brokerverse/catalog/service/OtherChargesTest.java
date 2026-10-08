package com.iortatechnxt.brokerverse.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.catalog.domain.ChargeBasis;
import com.iortatechnxt.brokerverse.catalog.domain.ChargeVatTreatment;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge.Scope;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge.Terms;
import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.ChargeRate;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.Period;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatedItem;
import com.iortatechnxt.brokerverse.catalog.service.PremiumRequest.RatingRates;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Other charges billed with the premium (template PM-04 Charges, TX-Q04). */
class OtherChargesTest {

  private static final PremiumCalculator CALC = new PremiumCalculator();
  private static final LocalDate DAY = LocalDate.of(2027, 3, 1);

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  private static PremiumRequest request(RatingRates rates, boolean endorsement) {
    return new PremiumRequest(
        RatingMethod.GENERIC,
        List.of(RatedItem.of("Item", bd("1000000"), bd("0.25"))),
        rates,
        false,
        Period.ANNUAL,
        null,
        endorsement,
        List.of(
            new ChargeRate("DOC_FEE", "Documentation fee", false, bd("250"), true),
            new ChargeRate("NOTARIAL", "Notarial fee", true, bd("1"), false)));
  }

  private static RatingRates rates(String vatPremium) {
    return new RatingRates(
        bd("12.5"), bd("0"), bd(vatPremium), null, null, bd("20"), bd("12"), null, null);
  }

  @Test
  void chargesAreAddedToTheGrossWithTheirVat() {
    PremiumBreakdown b = CALC.calculate(request(rates("12"), false));
    assertThat(b.netPremium()).isEqualByComparingTo("2500.00");
    assertThat(b.otherCharges())
        .extracting(PremiumBreakdown.ChargeAmount::code)
        .containsExactly("DOC_FEE", "NOTARIAL");
    PremiumBreakdown.ChargeAmount fee = b.otherCharges().get(0);
    assertThat(fee.amount()).isEqualByComparingTo("250.00");
    assertThat(fee.vat()).isEqualByComparingTo("30.00");
    assertThat(b.otherCharges().get(1).amount()).isEqualByComparingTo("25.00");
    assertThat(b.otherCharges().get(1).vat()).isEqualByComparingTo("0.00");
    assertThat(b.otherChargesTotal()).isEqualByComparingTo("305.00");
    // DST 312.50 + VAT 300.00 + other charges 305.00
    assertThat(b.totalCharges()).isEqualByComparingTo("917.50");
    assertThat(b.grossPremium()).isEqualByComparingTo("3417.50");
  }

  @Test
  void aPremiumWithoutVatTakesTheVatOnCommissionRateForTheCharges() {
    PremiumBreakdown b = CALC.calculate(request(rates("0"), false));
    assertThat(b.otherCharges().get(0).vat()).isEqualByComparingTo("30.00");
  }

  @Test
  void endorsementsCarryNoOtherCharge() {
    PremiumBreakdown b = CALC.calculate(request(rates("12"), true));
    assertThat(b.otherCharges()).isEmpty();
    assertThat(b.otherChargesTotal()).isEqualByComparingTo("0.00");
  }

  @Test
  void theMostSpecificRowInForceAppliesPerCharge() {
    OtherCharge all = charge("DOC_FEE", new Scope(null, null), "100", DAY.minusYears(1));
    OtherCharge line = charge("DOC_FEE", new Scope("FIRE", null), "150", DAY.minusYears(1));
    OtherCharge product = charge("DOC_FEE", new Scope("FIRE", "PAR01"), "200", DAY.minusYears(1));
    OtherCharge future = charge("NOTARIAL", new Scope(null, null), "50", DAY.plusDays(1));
    OtherCharge pending = new OtherCharge("COCAF", new Scope(null, null), terms("10", DAY));
    List<OtherCharge> rows = List.of(all, line, product, future, pending);

    assertThat(OtherChargeService.applicable(rows, "PAR01", "FIRE", DAY))
        .extracting(OtherCharge::getValue)
        .containsExactly(bd("200"));
    assertThat(OtherChargeService.applicable(rows, "PAR02", "FIRE", DAY))
        .extracting(OtherCharge::getValue)
        .containsExactly(bd("150"));
    assertThat(OtherChargeService.applicable(rows, "MTR10", "MOTOR", DAY))
        .extracting(OtherCharge::getValue)
        .containsExactly(bd("100"));
  }

  @Test
  void aRateChargeIsAPercentage() {
    assertThatThrownBy(
            () ->
                new OtherCharge(
                    "X",
                    new Scope(null, null),
                    new Terms(
                        "X",
                        ChargeBasis.RATE,
                        bd("120"),
                        ChargeVatTreatment.EXEMPT,
                        "4100",
                        DAY,
                        null)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                new OtherCharge(
                    "X",
                    new Scope(null, null),
                    new Terms("X", ChargeBasis.AMOUNT, bd("-1"), null, "4100", DAY, null)))
        .isInstanceOf(BusinessRuleException.class);
  }

  private static Terms terms(String value, LocalDate from) {
    return new Terms(
        "Charge", ChargeBasis.AMOUNT, bd(value), ChargeVatTreatment.VATABLE, "4100", from, null);
  }

  private static OtherCharge charge(String code, Scope scope, String value, LocalDate from) {
    OtherCharge c = new OtherCharge(code, scope, terms(value, from));
    c.authorize("checker", Instant.now());
    assertThat(c.getRecordStatus()).isEqualTo(RecordStatus.ACTIVE);
    return c;
  }
}
