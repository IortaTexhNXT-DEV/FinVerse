package com.iortatechnxt.brokerverse.finreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.finreport.domain.ScheduleColumn;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Basis;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Comparative;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Grouping;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.LayoutStatus;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.ScheduleFamily;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.SelectorKind;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Side;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleValues;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment.CommentKey;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleMath;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.Increase;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.RowFigures;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleQueries.Window;
import com.iortatechnxt.brokerverse.finreport.service.ScheduleRules;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Pure calculations and rules of the account schedule engine (FRBS 3.2.0). */
class ScheduleMathTest {

  private static final LocalDate AS_OF = LocalDate.of(2026, 3, 31);

  private static BigDecimal d(String v) {
    return new BigDecimal(v);
  }

  private static ScheduleValues values(Basis basis, String slots, Comparative cmp, Measure... m) {
    List<ScheduleColumn> cols =
        java.util.stream.IntStream.range(0, m.length)
            .mapToObj(i -> new ScheduleColumn(i * 10, m[i], m[i].name()))
            .toList();
    return new ScheduleValues(
        " Test ",
        ScheduleFamily.SCHEDULE,
        null,
        " ",
        SelectorKind.ACCOUNT_PREFIX,
        "1210, 1211,",
        Grouping.PARTY,
        "php",
        Side.DEBIT,
        basis,
        slots,
        cmp,
        true,
        false,
        null,
        true,
        cols);
  }

  @Test
  void theWindowShiftsMonthEndsAndMovementPeriods() {
    Window month =
        ScheduleMath.window(
            LocalDate.of(2026, 3, 1), AS_OF, 1, Basis.MOVEMENT, Comparative.PREVIOUS_MONTH);
    assertThat(month.yearStart()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(month.comparativeFrom()).isEqualTo(LocalDate.of(2026, 2, 1));
    assertThat(month.comparativeTo()).isEqualTo(LocalDate.of(2026, 2, 28));
    Window year =
        ScheduleMath.window(
            LocalDate.of(2026, 3, 1), AS_OF, 4, Basis.BALANCE, Comparative.PREVIOUS_YEAR);
    assertThat(year.yearStart()).isEqualTo(LocalDate.of(2025, 4, 1));
    assertThat(year.comparativeTo()).isEqualTo(LocalDate.of(2025, 3, 31));
    assertThat(year.comparativeFrom()).isEqualTo(LocalDate.of(1900, 1, 1));
    Window none =
        ScheduleMath.window(
            LocalDate.of(2026, 3, 10),
            LocalDate.of(2026, 3, 20),
            1,
            Basis.BALANCE,
            Comparative.NONE);
    assertThat(none.comparativeTo()).isEqualTo(none.comparativeFrom());
  }

  @Test
  void measuresFollowTheSideAndTheBasis() {
    RowFigures f =
        new RowFigures(
            "K", d("100.00"), d("50.00"), d("30.00"), d("-40.00"), d("120.00"), d("80.00"));
    Map<Measure, BigDecimal> debit = ScheduleMath.measures(f, Side.DEBIT, Basis.BALANCE);
    assertThat(debit.get(Measure.OPENING)).isEqualByComparingTo("100");
    assertThat(debit.get(Measure.MOVEMENT)).isEqualByComparingTo("20");
    assertThat(debit.get(Measure.CLOSING)).isEqualByComparingTo("120");
    assertThat(debit.get(Measure.VARIANCE)).isEqualByComparingTo("40");
    assertThat(debit.get(Measure.VARIANCE_PCT)).isEqualByComparingTo("50.00");
    Map<Measure, BigDecimal> credit = ScheduleMath.measures(f, Side.CREDIT, Basis.MOVEMENT);
    assertThat(credit.get(Measure.MOVEMENT)).isEqualByComparingTo("-20");
    assertThat(credit.get(Measure.CLOSING)).isEqualByComparingTo("40");
    assertThat(credit.get(Measure.VARIANCE)).isEqualByComparingTo("60");
    RowFigures noCmp = new RowFigures("K", d("0"), d("1"), d("0"), d("1"), d("1"), BigDecimal.ZERO);
    assertThat(ScheduleMath.measures(noCmp, Side.DEBIT, Basis.BALANCE).get(Measure.VARIANCE_PCT))
        .isNull();
  }

  @Test
  void fifoAgesTheBalanceOnItsMostRecentIncreases() {
    AgeingSlots slots = AgeingSlots.of(List.of(30, 90));
    List<Increase> newestFirst =
        List.of(
            new Increase("K", AS_OF.minusDays(10), d("100.00")),
            new Increase("K", AS_OF.minusDays(60), d("100.00")),
            new Increase("K", AS_OF.minusDays(200), d("100.00")));
    assertThat(ScheduleMath.fifo(d("150.00"), newestFirst, AS_OF, slots))
        .containsExactly(d("100.00"), d("50.00"), BigDecimal.ZERO);
    assertThat(ScheduleMath.fifo(d("500.00"), newestFirst, AS_OF, slots))
        .containsExactly(d("100.00"), d("100.00"), d("300.00"));
    assertThat(ScheduleMath.fifo(d("-5.00"), newestFirst, AS_OF, slots))
        .containsExactly(d("-5.00"), BigDecimal.ZERO, BigDecimal.ZERO);
  }

  @Test
  void definitionsAreValidatedAndNormalised() {
    ScheduleValues ok = values(Basis.BALANCE, "30,90", Comparative.NONE, Measure.CLOSING);
    ScheduleRules.validate(ok);
    assertThat(ok.name()).isEqualTo("Test");
    assertThat(ok.description()).isNull();
    assertThat(ok.currency()).isEqualTo("PHP");
    assertThat(ok.layoutStatus()).isEqualTo(LayoutStatus.TO_CONFIRM);
    assertThat(ok.selectorEntries()).containsExactly("1210", "1211");
    ScheduleDefinition def = new ScheduleDefinition("SCH-T", ok);
    assertThat(def.isAgeing()).isTrue();
    assertThat(def.shows(Measure.CLOSING)).isTrue();
    assertThat(def.shows(Measure.OPENING)).isFalse();
    assertThat(def.values().columns()).hasSize(1);
    assertThatThrownBy(
            () ->
                ScheduleRules.validate(
                    values(Basis.MOVEMENT, "30", Comparative.NONE, Measure.MOVEMENT)))
        .hasMessageContaining("balance");
    assertThatThrownBy(
            () ->
                ScheduleRules.validate(
                    values(Basis.BALANCE, null, Comparative.NONE, Measure.VARIANCE)))
        .hasMessageContaining("comparative");
    assertThatThrownBy(
            () ->
                ScheduleRules.validate(
                    values(
                        Basis.BALANCE, null, Comparative.NONE, Measure.CLOSING, Measure.CLOSING)))
        .hasMessageContaining("twice");
    assertThatThrownBy(() -> ScheduleRules.validate(values(Basis.BALANCE, null, Comparative.NONE)))
        .hasMessageContaining("figure");
  }

  @Test
  void commentsNeedACommentaryScheduleAMonthAndARow() {
    ScheduleDefinition def =
        new ScheduleDefinition(
            "SCH-C", values(Basis.BALANCE, null, Comparative.NONE, Measure.CLOSING));
    assertThat(ScheduleRules.comment(def, new CommentKey(1L, "SCH-C", "2026-03", "1210"), " ok "))
        .isEqualTo("ok");
    assertThatThrownBy(
            () -> ScheduleRules.comment(def, new CommentKey(1L, "SCH-C", "2026-13", "1210"), "x"))
        .hasMessageContaining("yyyy-MM");
    assertThatThrownBy(
            () -> ScheduleRules.comment(def, new CommentKey(1L, "SCH-C", "2026-03", " "), "x"))
        .hasMessageContaining("row");
    assertThatThrownBy(
            () ->
                ScheduleRules.comment(
                    def, new CommentKey(1L, "SCH-C", "2026-03", "1210"), "x".repeat(1001)))
        .hasMessageContaining("1000");
  }
}
