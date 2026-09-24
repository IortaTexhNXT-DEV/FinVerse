package com.iortatechnxt.brokerverse.nbreport.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbreport.domain.SalesTarget;
import com.iortatechnxt.brokerverse.nbreport.domain.UnitLevel;
import com.iortatechnxt.brokerverse.nbreport.service.UnitProduction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Pure values of the New Business reports. */
class NbReportValuesTest {

  private static UnitProduction unit(String premium, String target) {
    return new UnitProduction(
        UnitLevel.TEAM,
        "T1",
        "Team 1",
        2,
        new BigDecimal(premium),
        BigDecimal.ZERO,
        0,
        target == null ? null : new BigDecimal(target),
        BigDecimal.ZERO);
  }

  @Test
  void achievementIsThePremiumShareOfTheTarget() {
    assertThat(unit("1500", "1000").achievement()).isEqualByComparingTo("150.0");
    assertThat(unit("333", "1000").achievement()).isEqualByComparingTo("33.3");
    assertThat(unit("10", "0").achievement()).isNull();
    assertThat(unit("10", null).achievement()).isNull();
  }

  @Test
  void targetsAreRoundedAndNeverNegative() {
    var unit =
        new SalesTarget.Unit(
            UnitLevel.OFFICER,
            " ao ",
            LocalDate.parse("2026-01-01"),
            LocalDate.parse("2026-01-31"));
    SalesTarget t =
        new SalesTarget(
            1L, unit, new SalesTarget.Values(3, new BigDecimal("10.005"), BigDecimal.ONE));
    assertThat(t.getUnitCode()).isEqualTo("ao");
    assertThat(t.getTargetPremium()).isEqualByComparingTo("10.01");
    assertThat(t.getCurrency()).isEqualTo("PHP");
    var negative = new SalesTarget.Values(-1, BigDecimal.ONE, BigDecimal.ONE);
    assertThatThrownBy(() -> new SalesTarget(1L, unit, negative))
        .isInstanceOf(BusinessRuleException.class);
    var nullPremium = new SalesTarget.Values(1, null, BigDecimal.ONE);
    assertThatThrownBy(() -> new SalesTarget(1L, unit, nullPremium))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void codesAndFlagsAreShownAsWords() {
    assertThat(NbReportSupport.label("POLICY_ISSUED")).isEqualTo("Policy issued");
    assertThat(NbReportSupport.label(null)).isEmpty();
    assertThat(NbReportSupport.label("")).isEmpty();
    Map<String, Object> row = new HashMap<>();
    row.put("f", true);
    row.put("g", null);
    row.put("k", "NOT_SENT");
    NbReportSupport.relabel(NbReportSupport.flag(NbReportSupport.flag(row, "f"), "g"), "k");
    assertThat(row)
        .containsEntry("f", "Yes")
        .containsEntry("g", "No")
        .containsEntry("k", "Not sent");
  }
}
