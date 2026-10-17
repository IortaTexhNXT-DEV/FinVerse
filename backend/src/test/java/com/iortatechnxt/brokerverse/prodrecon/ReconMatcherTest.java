package com.iortatechnxt.brokerverse.prodrecon;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSide;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconMatcher;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconMatcher.Field;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconMatcher.MatchKey;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pairing keys and field comparison of production reconciliation (PRCID.022/026/027). */
class ReconMatcherTest {

  private static final BigDecimal TOLERANCE = new BigDecimal("1.00");
  private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
  private static final LocalDate TO = LocalDate.of(2027, 10, 1);

  private static ReconSide booked() {
    return new ReconSide(
        "MGIC-MC-2026-001",
        "BI-2026-000123",
        "PN-1, PN-2",
        FROM,
        TO,
        "Juan dela Cruz",
        new BigDecimal("2379.13"),
        new BigDecimal("13595.00"),
        new BigDecimal("17027.86"));
  }

  private static ReconSide insurer(String ref, String policy, BigDecimal gross) {
    return new ReconSide(
        policy, ref, null, FROM, TO, "JUAN DELA CRUZ.", null, new BigDecimal("13595.50"), gross);
  }

  @Test
  void pairsOnTheFirstKeyBothSidesCarry() {
    ReconSide theirs = insurer("bi 2026-000123", null, null);
    assertThat(MatchKey.INVOICE_NO.pairs(booked(), theirs)).isTrue();
    assertThat(MatchKey.POLICY_NO.pairs(booked(), theirs)).isFalse();
    assertThat(
            ReconMatcher.pairs(List.of(MatchKey.POLICY_NO, MatchKey.INVOICE_NO), booked(), theirs))
        .isTrue();
    ReconSide byPn = new ReconSide(null, null, "pn-2", null, null, null, null, null, null);
    assertThat(MatchKey.PN_NO.pairs(booked(), byPn)).isTrue();
    assertThat(ReconMatcher.pairs(List.of(MatchKey.INVOICE_NO), booked(), byPn)).isFalse();
  }

  @Test
  void differencesWithinTheToleranceAreMatched() {
    ReconSide theirs = insurer("BI-2026-000123", "MGIC-MC-2026-001", new BigDecimal("17028.86"));
    assertThat(ReconMatcher.differences(booked(), theirs, TOLERANCE)).isEmpty();
  }

  @Test
  void fieldsBeyondTheToleranceAreListed() {
    ReconSide theirs =
        new ReconSide(
            "OTHER-POLICY",
            "BI-2026-000123",
            "PN-9",
            FROM.plusDays(1),
            TO,
            "Maria Santos",
            new BigDecimal("2000.00"),
            new BigDecimal("13595.00"),
            new BigDecimal("17029.00"));
    assertThat(ReconMatcher.differences(booked(), theirs, TOLERANCE))
        .containsExactly(
            Field.POLICY_NO,
            Field.PN_NO,
            Field.PERIOD_FROM,
            Field.ASSURED_NAME,
            Field.COMMISSION,
            Field.GROSS_PREMIUM);
  }

  @Test
  void blankInsurerFieldsAreNotCompared() {
    ReconSide theirs =
        new ReconSide(null, "BI-2026-000123", null, null, null, null, null, null, null);
    assertThat(ReconMatcher.differences(booked(), theirs, TOLERANCE)).isEmpty();
    ReconSide noBooked = new ReconSide(null, "X", null, null, null, null, null, null, null);
    ReconSide amount =
        new ReconSide(null, "X", null, null, null, null, null, null, new BigDecimal("0.50"));
    assertThat(ReconMatcher.differences(noBooked, amount, TOLERANCE)).isEmpty();
  }
}
