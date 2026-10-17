package com.iortatechnxt.brokerverse.frbs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.LineStatus;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import com.iortatechnxt.brokerverse.frbs.domain.PaidInvoice;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine.LineDraft;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine.Ticket;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipient;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipient.RecipientValues;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRule;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRule.RuleValues;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeCalculator;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeCalculator.Computation;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeCalculator.Unit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The service-fee computation and the line tags (FRBS 2.10.0-2.10.2, Appendix A VI). */
class ServiceFeeCalculatorTest {

  private static final LocalDate PAID = LocalDate.of(2026, 9, 10);

  private static ServiceFeeRule rule(String segment, String markets, String rate, boolean net) {
    return new ServiceFeeRule(
        new RuleValues(
            segment,
            List.of(markets.split(",")),
            new BigDecimal(rate),
            net,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2030, 12, 31),
            true,
            null));
  }

  private static PaidInvoice invoice(String no, String segment, String unit, String commission) {
    return new PaidInvoice(
        no,
        no,
        "CL-1",
        "Assured",
        "INS-1",
        segment,
        unit,
        "CC-INV",
        1L,
        "PHP",
        new BigDecimal(commission),
        new BigDecimal("100.00"),
        PAID);
  }

  @Test
  void feesAreTheRateOfTheCommissionNetOfWithholdingTax() {
    ServiceFeeRule cbg = rule("CBG", "CBG,RETAIL", "2.5", true);
    assertThat(ServiceFeeCalculator.fee(invoice("I1", "CBG", "U1", "1000.00"), cbg).fee())
        .isEqualByComparingTo("22.50");
    ServiceFeeRule gross = rule("IBG", "CORBANK", "1", false);
    ServiceFeeCalculator.Fee f =
        ServiceFeeCalculator.fee(invoice("I2", "CORBANK", "U1", "1000.00"), gross);
    assertThat(f.base()).isEqualByComparingTo("1000.00");
    assertThat(f.fee()).isEqualByComparingTo("10.00");
    assertThat(cbg.covers("RETAIL", PAID)).isTrue();
    assertThat(cbg.covers("CORBANK", PAID)).isFalse();
    assertThat(cbg.covers(null, PAID)).isFalse();
    assertThat(cbg.covers("CBG", LocalDate.of(2019, 1, 1))).isFalse();
    assertThat(cbg.getBase()).isEqualTo(ServiceFeeRule.BASE);
  }

  @Test
  void invoicesAreGroupedPerSegmentUnitAndCurrencyAndPaidToTheRecipient() {
    List<ServiceFeeRule> rules =
        List.of(rule("CBG", "CBG,RETAIL", "2.5", true), rule("IBG", "CORBANK", "1", true));
    ServiceFeeRecipient mapped =
        new ServiceFeeRecipient(1L, "U1", new RecipientValues("BR-001", "Branch 1", "CC-1", true));
    Computation c =
        ServiceFeeCalculator.compute(
            List.of(
                invoice("I1", "CBG", "U1", "1000.00"),
                invoice("I2", "RETAIL", "U1", "500.00"),
                invoice("I3", "CORBANK", "U2", "2100.00"),
                invoice("I4", "INSTITUTIONAL", "U2", "900.00"),
                invoice("I5", "CBG", null, "300.00")),
            rules,
            Map.of("U1", mapped),
            Map.of("U2", new Unit("Unit Two", "CC-2")));
    assertThat(c.lines()).hasSize(3);
    assertThat(c.uncovered()).extracting(PaidInvoice::invoiceNo).containsExactly("I4");
    assertThat(c.invoiceCount()).isEqualTo(4);
    LineDraft u1 = c.lines().get(0).draft();
    assertThat(u1.payeeCode()).isEqualTo("BR-001");
    assertThat(u1.costCenter()).isEqualTo("CC-1");
    assertThat(u1.invoiceCount()).isEqualTo(2);
    assertThat(u1.base()).isEqualByComparingTo("1300.00");
    assertThat(u1.fee()).isEqualByComparingTo("32.50");
    LineDraft u2 = c.lines().get(1).draft();
    assertThat(u2.payeeCode()).isEqualTo("U2");
    assertThat(u2.payeeName()).isEqualTo("Unit Two");
    assertThat(u2.costCenter()).isEqualTo("CC-2");
    assertThat(u2.fee()).isEqualByComparingTo("20.00");
    LineDraft none = c.lines().get(2).draft();
    assertThat(none.salesUnit()).isEqualTo("UNASSIGNED");
    assertThat(none.costCenter()).isEqualTo("CC-INV");
    assertThat(c.feeTotal()).isEqualByComparingTo("57.50");
  }

  @Test
  void aLineIsSentReleasedAndLiquidatedInOrder() {
    ServiceFeeLine line =
        new ServiceFeeLine(
            1L,
            3,
            new LineDraft(
                "CBG",
                "U1",
                "BR-001",
                "Branch 1",
                "CC-1",
                1L,
                "PHP",
                new BigDecimal("2.5"),
                1,
                BigDecimal.TEN,
                BigDecimal.ONE,
                new BigDecimal("9"),
                new BigDecimal("0.23")));
    assertThatThrownBy(() -> line.release(PAID, "u")).hasMessageContaining("COMPUTED");
    line.nextSending();
    line.sent(new Ticket("DSQ-1", "SENT", null, null));
    assertThat(line.sourceRef("SFR-2026-000001")).isEqualTo("SFR-2026-000001:3");
    line.returned(new Ticket(null, "RETURNED", null, "No payee"));
    assertThat(line.getStatus()).isEqualTo(LineStatus.RETURNED);
    assertThat(line.getRequestNo()).isEqualTo("DSQ-1");
    line.nextSending();
    assertThat(line.sourceRef("SFR-2026-000001")).isEqualTo("SFR-2026-000001:3:2");
    line.sent(new Ticket("DSQ-2", "SENT", null, null));
    line.release(PAID, "u");
    assertThatThrownBy(() -> line.liquidate(PAID.minusDays(1), "u", "9", null))
        .hasMessageContaining("before the release");
    line.liquidate(PAID, "u", "9", "ok");
    assertThat(line.getStatus()).isEqualTo(LineStatus.LIQUIDATED);
    assertThat(line.getLiquidationRef()).isEqualTo("9");
  }

  @Test
  void aRunKeepsItsTotalsWhileComputed() {
    ServiceFeeRun run =
        new ServiceFeeRun(1L, "SFR-2026-000009", PAID, PAID, Instant.parse("2026-09-10T00:00:00Z"));
    run.computed(2, new BigDecimal("12.00"), Instant.parse("2026-09-11T00:00:00Z"));
    assertThat(run.getInvoiceCount()).isEqualTo(2);
    assertThat(RunStage.COMPUTED.editable()).isTrue();
    run.moveTo(RunStage.FOR_APPROVAL);
    assertThatThrownBy(() -> run.computed(1, BigDecimal.ONE, Instant.now()))
        .hasMessageContaining("recomputed");
  }
}
