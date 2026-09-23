package com.iortatechnxt.finverse.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.tax.domain.AtcQuarterAmounts;
import com.iortatechnxt.finverse.tax.domain.Certificate2307Aggregator;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.domain.WithholdingEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** BIR Form 2307 aggregation per payee, ATC and month of the quarter. */
class Certificate2307AggregatorTest {

  private static final TaxPeriod Q3 = TaxPeriod.quarter(2026, 3);

  @Test
  void incomeIsSplitByMonthOfTheQuarterAndTaxIsTotalled() {
    List<WithholdingEntry> entries =
        List.of(
            entry("S-0002", "WC160", "2026-07-03", "10000", "200"),
            entry("S-0002", "WC160", "2026-07-28", "5000", "100"),
            entry("S-0002", "WC160", "2026-09-30", "2500", "50"),
            entry("S-0003", "WC100", "2026-08-15", "40000", "2000"),
            entry("B-0001", "WC515", "2026-08-01", "15000", "1500"),
            entry("B-0001", "WC515", "2026-09-10", "-3000", "-300"),
            entry("S-0002", "WC120", "2026-08-31", "1000", "20"));

    Map<String, List<AtcQuarterAmounts>> result = Certificate2307Aggregator.aggregate(Q3, entries);

    assertThat(result.keySet()).containsExactly("B-0001", "S-0002", "S-0003");
    List<AtcQuarterAmounts> supplier = result.get("S-0002");
    assertThat(supplier).extracting(AtcQuarterAmounts::atc).containsExactly("WC120", "WC160");
    AtcQuarterAmounts services = supplier.get(1);
    assertThat(services.month1()).isEqualByComparingTo("15000");
    assertThat(services.month2()).isEqualByComparingTo("0");
    assertThat(services.month3()).isEqualByComparingTo("2500");
    assertThat(services.total()).isEqualByComparingTo("17500");
    assertThat(services.tax()).isEqualByComparingTo("350");
    assertThat(services.incomeNature()).isEqualTo("nature of WC160");
    assertThat(supplier.get(0).month2()).isEqualByComparingTo("1000");

    AtcQuarterAmounts commission = result.get("B-0001").get(0);
    assertThat(commission.month2()).isEqualByComparingTo("15000");
    assertThat(commission.month3()).isEqualByComparingTo("-3000");
    assertThat(commission.total()).isEqualByComparingTo("12000");
    assertThat(commission.tax()).isEqualByComparingTo("1200");
    assertThat(result.get("S-0003").get(0).month2()).isEqualByComparingTo("40000");
  }

  @Test
  void onlyQuartersAndDatesInsideThemAreAccepted() {
    TaxPeriod month = TaxPeriod.month(YearMonth.of(2026, 7));
    List<WithholdingEntry> july = List.of(entry("S-0001", "WC160", "2026-07-01", "1", "0"));
    assertThatThrownBy(() -> Certificate2307Aggregator.aggregate(month, july))
        .isInstanceOf(BusinessRuleException.class);
    List<WithholdingEntry> outside = List.of(entry("S-0001", "WC160", "2026-10-01", "1", "0"));
    assertThatThrownBy(() -> Certificate2307Aggregator.aggregate(Q3, outside))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(Certificate2307Aggregator.aggregate(Q3, List.of())).isEmpty();
  }

  private static WithholdingEntry entry(
      String party, String atc, String date, String income, String tax) {
    return new WithholdingEntry(
        party,
        atc,
        "nature of " + atc,
        LocalDate.parse(date),
        new BigDecimal(income),
        new BigDecimal(tax));
  }
}
