package com.iortatechnxt.brokerverse.renewal.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.Input;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.PolicyType;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.Result;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.Tables;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.Tier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * CBG Motor automatic renewal values (BDOI Renewal FRS FRRN.009.01, Annexes D and E): depreciation,
 * BI and PD tiers, the documentary stamp tax rounded up to PHP 0.50, VAT, LGT, CTPL and Auto Promo
 * to Regular.
 */
class CbgMotorRenewalTest {

  private static final LocalDate EXPIRY = LocalDate.of(2027, 3, 15);

  private static final Tables TABLES =
      new Tables(
          List.of(
              new Tier(new BigDecimal("100000"), new BigDecimal("240.00")),
              new Tier(new BigDecimal("300000"), new BigDecimal("495.00")),
              new Tier(new BigDecimal("500000"), new BigDecimal("650.00"))),
          List.of(
              new Tier(new BigDecimal("100000"), new BigDecimal("300.00")),
              new Tier(new BigDecimal("300000"), new BigDecimal("560.00"))),
          true,
          Map.of("MTR70", "MTR22"),
          Map.of("2.4230", new BigDecimal("2.3760")));

  private static BigDecimal bd(String v) {
    return new BigDecimal(v);
  }

  private static Input regular(String od, String bi, String pd) {
    return new Input(
        PolicyType.REGULAR,
        1,
        "MTR22",
        bd("2.3760"),
        EXPIRY,
        bd(od),
        bd(bi),
        bd("650.00"),
        bd(pd),
        bd("560.00"),
        bd("250.00"),
        bd("24000.00"),
        bd("3000.00"),
        bd("2880.00"),
        bd("48.00"),
        BigDecimal.ZERO,
        bd("29928.00"));
  }

  @Test
  void aRegularRenewalDepreciatesTheSumInsuredByTenPercent() {
    Result r = CbgMotorRenewal.compute(regular("1000000", "500000", "300000"), TABLES);
    assertThat(r.odTheft()).isEqualByComparingTo("900000.00");
    assertThat(r.odTheftPremium()).isEqualByComparingTo("21384.00");
    assertThat(r.inception()).isEqualTo(EXPIRY);
    assertThat(r.expiry()).isEqualTo(EXPIRY.plusYears(1));
    assertThat(r.bi()).isEqualByComparingTo("500000");
    assertThat(r.biPremium()).isEqualByComparingTo("650.00");
    BigDecimal basic = bd("21384.00").add(bd("650.00")).add(bd("560.00")).add(bd("250.00"));
    assertThat(r.basic()).isEqualByComparingTo(basic);
    assertThat(r.vat())
        .isEqualByComparingTo(basic.multiply(bd("0.12")).setScale(2, RoundingMode.HALF_UP));
    assertThat(r.lgt())
        .isEqualByComparingTo(basic.multiply(bd("0.002")).setScale(2, RoundingMode.HALF_UP));
    assertThat(r.total()).isEqualByComparingTo(r.basic().add(r.dst()).add(r.vat()).add(r.lgt()));
  }

  @Test
  void limitsAboveTheNewSumInsuredStepDownToTheNextTier() {
    Result r = CbgMotorRenewal.compute(regular("400000", "500000", "300000"), TABLES);
    assertThat(r.odTheft()).isEqualByComparingTo("360000.00");
    assertThat(r.bi()).isEqualByComparingTo("300000");
    assertThat(r.biPremium()).isEqualByComparingTo("495.00");
    assertThat(r.pd()).isEqualByComparingTo("300000");
    assertThat(r.pdPremium()).isEqualByComparingTo("560.00");
  }

  @Test
  void theDocumentaryStampTaxIsRoundedUpToFiftyCentavos() {
    assertThat(CbgMotorRenewal.dst(bd("12296.96"))).isEqualByComparingTo("1537.50");
    assertThat(CbgMotorRenewal.dst(bd("12300.80"))).isEqualByComparingTo("1538.00");
    assertThat(CbgMotorRenewal.dst(bd("12300.00"))).isEqualByComparingTo("1537.50");
  }

  @Test
  void multiYearAndCtplAndAutoPromoFollowAnnexDAndE() {
    Input threeYears =
        new Input(
            PolicyType.REGULAR,
            3,
            "MTR22",
            bd("2.3760"),
            EXPIRY,
            bd("2710000"),
            null,
            null,
            null,
            null,
            null,
            bd("1"),
            null,
            null,
            null,
            null,
            null);
    assertThat(CbgMotorRenewal.compute(threeYears, TABLES).odTheft())
        .isEqualByComparingTo("729000.00");
    Input ctpl =
        new Input(
            PolicyType.CTPL,
            1,
            "CTP01",
            null,
            EXPIRY,
            bd("0"),
            null,
            null,
            null,
            null,
            null,
            bd("560.00"),
            bd("70.00"),
            bd("67.20"),
            bd("1.12"),
            bd("50.00"),
            bd("748.32"));
    Result c = CbgMotorRenewal.compute(ctpl, TABLES);
    assertThat(c.total()).isEqualByComparingTo("748.32");
    assertThat(c.expiry()).isEqualTo(EXPIRY.plusYears(1));
    Input promo =
        new Input(
            PolicyType.AUTO_PROMO,
            1,
            "MTR70",
            bd("2.4230"),
            EXPIRY,
            bd("1000000"),
            bd("500000"),
            bd("650.00"),
            bd("300000"),
            bd("560.00"),
            bd("250.00"),
            bd("25000"),
            null,
            null,
            null,
            null,
            null);
    Result p = CbgMotorRenewal.compute(promo, TABLES);
    assertThat(p.riskCode()).isEqualTo("MTR22");
    assertThat(p.premiumRate()).isEqualByComparingTo("2.3760");
    assertThat(p.bi()).isEqualByComparingTo("500000");
  }
}
