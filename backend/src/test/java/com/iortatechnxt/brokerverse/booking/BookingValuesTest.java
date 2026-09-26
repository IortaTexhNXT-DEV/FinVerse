package com.iortatechnxt.brokerverse.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.domain.AutoBookRule;
import com.iortatechnxt.brokerverse.booking.domain.BatchRow;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveRule;
import com.iortatechnxt.brokerverse.booking.domain.InsurerShare;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponent;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.booking.service.BookingEvents;
import com.iortatechnxt.brokerverse.booking.service.BookingEvents.ShareAmounts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Pure calculations of the booking values: components, commission, shares and rules. */
class BookingValuesTest {

  private static final PremiumComponents PREMIUM =
      new PremiumComponents(
          new BigDecimal("13595.00"),
          new BigDecimal("1699.50"),
          new BigDecimal("1631.40"),
          new BigDecimal("101.96"),
          BigDecimal.ZERO,
          null);

  @Test
  void componentsAddScaleAndNegate() {
    assertThat(PREMIUM.total()).isEqualByComparingTo("17027.86");
    assertThat(PREMIUM.negate().total()).isEqualByComparingTo("-17027.86");
    assertThat(PREMIUM.plus(PREMIUM).minus(PREMIUM)).isEqualTo(PREMIUM);
    assertThat(PREMIUM.withoutDst().dst()).isZero();
    assertThat(PREMIUM.times(new BigDecimal("0.5")).basic()).isEqualByComparingTo("6797.50");
    assertThat(PremiumComponents.ZERO.isZero()).isTrue();
    assertThat(PREMIUM.isZero()).isFalse();
    Map<PremiumComponent, BigDecimal> map = PREMIUM.asMap();
    assertThat(map).hasSize(6);
    assertThat(PremiumComponents.of(map)).isEqualTo(PREMIUM);
  }

  @Test
  void commissionComputesWithholdingAndNet() {
    CommissionTerms terms =
        CommissionTerms.of(
            new BigDecimal("17.5"),
            new BigDecimal("2379.13"),
            new BigDecimal("285.50"),
            BigDecimal.TEN);
    assertThat(terms.wtaxAmount()).isEqualByComparingTo("237.91");
    assertThat(terms.receivable()).isEqualByComparingTo("2664.63");
    assertThat(terms.net()).isEqualByComparingTo("2426.72");
    assertThat(terms.negate().commission()).isEqualByComparingTo("-2379.13");
    assertThat(terms.times(new BigDecimal("0.5")).commission()).isEqualByComparingTo("1189.57");
    assertThat(CommissionTerms.of(null, null, null, null).wtaxAmount()).isZero();
  }

  @Test
  void sharesAddUpExactlyWithTheRemainderOnTheLastInsurer() {
    CommissionTerms terms =
        CommissionTerms.of(
            new BigDecimal("17.5"),
            new BigDecimal("2379.13"),
            new BigDecimal("285.50"),
            BigDecimal.TEN);
    List<ShareAmounts> split =
        BookingEvents.split(
            PREMIUM,
            terms,
            List.of(
                new InsurerShare("A", new BigDecimal("33.3333")),
                new InsurerShare("B", new BigDecimal("33.3333")),
                new InsurerShare("C", new BigDecimal("33.3334"))));
    assertThat(split).hasSize(3);
    assertThat(
            split.stream().map(s -> s.premium().total()).reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(PREMIUM.total());
    assertThat(
            split.stream()
                .map(s -> s.commission().commission())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo("2379.13");
  }

  @Test
  void rulesMatchOnTheirCriteria() {
    AutoBookRule auto = new AutoBookRule(1L, new AutoBookRule.Criteria("PAR08", " ", true, "x"));
    assertThat(auto.matches("PAR08", "CBG")).isTrue();
    assertThat(auto.matches("PAR01", "CBG")).isFalse();
    auto.apply(new AutoBookRule.Criteria(null, "CBG", false, "off"));
    assertThat(auto.matches("PAR08", "CBG")).isFalse();

    IncentiveRule incentive =
        new IncentiveRule(
            1L,
            new IncentiveRule.Criteria(
                "MTR10",
                "CBG",
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                true,
                "x"));
    assertThat(incentive.matches("MTR10", "CBG", "EMAIL", LocalDate.of(2026, 5, 1))).isTrue();
    assertThat(incentive.matches("MTR10", "CBG", "EMAIL", LocalDate.of(2027, 1, 1))).isFalse();
    assertThat(incentive.matches("MTR10", "RETAIL", "EMAIL", LocalDate.of(2026, 5, 1))).isFalse();
    assertThatThrownBy(
            () ->
                incentive.apply(
                    new IncentiveRule.Criteria(
                        null,
                        null,
                        null,
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 1, 1),
                        true,
                        "y")))
        .extracting("code")
        .isEqualTo("INCENTIVE_PERIOD_INVALID");
  }

  @Test
  void kindsAndRowsTellTheirOutcome() {
    assertThat(InvoiceKind.CANCELLATION.isNegative()).isTrue();
    assertThat(InvoiceKind.BOOKING.isNegative()).isFalse();
    assertThat(BatchRow.booked("A", "I").isBooked()).isTrue();
    assertThat(BatchRow.failed("A", "x".repeat(1500)).message()).hasSize(1000);
  }
}
