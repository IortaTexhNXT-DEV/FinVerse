package com.iortatechnxt.finverse.fixedasset;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.fixedasset.domain.DepreciationMethod;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationCalculator;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationCalculator.Basis;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationCalculator.Charge;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationCalculator.Position;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DepreciationCalculatorTest {

  private static final Position NEW = new Position(BigDecimal.ZERO, 0);

  @Test
  void straightLineChargesDepreciableAmountEvenly() {
    Basis basis = new Basis(bd("60000"), bd("3000"), DepreciationMethod.STRAIGHT_LINE, 60);
    assertThat(DepreciationCalculator.monthlyCharge(basis, NEW)).isEqualByComparingTo("950.00");
    Position afterYear = DepreciationCalculator.advance(basis, NEW, 12);
    assertThat(afterYear.accumulated()).isEqualByComparingTo("11400.00");
    assertThat(afterYear.months()).isEqualTo(12);
  }

  @Test
  void straightLineLastMonthAbsorbsRoundingAndStopsAtResidual() {
    Basis basis = new Basis(bd("1000"), BigDecimal.ZERO, DepreciationMethod.STRAIGHT_LINE, 3);
    assertThat(DepreciationCalculator.monthlyCharge(basis, NEW)).isEqualByComparingTo("333.33");
    Position end = DepreciationCalculator.advance(basis, NEW, 10);
    assertThat(end.accumulated()).isEqualByComparingTo("1000.00");
    assertThat(end.months()).isEqualTo(3);
    assertThat(DepreciationCalculator.monthlyCharge(basis, end)).isZero();
  }

  @Test
  void decliningBalanceChargesTwiceTheStraightLineRateOnNetBookValue() {
    Basis basis = new Basis(bd("36000"), BigDecimal.ZERO, DepreciationMethod.DECLINING_BALANCE, 36);
    assertThat(DepreciationCalculator.monthlyCharge(basis, NEW)).isEqualByComparingTo("2000.00");
    Position second = new Position(bd("2000.00"), 1);
    assertThat(DepreciationCalculator.monthlyCharge(basis, second)).isEqualByComparingTo("1888.89");
    Charge twoMonths = DepreciationCalculator.charge(basis, NEW, 2);
    assertThat(twoMonths.amount()).isEqualByComparingTo("3888.89");
    assertThat(twoMonths.months()).isEqualTo(2);
  }

  @Test
  void decliningBalanceIsFullyDepreciatedAtEndOfLifeAndNeverBelowResidual() {
    Basis basis = new Basis(bd("10000"), bd("1000"), DepreciationMethod.DECLINING_BALANCE, 12);
    Position end = DepreciationCalculator.advance(basis, NEW, 12);
    assertThat(end.accumulated()).isEqualByComparingTo("9000.00");
    Position nearlyDone = new Position(bd("8990.00"), 5);
    assertThat(DepreciationCalculator.monthlyCharge(basis, nearlyDone))
        .isEqualByComparingTo("10.00");
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
