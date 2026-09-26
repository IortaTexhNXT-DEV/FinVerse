package com.iortatechnxt.brokerverse.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.claims.domain.ClaimPolicy;
import com.iortatechnxt.brokerverse.claims.domain.ClaimPolicyValues;
import com.iortatechnxt.brokerverse.claims.domain.ClaimStatus;
import com.iortatechnxt.brokerverse.claims.domain.ClaimTotals;
import com.iortatechnxt.brokerverse.claims.domain.CostType;
import com.iortatechnxt.brokerverse.claims.domain.EstimateSide;
import com.iortatechnxt.brokerverse.claims.domain.EstimateType;
import com.iortatechnxt.brokerverse.claims.domain.MovementKind;
import com.iortatechnxt.brokerverse.claims.domain.MovementTotal;
import com.iortatechnxt.brokerverse.claims.domain.ShareSplit;
import com.iortatechnxt.brokerverse.claims.service.ClaimFigures;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure claim rules: shares and coinsurance split, totals, estimate types, figures. */
class ClaimDomainTest {

  private static ClaimPolicy policy(String share, boolean leader, String coinsurer) {
    return new ClaimPolicy(
        new ClaimPolicyValues(
            1L,
            "P-1",
            "FIRE-COM",
            "Fire",
            "FIRE",
            "C-1",
            "Client",
            "Insured",
            null,
            2026,
            new BigDecimal(share),
            leader,
            coinsurer,
            null,
            null,
            BigDecimal.TEN));
  }

  private static BigDecimal amount(String value) {
    return new BigDecimal(value);
  }

  @Test
  void coinsuranceSplitKeepsCumulativeSharesExact() {
    ClaimPolicy leader = policy("60", true, "CO-1");
    ShareSplit first = leader.split(BigDecimal.ZERO, amount("0.01"));
    ShareSplit second = leader.split(amount("0.01"), amount("0.01"));
    assertThat(first.ours().add(second.ours()))
        .isEqualByComparingTo(leader.ourShare(amount("0.02")));
    assertThat(first.payable()).isEqualByComparingTo("0.01");
    assertThat(first.coinsurers()).isEqualByComparingTo(amount("0.01").subtract(first.ours()));
    assertThat(leader.leadsCoinsurance()).isTrue();

    ClaimPolicy follower = policy("60", false, "CO-1");
    ShareSplit f = follower.split(BigDecimal.ZERO, amount("1000"));
    assertThat(f.ours()).isEqualByComparingTo("600.00");
    assertThat(f.payable()).isEqualByComparingTo("600.00");
    assertThat(f.coinsurers()).isEqualByComparingTo("0");

    ClaimPolicy direct = policy("100", true, null);
    assertThat(direct.leadsCoinsurance()).isFalse();
    assertThat(direct.split(BigDecimal.ZERO, amount("123.45")).payable())
        .isEqualByComparingTo("123.45");
  }

  @Test
  void totalsGuardOutstandingAndRecoveries() {
    ClaimTotals t = new ClaimTotals();
    t.setEstimate(EstimateSide.PAYMENT, CostType.LOSS, amount("1000"));
    t.setEstimate(EstimateSide.PAYMENT, CostType.EXPENSE, amount("100"));
    t.setEstimate(EstimateSide.RECOVERY, CostType.LOSS, amount("50"));
    t.addPaid(EstimateSide.PAYMENT, CostType.LOSS, amount("400"));
    t.addPaid(EstimateSide.PAYMENT, CostType.EXPENSE, amount("100"));
    t.addPaid(EstimateSide.RECOVERY, CostType.LOSS, amount("20"));

    assertThat(t.paymentOutstanding()).isEqualByComparingTo("600");
    assertThat(t.paymentEstimate()).isEqualByComparingTo("1100");
    assertThat(t.totalPaid()).isEqualByComparingTo("500");
    assertThat(t.outstanding(EstimateSide.RECOVERY, CostType.LOSS)).isEqualByComparingTo("30");
    assertThatThrownBy(() -> t.addPaid(EstimateSide.PAYMENT, CostType.LOSS, amount("601")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("loss");
    assertThatThrownBy(() -> t.addPaid(EstimateSide.RECOVERY, CostType.LOSS, amount("31")))
        .hasMessageContaining("recovery");
    assertThatThrownBy(() -> t.setEstimate(EstimateSide.PAYMENT, CostType.LOSS, amount("399")))
        .hasMessageContaining("already paid");
    assertThatThrownBy(() -> t.setEstimate(EstimateSide.RECOVERY, CostType.LOSS, amount("10")))
        .hasMessageContaining("already recovered");
    assertThatThrownBy(() -> t.setEstimate(EstimateSide.RECOVERY, CostType.EXPENSE, amount("10")))
        .hasMessageContaining("loss only");
  }

  @Test
  void estimateTypesFollowTheReportsBook() {
    assertThat(EstimateType.of(EstimateSide.PAYMENT, false).code()).isEqualTo(1);
    assertThat(EstimateType.of(EstimateSide.RECOVERY, false).code()).isEqualTo(2);
    assertThat(EstimateType.of(EstimateSide.PAYMENT, true).code()).isEqualTo(3);
    assertThat(EstimateType.of(EstimateSide.RECOVERY, true).code()).isEqualTo(4);
    assertThat(ClaimStatus.REOPENED.isActive()).isTrue();
    assertThat(ClaimStatus.WITHDRAWN.isFinished()).isTrue();
  }

  @Test
  void figuresSumTypedLines() {
    List<MovementTotal> totals =
        List.of(
            total(MovementKind.ESTIMATE, EstimateSide.PAYMENT, CostType.LOSS, "1000"),
            total(MovementKind.ESTIMATE, EstimateSide.PAYMENT, CostType.EXPENSE, "200"),
            total(MovementKind.ESTIMATE, EstimateSide.RECOVERY, CostType.LOSS, "80"),
            total(MovementKind.PAID, EstimateSide.PAYMENT, CostType.LOSS, "300"),
            total(MovementKind.PAID, EstimateSide.PAYMENT, CostType.EXPENSE, "50"),
            total(MovementKind.PAID, EstimateSide.RECOVERY, CostType.LOSS, "30"));
    ClaimFigures f = ClaimFigures.of(totals, false);
    assertThat(f.paymentEstimate(true)).isEqualByComparingTo("1200");
    assertThat(f.paymentEstimate(false)).isEqualByComparingTo("1000");
    assertThat(f.paymentOutstanding(true)).isEqualByComparingTo("850");
    assertThat(f.paymentOutstanding(false)).isEqualByComparingTo("700");
    assertThat(f.recoveryOutstanding()).isEqualByComparingTo("50");
    assertThat(f.netPaid()).isEqualByComparingTo("320");
    assertThat(ClaimFigures.of(totals, true).paid(true)).isEqualByComparingTo("700");
    assertThat(f.plus(f).paid(false)).isEqualByComparingTo("600");
    assertThat(f.isEmpty()).isFalse();
    assertThat(ClaimFigures.none().isEmpty()).isTrue();
  }

  private static MovementTotal total(
      MovementKind kind, EstimateSide side, CostType cost, String amount) {
    BigDecimal v = new BigDecimal(amount);
    return new MovementTotal(1L, kind, side, cost, v, v.multiply(BigDecimal.TWO));
  }
}
