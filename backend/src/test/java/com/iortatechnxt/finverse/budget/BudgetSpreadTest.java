package com.iortatechnxt.finverse.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.budget.service.BudgetCsvParser;
import com.iortatechnxt.finverse.budget.service.BudgetSpread;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BudgetSpreadTest {

  private static BigDecimal sum(List<BigDecimal> values) {
    return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Test
  void evenSpreadPutsRoundingRemainderInDecember() {
    List<BigDecimal> months = BudgetSpread.even(new BigDecimal("1000.00"));
    assertThat(months).hasSize(12);
    assertThat(months.get(0)).isEqualByComparingTo("83.33");
    assertThat(months.get(11)).isEqualByComparingTo("83.37");
    assertThat(sum(months)).isEqualByComparingTo("1000.00");
  }

  @Test
  void weightedSpreadFollowsSeasonality() {
    List<BigDecimal> weights =
        Stream.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 13).map(BigDecimal::valueOf).toList();
    List<BigDecimal> months = BudgetSpread.weighted(new BigDecimal("2400"), weights);
    assertThat(months.get(0)).isEqualByComparingTo("100.00");
    assertThat(months.get(11)).isEqualByComparingTo("1300.00");
    assertThat(sum(months)).isEqualByComparingTo("2400");
  }

  @Test
  void invalidSeasonalityIsRejected() {
    List<BigDecimal> zeros = Collections.nCopies(12, BigDecimal.ZERO);
    assertThatThrownBy(() -> BudgetSpread.weighted(BigDecimal.TEN, zeros))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> BudgetSpread.weighted(BigDecimal.TEN, List.of(BigDecimal.ONE)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void scaleAppliesPercentage() {
    List<BigDecimal> scaled =
        BudgetSpread.scale(
            List.of(new BigDecimal("100.00"), new BigDecimal("33.33")), BigDecimal.TEN);
    assertThat(scaled).containsExactly(new BigDecimal("110.00"), new BigDecimal("36.66"));
  }

  @Test
  void csvWithMonthlyAndAnnualLayouts() {
    String monthly =
        """
        # budget
        account_code,cost_centre,m01,m02,m03,m04,m05,m06,m07,m08,m09,m10,m11,m12
        5601,FIN,1,2,3,4,5,6,7,8,9,10,11,12

        4100,,100,,,,,,,,,,,
        """;
    List<BudgetCsvParser.ParsedLine> lines = BudgetCsvParser.parse(monthly);
    assertThat(lines).hasSize(2);
    assertThat(lines.get(0).costCenter()).isEqualTo("FIN");
    assertThat(lines.get(0).months().get(11)).isEqualByComparingTo("12");
    assertThat(lines.get(1).costCenter()).isNull();
    assertThat(sum(lines.get(1).months())).isEqualByComparingTo("100");

    List<BudgetCsvParser.ParsedLine> annual =
        BudgetCsvParser.parse("account_code,cost_centre,annual\n5603,FIN,1200\n");
    assertThat(annual.get(0).months()).allMatch(m -> m.compareTo(new BigDecimal("100")) == 0);
  }

  @Test
  void invalidCsvReportsLineErrors() {
    assertThatThrownBy(() -> BudgetCsvParser.parse(""))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("empty");
    assertThatThrownBy(() -> BudgetCsvParser.parse("code,x\n")).hasMessageContaining("Header");
    assertThatThrownBy(
            () ->
                BudgetCsvParser.parse(
                    "account_code,cost_centre,annual\n5601,FIN\n,FIN,1\n5603,,x\n"))
        .hasMessageContaining("Line 2")
        .hasMessageContaining("Line 3")
        .hasMessageContaining("Line 4");
  }
}
