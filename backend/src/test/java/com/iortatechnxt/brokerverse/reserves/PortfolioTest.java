package com.iortatechnxt.brokerverse.reserves;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.reserves.domain.PremiumAmounts;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveKey;
import com.iortatechnxt.brokerverse.reserves.domain.UprItem;
import com.iortatechnxt.brokerverse.reserves.service.Portfolio;
import com.iortatechnxt.brokerverse.underwriting.domain.UprBasis;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** UPR of a portfolio: endorsements, cancellations, RI splits and the earned premium identity. */
class PortfolioTest {

  private static final ReserveKey KEY = new ReserveKey(1L, "FIRE", "FIRE-COM", "BROKER");
  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 12, 31);

  private static PremiumAmounts amounts(String premium, String commission, String treaty) {
    return new PremiumAmounts(
        new BigDecimal(premium),
        new BigDecimal(commission),
        new BigDecimal(treaty),
        BigDecimal.ZERO,
        new BigDecimal(treaty).multiply(new BigDecimal("0.25")));
  }

  private static Portfolio.Entry entry(
      int endorsementNo, LocalDate from, LocalDate approved, PremiumAmounts written) {
    return new Portfolio.Entry(
        7L,
        endorsementNo,
        "P-1/E" + endorsementNo,
        endorsementNo == 0 ? "NEW" : "ENDT",
        KEY,
        UprBasis.DAYS_365,
        from,
        TO,
        approved,
        written);
  }

  @Test
  void originalPolicyUprAndDacFollowTheUnearnedDays() {
    Portfolio book =
        new Portfolio(List.of(entry(0, FROM, FROM, amounts("36500", "5475", "10950"))));
    UprItem item = book.uprAt(LocalDate.of(2026, 3, 31)).get(0);
    // 275 of 365 days unearned
    assertThat(item.unearned().premium()).isEqualByComparingTo("27500.00");
    assertThat(item.unearned().commission()).isEqualByComparingTo("4125.00");
    assertThat(item.unearned().treatyPremium()).isEqualByComparingTo("8250.00");
    assertThat(item.unearned().riCommission()).isEqualByComparingTo("2062.50");
    assertThat(item.earned().premium()).isEqualByComparingTo("9000.00");
  }

  @Test
  void proRataCancellationReleasesTheRemainingUpr() {
    LocalDate cancelled = LocalDate.of(2026, 10, 1);
    // pro-rata return premium for 92 of 365 days: 36 500 x 92 / 365 = 9 200
    Portfolio book =
        new Portfolio(
            List.of(
                entry(0, FROM, FROM, amounts("36500", "0", "0")),
                entry(1, cancelled, cancelled, amounts("-9200", "0", "0"))));
    Map<ReserveKey, PremiumAmounts> before = book.unearnedByKey(LocalDate.of(2026, 9, 30));
    Map<ReserveKey, PremiumAmounts> after = book.unearnedByKey(LocalDate.of(2026, 10, 31));
    assertThat(before.get(KEY).premium()).isEqualByComparingTo("9200.00");
    assertThat(after.get(KEY).premium()).isEqualByComparingTo("0.00");
  }

  @Test
  void transactionsApprovedAfterTheDateAreIgnored() {
    Portfolio book =
        new Portfolio(
            List.of(
                entry(0, FROM, FROM, amounts("36500", "0", "0")),
                entry(
                    1,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 5),
                    amounts("1840", "0", "0"))));
    assertThat(book.uprAt(LocalDate.of(2026, 6, 30))).hasSize(1);
    assertThat(book.uprAt(LocalDate.of(2026, 9, 30))).hasSize(2);
  }

  @Test
  void earnedPremiumIsWrittenPlusOpeningLessClosingUpr() {
    Portfolio book =
        new Portfolio(
            List.of(
                entry(0, FROM, FROM, amounts("36500", "0", "3650")),
                entry(
                    1,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 1),
                    amounts("1840", "0", "0"))));
    Map<ReserveKey, PremiumAmounts> earned =
        book.earnedByKey(LocalDate.of(2026, 6, 30), LocalDate.of(2026, 9, 30));
    // original: 92 days of 365 = 9 200; endorsement: 92 of 184 days = 920
    assertThat(earned.get(KEY).premium()).isEqualByComparingTo("10120.00");
    assertThat(earned.get(KEY).treatyPremium()).isEqualByComparingTo("920.00");
    assertThat(earned.get(KEY).cededPremium()).isEqualByComparingTo("920.00");
  }
}
