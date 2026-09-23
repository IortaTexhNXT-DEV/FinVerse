package com.iortatechnxt.finverse.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.tax.domain.FilingFrequency;
import com.iortatechnxt.finverse.tax.domain.FilingSchedule;
import com.iortatechnxt.finverse.tax.domain.IcLineItem;
import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import com.iortatechnxt.finverse.tax.domain.NormalBalance;
import com.iortatechnxt.finverse.tax.domain.ReturnFigures;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.domain.TaxType;
import com.iortatechnxt.finverse.tax.domain.Taxpayer;
import com.iortatechnxt.finverse.tax.domain.WorksheetKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/** Tax arithmetic, periods, due dates and identities (pure logic). */
class TaxMathTest {

  @Test
  void vatPayableIsOutputLessCreditsAndExcessIsCarriedOver() {
    ReturnFigures payable =
        ReturnFigures.of(
            new BigDecimal("1000000"), new BigDecimal("120000"), new BigDecimal("45000.004"));
    assertThat(payable.amountPayable()).isEqualByComparingTo("75000.00");
    assertThat(payable.excessCredit()).isEqualByComparingTo("0");
    assertThat(payable.taxCredits()).isEqualByComparingTo("45000.00");

    ReturnFigures excess =
        ReturnFigures.of(BigDecimal.ZERO, new BigDecimal("10000"), new BigDecimal("12500.50"));
    assertThat(excess.amountPayable()).isEqualByComparingTo("0");
    assertThat(excess.excessCredit()).isEqualByComparingTo("2500.50");

    ReturnFigures none = ReturnFigures.of(null, null, null);
    assertThat(none.amountPayable()).isEqualByComparingTo("0");
    assertThat(none.excessCredit()).isEqualByComparingTo("0");
  }

  @Test
  void percentagesRoundHalfEvenToCentavos() {
    assertThat(ReturnFigures.percentOf(new BigDecimal("100000"), new BigDecimal("12.5")))
        .isEqualByComparingTo("12500.00");
    assertThat(ReturnFigures.percentOf(new BigDecimal("33.33"), new BigDecimal("0.75")))
        .isEqualByComparingTo("0.25");
    assertThat(ReturnFigures.percentOf(new BigDecimal("0.125"), new BigDecimal("100")))
        .isEqualByComparingTo("0.12");
    assertThat(ReturnFigures.effectiveRate(new BigDecimal("10000"), new BigDecimal("200")))
        .isEqualByComparingTo("2.00");
    assertThat(ReturnFigures.effectiveRate(BigDecimal.ZERO, BigDecimal.TEN))
        .isEqualByComparingTo("0");
  }

  @Test
  void quartersAndMonthsOfTheCalendar() {
    TaxPeriod q2 = TaxPeriod.quarter(2026, 2);
    assertThat(q2.from()).isEqualTo(LocalDate.of(2026, 4, 1));
    assertThat(q2.to()).isEqualTo(LocalDate.of(2026, 6, 30));
    assertThat(q2.isQuarter()).isTrue();
    assertThat(q2.label()).isEqualTo("2026-Q2");
    assertThat(q2.monthIndex(LocalDate.of(2026, 4, 30))).isEqualTo(1);
    assertThat(q2.monthIndex(LocalDate.of(2026, 5, 1))).isEqualTo(2);
    assertThat(q2.monthIndex(LocalDate.of(2026, 6, 30))).isEqualTo(3);
    assertThat(q2.previous()).isEqualTo(TaxPeriod.quarter(2026, 1));
    assertThat(TaxPeriod.quarterOf(LocalDate.of(2026, 12, 31)))
        .isEqualTo(TaxPeriod.quarter(2026, 4));
    assertThat(TaxPeriod.quarter(2026, 1).previous()).isEqualTo(TaxPeriod.quarter(2025, 4));

    TaxPeriod march = TaxPeriod.month(YearMonth.of(2026, 3));
    assertThat(march.isQuarter()).isFalse();
    assertThat(march.label()).isEqualTo("2026-03");
    assertThat(march.previous()).isEqualTo(TaxPeriod.month(YearMonth.of(2026, 2)));
    assertThat(new TaxPeriod(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 3)).label())
        .isEqualTo("2026-01-05..2026-02-03");
  }

  @Test
  void invalidPeriodsAreRejected() {
    LocalDate day = LocalDate.of(2026, 5, 1);
    assertThatThrownBy(() -> new TaxPeriod(day, day.minusDays(1)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> TaxPeriod.quarter(2026, 5)).isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> TaxPeriod.quarter(2026, 0)).isInstanceOf(BusinessRuleException.class);
    TaxPeriod q1 = TaxPeriod.quarter(2026, 1);
    assertThatThrownBy(() -> q1.monthIndex(day)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void dueDatesFollowTheFormRule() {
    FilingSchedule vat = new FilingSchedule(FilingFrequency.QUARTERLY, 1, 25);
    assertThat(vat.dueDate(LocalDate.of(2026, 3, 31))).isEqualTo(LocalDate.of(2026, 4, 25));
    FilingSchedule ewtQuarter = new FilingSchedule(FilingFrequency.QUARTERLY, 1, 31);
    assertThat(ewtQuarter.dueDate(LocalDate.of(2026, 3, 31))).isEqualTo(LocalDate.of(2026, 4, 30));
    assertThat(ewtQuarter.dueDate(LocalDate.of(2026, 12, 31))).isEqualTo(LocalDate.of(2027, 1, 31));
    FilingSchedule dst = new FilingSchedule(FilingFrequency.MONTHLY, 1, 5);
    assertThat(dst.dueDate(LocalDate.of(2026, 1, 31))).isEqualTo(LocalDate.of(2026, 2, 5));
    assertThat(
            new FilingSchedule(FilingFrequency.MONTHLY, 1, 30).dueDate(LocalDate.of(2026, 1, 31)))
        .isEqualTo(LocalDate.of(2026, 2, 28));
  }

  @Test
  void filingPeriodsOfAYear() {
    assertThat(new FilingSchedule(FilingFrequency.MONTHLY, 1, 10).periods(2026)).hasSize(12);
    assertThat(new FilingSchedule(FilingFrequency.QUARTERLY, 1, 25).periods(2026)).hasSize(4);
    FilingSchedule monthly = new FilingSchedule(FilingFrequency.MONTHLY_EXCEPT_QUARTER_END, 1, 10);
    assertThat(monthly.periods(2026))
        .hasSize(8)
        .extracting(TaxPeriod::label)
        .doesNotContain("2026-03", "2026-06", "2026-09", "2026-12")
        .contains("2026-01", "2026-11");
    FilingSchedule annual = new FilingSchedule(FilingFrequency.ANNUAL, 4, 15);
    assertThat(annual.periods(2026))
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.from()).isEqualTo(LocalDate.of(2026, 1, 1));
              assertThat(p.to()).isEqualTo(LocalDate.of(2026, 12, 31));
            });
    assertThat(annual.dueDate(LocalDate.of(2026, 12, 31))).isEqualTo(LocalDate.of(2027, 4, 15));
    assertThat(monthly.periodStarting(LocalDate.of(2026, 2, 1)).label()).isEqualTo("2026-02");
    assertThatThrownBy(() -> monthly.periodStarting(LocalDate.of(2026, 3, 1)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void tinsAreParsedAndFormatted() {
    Taxpayer t = Taxpayer.parse("801-111-222-000", "Metro", "Makati", "1200");
    assertThat(t.tin()).isEqualTo("801111222");
    assertThat(t.branchCode()).isEqualTo("000");
    assertThat(t.formattedTin()).isEqualTo("801-111-222-000");
    assertThat(Taxpayer.parse("801111222", "X", null, null).branchCode()).isEqualTo("000");
    assertThat(Taxpayer.parse("801-111-222-00012", "X", null, null).branchCode())
        .isEqualTo("00012");
    assertThat(Taxpayer.parse(null, "Unknown", null, null).tin()).isEqualTo("000000000");
  }

  @Test
  void icLinesMatchRangesAndReportGroups() {
    IcLineItem line = new IcLineItem(1L, IcSchedule.INVESTMENTS, "FA");
    line.setAccountFrom("1500");
    line.setAccountTo("1599");
    assertThat(line.matches("1501", null)).isTrue();
    assertThat(line.matches("1600", null)).isFalse();
    assertThat(line.mappingText()).isEqualTo("1500-1599");
    line.setReportGroup("Financial Assets");
    assertThat(line.matches("1120", "Financial Assets")).isTrue();
    assertThat(line.mappingText()).isEqualTo("1500-1599, group Financial Assets");
    IcLineItem single = new IcLineItem(1L, IcSchedule.RESERVES, "UPR");
    single.setAccountFrom("2101");
    single.setAccountTo("2101");
    assertThat(single.mappingText()).isEqualTo("2101");
    assertThat(NormalBalance.CREDIT.present(new BigDecimal("-50"))).isEqualByComparingTo("50");
    assertThat(NormalBalance.DEBIT.present(new BigDecimal("-50"))).isEqualByComparingTo("-50");
  }

  @Test
  void worksheetKindsKnowTheirLevies() {
    assertThat(WorksheetKind.ofLevy(TaxType.DST)).isEqualTo(WorksheetKind.DST);
    assertThat(WorksheetKind.ofLevy(TaxType.FST).premiumLevy()).isEqualTo(TaxType.FST);
    assertThat(WorksheetKind.VAT.premiumLevy()).isNull();
    assertThatThrownBy(() -> WorksheetKind.ofLevy(TaxType.EWT))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(TaxType.LGT.isPremiumLevy()).isTrue();
    assertThat(TaxType.VAT_OUTPUT.isPremiumLevy()).isFalse();
  }
}
