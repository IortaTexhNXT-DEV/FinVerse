package com.iortatechnxt.brokerverse.remittance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRule;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.IncentiveBasis;
import com.iortatechnxt.brokerverse.remittance.service.CsvRows;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Decision;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Facts;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Line;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Part;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Position;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure remittance rules: eligibility, amounts, incentives and file reading (RMTID.014/017/023). */
class RemittanceRulesTest {

  private static BigDecimal d(String v) {
    return new BigDecimal(v);
  }

  /** Premium 1,000 (basic 800), commission 150, VAT 18, WTAX 15; paid and remitted as given. */
  private static Position position(String paid, String dtipRemitted, String commissionRemitted) {
    BigDecimal remitted = d(dtipRemitted);
    return new Position(
        d("1000.00"),
        d(paid),
        d("800.00"),
        new Part(d("1000.00"), remitted, d("1000.00").subtract(remitted)),
        new Part(d("150.00"), d(commissionRemitted), d("150.00").subtract(d(commissionRemitted))),
        new Part(d("18.00"), BigDecimal.ZERO, d("18.00")),
        new Part(d("15.00"), BigDecimal.ZERO, d("15.00")));
  }

  private static Facts facts(
      Position p, boolean hold, boolean checkHolding, String lockedBy, boolean cap) {
    return new Facts(p, hold, false, false, checkHolding, lockedBy, cap);
  }

  @Test
  void nothingPaidIsNotYetDue() {
    Decision d = RemittanceRules.decide(facts(position("0", "0", "0"), false, false, null, false));
    assertThat(d.tag()).isEqualTo(ExtractionTag.UNEXTRACTED_NOT_DUE);
    assertThat(d.remittable()).isZero();
  }

  @Test
  void paidInvoicesAreExtractedUnlessSomethingBlocksThem() {
    Position paid = position("1000.00", "0", "0");
    assertThat(RemittanceRules.decide(facts(paid, false, false, null, false)).tag())
        .isEqualTo(ExtractionTag.EXTRACTED);
    Decision blocked = RemittanceRules.decide(facts(paid, true, true, "ADJUSTMENT", false));
    assertThat(blocked.tag()).isEqualTo(ExtractionTag.UNEXTRACTED_DUE);
    assertThat(blocked.reasons()).containsExactly("ON_HOLD", "CHECK_HOLDING", "OTHERS");
    assertThat(blocked.remarks()).contains("ADJUSTMENT");
    Decision others =
        RemittanceRules.decide(new Facts(paid, false, true, true, false, null, false));
    assertThat(others.reasons()).containsExactly("PENDING_NEG_ADJ", "WRITTEN_OFF");
  }

  @Test
  void paidArAboveTheDtipIsExcludedOrCappedAsParameterised() {
    Position over = position("1000.00", "100.00", "0");
    Decision excluded = RemittanceRules.decide(facts(over, false, false, null, false));
    assertThat(excluded.tag()).isEqualTo(ExtractionTag.EXTRACTED);
    Position above =
        new Position(
            d("1000.00"),
            d("1000.00"),
            d("800.00"),
            new Part(d("900.00"), BigDecimal.ZERO, d("900.00")),
            new Part(d("150.00"), BigDecimal.ZERO, d("150.00")),
            new Part(d("18.00"), BigDecimal.ZERO, d("18.00")),
            new Part(d("15.00"), BigDecimal.ZERO, d("15.00")));
    Decision exclude = RemittanceRules.decide(facts(above, false, false, null, false));
    assertThat(exclude.tag()).isEqualTo(ExtractionTag.UNEXTRACTED_DUE);
    assertThat(exclude.reasons()).containsExactly("PAID_AR_OVER_DTIP");
    assertThat(exclude.overDtip()).isTrue();
    Decision cap = RemittanceRules.decide(facts(above, false, false, null, true));
    assertThat(cap.tag()).isEqualTo(ExtractionTag.EXTRACTED);
    assertThat(cap.remittable()).isEqualByComparingTo("900.00");
    assertThat(cap.remarks()).startsWith("Paid AR capped");
  }

  @Test
  void commissionIsRealizedInProportionAndTheLastRemittanceTakesTheRest() {
    Line half = RemittanceRules.amounts(position("333.33", "0", "0"), d("333.33"), null);
    RemittanceAmounts a = half.amounts();
    assertThat(a.paidAr()).isEqualByComparingTo("333.33");
    assertThat(a.commission()).isEqualByComparingTo("50.00");
    assertThat(a.commissionVat()).isEqualByComparingTo("6.00");
    assertThat(a.wtax()).isEqualByComparingTo("5.00");
    assertThat(a.netDue()).isEqualByComparingTo("282.33");
    assertThat(half.basicPremium()).isEqualByComparingTo("266.66");
    assertThat(a.incentive()).isZero();

    Line rest =
        RemittanceRules.amounts(position("1000.00", "333.33", "50.00"), d("666.67"), d("2"));
    assertThat(rest.amounts().commission()).isEqualByComparingTo("100.00");
    assertThat(rest.amounts().incentive()).isEqualByComparingTo("10.67");
    assertThat(rest.amounts().incentiveVat()).isEqualByComparingTo("1.28");
    assertThat(rest.amounts().payable())
        .isEqualByComparingTo(rest.amounts().netDue().subtract(d("11.95")));
    assertThat(a.plus(rest.amounts()).paidAr()).isEqualByComparingTo("1000.00");
    assertThat(RemittanceAmounts.NONE.commissionReceivable()).isZero();
  }

  @Test
  void incentiveRulesMatchInsurerLineSegmentPeriodAndWindow() {
    EarlyIncentiveRule rule =
        new EarlyIncentiveRule(
            1L,
            new EarlyIncentiveRule.Terms(
                "INS-A",
                "MOTOR",
                " ",
                d("2"),
                30,
                IncentiveBasis.INCEPTION,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                true,
                "Test"));
    LocalDate on = LocalDate.of(2026, 9, 1);
    assertThat(rule.covers("INS-A", "MOTOR", "CBG", on)).isTrue();
    assertThat(rule.getSegment()).isNull();
    assertThat(rule.covers("INS-B", "MOTOR", "CBG", on)).isFalse();
    assertThat(rule.covers("INS-A", "FIRE", "CBG", on)).isFalse();
    assertThat(rule.covers("INS-A", "MOTOR", "CBG", LocalDate.of(2027, 1, 1))).isFalse();
    assertThat(rule.isEarly(LocalDate.of(2026, 8, 15), on, on)).isTrue();
    assertThat(rule.isEarly(LocalDate.of(2026, 7, 1), on, on)).isFalse();
    rule.update(
        new EarlyIncentiveRule.Terms(
            "INS-A",
            null,
            "CBG",
            d("2"),
            10,
            IncentiveBasis.BOOKING,
            LocalDate.of(2026, 1, 1),
            null,
            false,
            null));
    assertThat(rule.covers("INS-A", "FIRE", "CBG", on)).isFalse();
    assertThat(rule.isEarly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 25), on)).isTrue();
  }

  @Test
  void stagesKnowWhatTheyAllow() {
    assertThat(BatchStage.REVIEW_IN_PROCESS.isEditable()).isTrue();
    assertThat(BatchStage.FOR_APPROVAL.isEditable()).isFalse();
    assertThat(BatchStage.FOR_APPROVAL.isOpen()).isTrue();
    assertThat(BatchStage.APPROVED.isOpen()).isFalse();
    assertThat(HoldStage.DRAFT.isLive()).isTrue();
    assertThat(HoldStage.RELEASED.isLive()).isFalse();
    assertThat(HoldStage.CANCEL_FOR_APPROVAL.holdsInvoice()).isTrue();
    assertThat(HoldStage.FOR_APPROVAL.holdsInvoice()).isFalse();
  }

  @Test
  void feedFilesAreReadWithAnySeparatorAndQuotes() {
    var rows =
        CsvRows.parse(
            "﻿InvoiceNo\tAmount\n\"INV, 1\"\t10\n\nINV-2\n".getBytes(StandardCharsets.UTF_8),
            Set.of("invoiceNo"));
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).get("invoiceNo")).isEqualTo("INV, 1");
    assertThat(rows.get(0).lineNo()).isEqualTo(2);
    assertThat(rows.get(1).get("amount")).isEmpty();
    assertThatThrownBy(() -> rows.get(1).require("amount"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Line 3");
    assertThatThrownBy(() -> CsvRows.parse(new byte[0], Set.of()))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> CsvRows.parse("a;b\n".getBytes(StandardCharsets.UTF_8), Set.of("c")))
        .isInstanceOf(BusinessRuleException.class);
  }
}
