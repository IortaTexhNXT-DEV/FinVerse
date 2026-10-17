package com.iortatechnxt.brokerverse.renewal.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardFilter;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardItem;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardMath;
import com.iortatechnxt.brokerverse.renewal.dashboard.service.DashboardMetrics;
import com.iortatechnxt.brokerverse.renewal.service.RenewalWorkingDays;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The figures of the Renewal dashboard (BDOI Renewal FRS FRRN.002.02.01 to .09) on accounts built
 * in memory: budget percentage and variance, ageing buckets, closing ratio, biggest open deals,
 * persistency, insurer approval, the KPI cards and the business-day ageing.
 */
class DashboardFiguresTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);
  private static final DashboardFilter PERIOD =
      new DashboardFilter(
          1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, null);

  private static DashboardItem renewal(String ref, String stage, LocalDate expiry, String premium) {
    Map<String, Object> m = new HashMap<>();
    m.put("kind", "RENEWAL");
    m.put("ref", ref);
    m.put("stage", stage);
    m.put("expiry_date", expiry);
    m.put("premium", new BigDecimal(premium));
    m.put("commission", new BigDecimal(premium).movePointLeft(1));
    m.put("segment", "RETAIL");
    return new DashboardItem(m);
  }

  @Test
  void budgetPercentageAndVarianceFollowTheFormulas() {
    BigDecimal budget = new BigDecimal("10000000.00");
    BigDecimal actual = new BigDecimal("8500000.00");
    assertThat(DashboardMath.percent(actual, budget)).isEqualByComparingTo("85.00");
    assertThat(DashboardMath.variance(actual, budget)).isEqualByComparingTo("-1500000.00");
    assertThat(DashboardMath.percent(actual, BigDecimal.ZERO)).isNull();
    assertThat(DashboardMath.growth(new BigDecimal("9000000.00"), budget))
        .isEqualByComparingTo("90.00");
  }

  @Test
  void closingRatioIsBookedOverTotal() {
    assertThat(DashboardMath.percent(40, 50)).isEqualByComparingTo("80.00");
    List<DashboardItem> items = new ArrayList<>();
    for (int n = 0; n < 50; n++) {
      DashboardItem i =
          renewal("R" + n, n < 40 ? "RENEWED" : "FOR_DISPOSITION", TODAY.plusDays(5), "1000");
      i.put("category", "RENEWAL");
      i.put("tier", n < 40 ? "BOOKED" : "IN_PROCESS");
      items.add(i);
    }
    DashboardMetrics m = new DashboardMetrics(PERIOD, TODAY, 7);
    long booked = items.stream().filter(m.of("CLOSING|RENEWAL|BOOKED")).count();
    long total = items.stream().filter(m.of("CLOSING|RENEWAL|ALL")).count();
    assertThat(DashboardMath.percent(booked, total)).isEqualByComparingTo("80.00");
  }

  @Test
  void outstandingAccountsAgeByExpiryMonthAndLeaveOnceBooked() {
    DashboardMetrics m = new DashboardMetrics(PERIOD, TODAY, 7);
    DashboardItem next = renewal("N", "FOR_DISPOSITION", LocalDate.of(2026, 11, 15), "100");
    DashboardItem prior = renewal("P", "IN_PROCESSING", LocalDate.of(2026, 8, 2), "200");
    DashboardItem booked = renewal("B", "RENEWED", LocalDate.of(2026, 11, 20), "300");
    List<DashboardItem> items = List.of(next, prior, booked);
    assertThat(items.stream().filter(m.of("AGEING|PLUS1"))).containsExactly(next);
    assertThat(items.stream().filter(m.of("AGEING|PRIOR"))).containsExactly(prior);
    assertThat(items.stream().filter(m.of("AGEING|ALL"))).containsExactly(next, prior);
  }

  @Test
  void biggestOpenDealsAreRankedByPremium() {
    DashboardMetrics m = new DashboardMetrics(PERIOD, TODAY, 7);
    List<DashboardItem> items = new ArrayList<>();
    for (int n = 1; n <= 60; n++) {
      items.add(renewal("R" + n, "FOR_DISPOSITION", LocalDate.of(2026, 10, 20), n + "000"));
    }
    items.add(renewal("BOOKED", "RENEWED", LocalDate.of(2026, 10, 20), "999999"));
    List<DashboardItem> top = m.top(items, 50);
    assertThat(top).hasSize(50);
    assertThat(top.get(0).text("ref")).isEqualTo("R60");
    assertThat(top).noneMatch(i -> "BOOKED".equals(i.text("ref")));
  }

  @Test
  void persistencyCountsTheRenewedOfTheRenewableAccounts() {
    DashboardMetrics m = new DashboardMetrics(PERIOD, TODAY, 7);
    List<DashboardItem> items = new ArrayList<>();
    for (int n = 0; n < 100; n++) {
      items.add(renewal("R" + n, n < 90 ? "RENEWED" : "CLOSED", LocalDate.of(2026, 10, 3), "100"));
    }
    items.add(renewal("JAN", "RENEWED", LocalDate.of(2026, 1, 10), "100"));
    long renewable = items.stream().filter(m.of("PERSISTENCY|M0|ALL")).count();
    long renewed = items.stream().filter(m.of("PERSISTENCY|M0|RENEWED")).count();
    assertThat(DashboardMath.percent(renewed, renewable)).isEqualByComparingTo("90.00");
    assertThat(items.stream().filter(m.of("PERSISTENCY|YTD|ALL")).count()).isEqualTo(101);
  }

  @Test
  void insurerApprovalCountsApprovedPendingAndReturned() {
    DashboardMetrics m = new DashboardMetrics(PERIOD, TODAY, 7);
    DashboardItem approved = renewal("A", "RA_READY", LocalDate.of(2026, 10, 10), "100");
    approved.put("disposition", "FOR_RENEWAL");
    approved.put("insurer_response", "RENEW_AS_IS");
    DashboardItem pending = renewal("P", "WITH_INSURER", LocalDate.of(2026, 10, 11), "100");
    pending.put("disposition", "FOR_RENEWAL");
    pending.put("insurer_code", "INS-MGIC");
    DashboardItem returned = renewal("R", "FOR_DISPOSITION", LocalDate.of(2026, 10, 12), "100");
    returned.put("disposition", "FOR_RENEWAL");
    returned.put("insurer_response", "REJECT");
    List<DashboardItem> items = List.of(approved, pending, returned);
    long total = items.stream().filter(m.of("INSURER|ALL|TOTAL")).count();
    long ok = items.stream().filter(m.of("INSURER|ALL|APPROVED")).count();
    assertThat(items.stream().filter(m.of("INSURER|ALL|PENDING"))).containsExactly(pending);
    assertThat(items.stream().filter(m.of("INSURER|ALL|RETURNED"))).containsExactly(returned);
    assertThat(items.stream().filter(m.of("INSURER_PENDING|INS-MGIC"))).containsExactly(pending);
    assertThat(DashboardMath.percent(ok, total)).isEqualByComparingTo("33.33");
  }

  @Test
  void cardsCountDispositionsAndHoldCoversEndingWithinSevenDays() {
    DashboardMetrics m = new DashboardMetrics(PERIOD, TODAY, 7);
    DashboardItem none = renewal("N", "FOR_DISPOSITION", LocalDate.of(2026, 10, 20), "100");
    DashboardItem five = renewal("F", "RA_SENT", LocalDate.of(2026, 9, 30), "100");
    five.put("hold_cover_status", "CONFIRMED");
    five.put("hold_cover_until", TODAY.plusDays(5));
    five.put("disposition", "FOR_RENEWAL");
    DashboardItem ten = renewal("T", "RA_SENT", LocalDate.of(2026, 9, 30), "100");
    ten.put("hold_cover_status", "CONFIRMED");
    ten.put("hold_cover_until", TODAY.plusDays(10));
    List<DashboardItem> items = List.of(none, five, ten);
    assertThat(items.stream().filter(m.of("CARD|FOR_DISPOSITION"))).containsExactly(none);
    assertThat(items.stream().filter(m.of("CARD|EXPIRING_HOLD_COVER"))).containsExactly(five);
    assertThat(items.stream().filter(m.of("CARD|EXPIRING_30"))).containsExactly(none);
  }

  @Test
  void ageingCountsBusinessDaysFromTheNextDay() {
    LocalDate friday = LocalDate.of(2026, 10, 9);
    assertThat(RenewalWorkingDays.between(friday, friday.plusDays(3), RenewalWorkingDays::weekday))
        .isEqualTo(1);
    assertThat(RenewalWorkingDays.between(friday, friday, RenewalWorkingDays::weekday)).isZero();
  }
}
