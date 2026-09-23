package com.iortatechnxt.finverse.finreport;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.coa.domain.AccountLevel;
import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.AccountNode;
import com.iortatechnxt.finverse.finreport.service.FormatLine;
import com.iortatechnxt.finverse.finreport.service.FormatLine.Type;
import com.iortatechnxt.finverse.finreport.service.StatementFormat;
import com.iortatechnxt.finverse.finreport.service.StatementFormat.Evaluation;
import com.iortatechnxt.finverse.finreport.service.StatementFormat.Rounding;
import com.iortatechnxt.finverse.finreport.service.StatementFormatService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatementFormatTest {

  private static AccountNode account(long id, String code, AccountClass cls, String group) {
    return new AccountNode(id, code, code, AccountLevel.SUB, null, cls, true, false, false, group);
  }

  private static final AccountHierarchy CHART =
      new AccountHierarchy(
          List.of(
              account(1, "1101", AccountClass.ASSET, "Cash"),
              account(2, "1201", AccountClass.ASSET, "Receivables"),
              account(3, "2101", AccountClass.LIABILITY, "Reserves"),
              account(4, "3101", AccountClass.EQUITY, "Capital"),
              account(5, "4101", AccountClass.INCOME, "Premiums"),
              account(6, "5101", AccountClass.EXPENSE, null),
              account(7, "9101", AccountClass.MEMORANDUM, null)));

  /** Cash 1500, receivables 1000, reserves -800, capital -1000, income -1200, expense 500. */
  private static final Map<Long, BigDecimal> BALANCES =
      Map.of(
          1L, new BigDecimal("1500.40"),
          2L, new BigDecimal("1000.30"),
          3L, new BigDecimal("-800.00"),
          4L, new BigDecimal("-1000.70"),
          5L, new BigDecimal("-1200.00"),
          6L, new BigDecimal("500.00"));

  private static BigDecimal value(StatementFormat f, Evaluation e, String caption) {
    return f.lines().stream()
        .filter(l -> l.caption().equals(caption))
        .map(l -> e.value(l.lineNo()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void standardBalanceSheetBalancesWithUnclosedResult() {
    StatementFormat bs =
        StatementFormatService.standard(StatementFormatService.BALANCE_SHEET, CHART);
    Evaluation e = bs.evaluate(CHART, BALANCES, Rounding.NONE);
    assertThat(value(bs, e, "TOTAL ASSETS")).isEqualByComparingTo("2500.70");
    assertThat(value(bs, e, "NET ASSETS")).isEqualByComparingTo("1700.70");
    assertThat(value(bs, e, "SHAREHOLDERS' FUNDS")).isEqualByComparingTo("1700.70");
    assertThat(value(bs, e, "Surplus / (Deficit) Not Yet Appropriated"))
        .isEqualByComparingTo("700");
    assertThat(bs.lines())
        .filteredOn(l -> l.type() == Type.ACCOUNTS)
        .extracting(FormatLine::scheduleRef)
        .containsExactly("1", "2", "3", "4");
    assertThat(e.unmappedAccounts()).isEmpty();
  }

  @Test
  void standardIncomeStatementComputesSurplusWithOtherGroup() {
    StatementFormat ie =
        StatementFormatService.standard(StatementFormatService.INCOME_EXPENSE, CHART);
    Evaluation e = ie.evaluate(CHART, BALANCES, Rounding.NONE);
    assertThat(value(ie, e, "TOTAL INCOME")).isEqualByComparingTo("1200");
    assertThat(value(ie, e, "Other expense")).isEqualByComparingTo("500");
    assertThat(value(ie, e, "SURPLUS / (DEFICIT)")).isEqualByComparingTo("700");
  }

  @Test
  void codeRangeFormatRoundsLinesAndRecomputesTotalsFromRoundedLines() {
    StatementFormat f =
        new StatementFormat(
            "T",
            "Test",
            "BS",
            List.of(
                new FormatLine(10, "Cash", Type.ACCOUNTS, "1", "1100", "1199", null, null, 1, null),
                new FormatLine(
                    20, "Debtors", Type.ACCOUNTS, "2", "1200", "1299", null, null, 1, null),
                new FormatLine(
                    30, "Assets", Type.TOTAL, null, null, null, null, null, 1, List.of(10, 20)),
                new FormatLine(
                    40, "Minus", Type.TOTAL, null, null, null, null, null, -1, List.of(30, -10))));
    Evaluation e = f.evaluate(CHART, BALANCES, Rounding.THOUSANDS);
    assertThat(e.value(10)).isEqualByComparingTo("2");
    assertThat(e.value(20)).isEqualByComparingTo("1");
    assertThat(e.value(30)).isEqualByComparingTo("3");
    assertThat(e.value(40)).isEqualByComparingTo("-1");
    assertThat(e.unmappedAccounts()).containsExactly("2101", "3101");
  }

  @Test
  void roundingOptions() {
    assertThat(Rounding.NONE.apply(new BigDecimal("1234.56"))).isEqualByComparingTo("1234.56");
    assertThat(Rounding.LAKHS.apply(new BigDecimal("250000"))).isEqualByComparingTo("3");
    assertThat(Rounding.MILLIONS.apply(new BigDecimal("-2600000"))).isEqualByComparingTo("-3");
  }

  @Test
  void headingLinesCoverNoAccount() {
    FormatLine heading =
        new FormatLine(1, "H", Type.HEADING, null, null, null, null, null, 1, null);
    assertThat(heading.covers(CHART.node(1L))).isFalse();
    assertThat(heading.terms()).isEmpty();
  }
}
