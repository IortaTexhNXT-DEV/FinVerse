package com.iortatechnxt.brokerverse.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate.RateValidity;
import com.iortatechnxt.brokerverse.catalog.domain.MotorCoverage;
import com.iortatechnxt.brokerverse.catalog.domain.MotorLimit.LimitPrice;
import com.iortatechnxt.brokerverse.catalog.domain.PaymentGate;
import com.iortatechnxt.brokerverse.catalog.domain.RateCode;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.TsuInvolvement;
import com.iortatechnxt.brokerverse.catalog.service.CatalogApprovalSource;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
import com.iortatechnxt.brokerverse.catalog.service.RateResolver;
import com.iortatechnxt.brokerverse.catalog.service.RateTableService;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService.Rating;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CatalogRatingIT {

  @Autowired private ProductCatalogService catalog;
  @Autowired private ProductRuleService rules;
  @Autowired private CatalogRecords records;
  @Autowired private CatalogApprovalSource approvals;
  @Autowired private InsurerService insurers;
  @Autowired private PartyService parties;
  @Autowired private RateTableService rates;
  @Autowired private RateResolver resolver;
  @Autowired private RatingService rating;
  @Autowired private TsuRoutingService tsu;
  @Autowired private SalesOrganisationService sales;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private static String unique(String prefix) {
    return prefix + Long.toString(System.nanoTime() % 1_000_000_000L, 36).toUpperCase();
  }

  private static ProductDetails details(String line, String cover, boolean packaged) {
    return new ProductDetails(
        "Test product",
        line,
        cover,
        packaged,
        false,
        List.of("CBG"),
        false,
        true,
        true,
        3,
        false,
        PaymentGate.CLIENT_CONFIRMATION,
        new BigDecimal("0.5"),
        new BigDecimal("15"),
        new BigDecimal("1000"),
        null,
        TsuInvolvement.BY_RULES);
  }

  @Test
  void ratesResolveByDateLineAndProduct() {
    Long company = data.company().getId();
    LocalDate day = LocalDate.of(2031, 1, 1);
    var row =
        as.run(
            "badmin",
            () ->
                rates.createRate(
                    RateCode.FIRE_SERVICE_TAX,
                    "ENGINEERING",
                    new RateValidity(new BigDecimal("1.5"), day, day.plusDays(10))));
    assertThat(resolver.rate(RateCode.FIRE_SERVICE_TAX, "ENGINEERING", day)).isEmpty();
    as.run("approver", () -> records.authorize(CatalogKind.RATE, row.getId()));
    assertThat(resolver.rate(RateCode.FIRE_SERVICE_TAX, "ENGINEERING", day))
        .contains(new BigDecimal("1.50000000"));
    assertThat(resolver.rate(RateCode.FIRE_SERVICE_TAX, "ENGINEERING", day.plusDays(11))).isEmpty();
    assertThat(resolver.rate(RateCode.VAT_PREMIUM, "PROPERTY", day))
        .hasValueSatisfying(v -> assertThat(v).isZero());
    assertThat(resolver.rate(RateCode.VAT_PREMIUM, "MOTOR", day))
        .hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("12"));
    assertThat(resolver.commission(company, "INS-MGIC", "MTR10", day))
        .hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("17.5"));
    assertThat(resolver.commission(company, "INS-MGIC", "MTR15", day))
        .hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("20"));
    assertThat(resolver.shortPeriodPercent(3, day))
        .hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("40"));
    assertThat(resolver.motorLimitPremium(MotorCoverage.PD, new BigDecimal("100000"), day))
        .hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("440"));
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        rates.createRate(
                            RateCode.DST,
                            null,
                            new RateValidity(new BigDecimal("101"), day, null))))
        .extracting("code")
        .isEqualTo("RATE_INVALID");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        rates.createRate(
                            RateCode.DST,
                            null,
                            new RateValidity(BigDecimal.ONE, day, day.minusDays(1)))))
        .extracting("code")
        .isEqualTo("EFFECTIVITY_INVALID");

    var limit =
        as.run(
            "badmin",
            () ->
                rates.createMotorLimit(
                    MotorCoverage.BI,
                    new BigDecimal("2500000"),
                    new LimitPrice(new BigDecimal("2800"), day, null)));
    as.run(
        "badmin",
        () ->
            rates.updateMotorLimit(
                limit.getId(), new LimitPrice(new BigDecimal("2900"), day, null)));
    var shortRow =
        as.run(
            "badmin",
            () -> rates.createShortPeriod(12, new RateValidity(new BigDecimal("100"), day, null)));
    as.run(
        "badmin",
        () ->
            rates.updateShortPeriod(
                shortRow.getId(), new RateValidity(new BigDecimal("99"), day, null)));
    var commission =
        as.run(
            "badmin",
            () ->
                rates.createCommission(
                    company,
                    "INS-LAC",
                    "PAR01",
                    new RateValidity(new BigDecimal("21"), day, null)));
    as.run(
        "badmin",
        () ->
            rates.updateCommission(
                commission.getId(), new RateValidity(new BigDecimal("22"), day, null)));
    as.run(
        "badmin",
        () -> rates.updateRate(row.getId(), new RateValidity(new BigDecimal("1.6"), day, null)));
    assertThat(rates.commissions(company, "INS-LAC")).extracting("productCode").contains("PAR01");
    assertThat(rates.rates()).isNotEmpty();
    assertThat(rates.shortPeriods()).hasSizeGreaterThanOrEqualTo(12);
    assertThat(rates.motorLimits()).isNotEmpty();
  }

  @Test
  void ratingUsesTheBranchLgtAndInsurerCommission() {
    Long company = data.company().getId();
    Rating fire =
        rating.rate(
            new RatingQuery(
                company,
                "PAR01",
                "INS-MGIC",
                "MKT",
                List.of(
                    new RatingQuery.Item("Building", new BigDecimal("1000000"), null, null, null)),
                false,
                PeriodBasis.ANNUAL,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2027, 10, 1),
                null,
                false,
                null));
    assertThat(fire.breakdown().netPremium()).isEqualByComparingTo("2500.00");
    assertThat(fire.breakdown().dst()).isEqualByComparingTo("312.50");
    assertThat(fire.breakdown().premiumTax()).isEqualByComparingTo("300.00");
    assertThat(fire.breakdown().fst()).isEqualByComparingTo("50.00");
    assertThat(fire.breakdown().lgt()).isEqualByComparingTo("18.75");
    assertThat(fire.breakdown().grossPremium()).isEqualByComparingTo("3181.25");
    assertThat(fire.breakdown().commission()).isEqualByComparingTo("625.00");

    Rating motor =
        rating.rate(
            new RatingQuery(
                company,
                "MTR10",
                "INS-MGIC",
                "MKT",
                List.of(
                    new RatingQuery.Item(
                        "ABC",
                        new BigDecimal("1000000"),
                        null,
                        new BigDecimal("100000"),
                        new BigDecimal("100000"))),
                false,
                null,
                null,
                null,
                null,
                false,
                LocalDate.of(2026, 10, 1)));
    assertThat(motor.breakdown().netPremium()).isEqualByComparingTo("12425.00");
    assertThat(motor.breakdown().grossPremium()).isEqualByComparingTo("15562.69");
    assertThat(motor.breakdown().commission()).isEqualByComparingTo("2174.38");

    Rating shortTerm =
        rating.rate(
            new RatingQuery(
                company,
                "PAR01",
                null,
                null,
                List.of(
                    new RatingQuery.Item("Building", new BigDecimal("4000000"), null, null, null)),
                false,
                PeriodBasis.SHORT_PERIOD,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 15),
                new BigDecimal("10"),
                false,
                null));
    assertThat(shortTerm.shortPeriodPercent()).isEqualByComparingTo("40");
    assertThat(shortTerm.breakdown().netPremium()).isEqualByComparingTo("4000.00");
    assertThat(shortTerm.breakdown().commission()).isEqualByComparingTo("400.00");

    assertThatThrownBy(
            () ->
                rating.rate(
                    new RatingQuery(
                        company,
                        "MTR10",
                        null,
                        null,
                        List.of(
                            new RatingQuery.Item(
                                "X", BigDecimal.TEN, null, new BigDecimal("123"), null)),
                        false,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null)))
        .extracting("code")
        .isEqualTo("MOTOR_LIMIT_UNKNOWN");
    assertThatThrownBy(
            () ->
                rating.rate(
                    new RatingQuery(
                        company,
                        "CTP01",
                        null,
                        null,
                        List.of(new RatingQuery.Item(null, BigDecimal.TEN, null, null, null)),
                        false,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null)))
        .extracting("code")
        .isEqualTo("RATING_RATE_REQUIRED");
  }
}
