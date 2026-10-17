package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.OrAmounts;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeries;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationPlanner;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationPlanner.InvoiceBalances;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationPlanner.Plan;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Component application (CSHID.020/022), the 2% CWT cap and receipt numbering (CSHID.006/015). */
class ApplicationPlannerTest {

  private static final Map<LedgerComponent, BigDecimal> BALANCES =
      Map.of(
          LedgerComponent.BASIC, new BigDecimal("1000.00"),
          LedgerComponent.DST, new BigDecimal("125.00"),
          LedgerComponent.PREMIUM_TAX_VAT, new BigDecimal("120.00"),
          LedgerComponent.LGT, new BigDecimal("7.50"),
          LedgerComponent.DTIP, new BigDecimal("900.00"));

  private static BigDecimal d(String v) {
    return new BigDecimal(v);
  }

  @Test
  void paymentsFollowTheHierarchyDstVatLgtOtherBasic() {
    Plan plan =
        ApplicationPlanner.plan(new InvoiceBalances(BALANCES, BigDecimal.ZERO), d("300.00"), false);
    assertThat(plan.allocation().keySet())
        .containsExactly(
            LedgerComponent.DST,
            LedgerComponent.PREMIUM_TAX_VAT,
            LedgerComponent.LGT,
            LedgerComponent.BASIC);
    assertThat(plan.allocation().get(LedgerComponent.BASIC)).isEqualByComparingTo("47.50");
    assertThat(plan.applied()).isEqualByComparingTo("300.00");
    assertThat(plan.excess()).isZero();
  }

  @Test
  void anOverpaymentLeavesAnExcessAndTheDtipIsNeverApplied() {
    Plan plan =
        ApplicationPlanner.plan(
            new InvoiceBalances(BALANCES, BigDecimal.ZERO), d("2000.00"), false);
    assertThat(plan.applied()).isEqualByComparingTo("1252.50");
    assertThat(plan.excess()).isEqualByComparingTo("747.50");
    assertThat(plan.allocation()).doesNotContainKey(LedgerComponent.DTIP);
  }

  @Test
  void aCwtClientIsCappedAtNinetyEightPercentAndDstOnlyTakesTheDst() {
    BigDecimal withheld = ApplicationPlanner.withheld(d("1252.50"), BigDecimal.ZERO, d("98"));
    assertThat(withheld).isEqualByComparingTo("25.05");
    Plan capped =
        ApplicationPlanner.plan(new InvoiceBalances(BALANCES, withheld), d("1252.50"), false);
    assertThat(capped.applied()).isEqualByComparingTo("1227.45");
    assertThat(capped.excess()).isEqualByComparingTo("25.05");
    assertThat(ApplicationPlanner.withheld(d("1227.45"), d("25.05"), d("98"))).isZero();

    Plan dst =
        ApplicationPlanner.plan(new InvoiceBalances(BALANCES, BigDecimal.ZERO), d("500.00"), true);
    assertThat(dst.allocation()).containsOnlyKeys(LedgerComponent.DST);
    assertThat(dst.excess()).isEqualByComparingTo("375.00");
  }

  @Test
  void seriesNumbersArePaddedAndRunOut() {
    ReceiptSeries s = new ReceiptSeries(1L, 1L, ReceiptKind.AR, "AR-X-", 98, 100);
    s.update("ATP", 100, 1);
    assertThat(s.allocate()).isEqualTo("AR-X-098");
    assertThat(s.allocate()).isEqualTo("AR-X-099");
    assertThat(s.isLow()).isTrue();
    assertThat(s.allocate()).isEqualTo("AR-X-100");
    assertThat(s.remaining()).isZero();
    Assertions.assertThatThrownBy(s::allocate)
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo("RECEIPT_SERIES_DEPLETED");
    Assertions.assertThatThrownBy(() -> new ReceiptSeries(1L, 1L, ReceiptKind.OR, "OR-", 5, 1))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(new OrAmounts(d("100"), d("12"), d("2")).net()).isEqualByComparingTo("110");
    assertThat(List.of(ReceiptKind.values())).hasSize(2);
  }
}
