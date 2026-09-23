package com.iortatechnxt.finverse.accounting;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.accounting.service.AccountingRuleService.Simulation;
import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationTest {

  private static JournalLineRequest line(String account, BalanceSide side, String amount) {
    return new JournalLineRequest(
        account, side, new BigDecimal(amount), null, null, null, null, null, null, null, null);
  }

  @Test
  void reportsTheImbalanceOfSampleAmountsThatDoNotAddUp() {
    // POLICY_ISSUE with TOTAL_DUE 100,000 against premium and taxes of 125,450.
    Simulation s =
        Simulation.of(
            1L,
            "Policy issue",
            List.of(
                line("1301", BalanceSide.DEBIT, "100000.00"),
                line("4101", BalanceSide.CREDIT, "100000.00"),
                line("2301", BalanceSide.CREDIT, "12500.00"),
                line("2302", BalanceSide.CREDIT, "12000.00"),
                line("2303", BalanceSide.CREDIT, "750.00"),
                line("2304", BalanceSide.CREDIT, "200.00")));

    assertThat(s.totalDebit()).isEqualByComparingTo("100000.00");
    assertThat(s.totalCredit()).isEqualByComparingTo("125450.00");
    assertThat(s.difference()).isEqualByComparingTo("-25450.00");
    assertThat(s.balanced()).isFalse();
  }

  @Test
  void balancedOnlyWithLinesAndEqualSides() {
    Simulation balanced =
        Simulation.of(
            1L,
            "Receipt",
            List.of(
                line("1111", BalanceSide.DEBIT, "1500.00"),
                line("4700", BalanceSide.CREDIT, "1500.00")));
    assertThat(balanced.balanced()).isTrue();
    assertThat(balanced.difference()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(Simulation.of(1L, "Empty", List.of()).balanced()).isFalse();
  }
}
