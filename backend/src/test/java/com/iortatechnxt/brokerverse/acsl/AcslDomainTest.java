package com.iortatechnxt.brokerverse.acsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLineValues;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlControl;
import com.iortatechnxt.brokerverse.acsl.domain.LineOrigin;
import com.iortatechnxt.brokerverse.acsl.domain.ReconBucket;
import com.iortatechnxt.brokerverse.acsl.domain.ReconRun;
import com.iortatechnxt.brokerverse.acsl.domain.SlSource;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionLineRules;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionService;
import com.iortatechnxt.brokerverse.acsl.service.SoaRowParser;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Pure rules of ACSL: correction balance, ledger signs, proposal components, SOA values. */
class AcslDomainTest {

  private static CorrectionLineValues line(BalanceSide side, String amount) {
    return new CorrectionLineValues(
        "1210.01",
        side,
        new BigDecimal(amount),
        "CL-1",
        null,
        null,
        null,
        null,
        null,
        LineOrigin.MANUAL,
        null,
        null);
  }

  @Test
  void aCorrectionIsBalancedWhenDebitsEqualCredits() {
    Correction c =
        new Correction(
            1L, 1L, "COR-1", new Correction.Header("OTHER", null, null, null, "PHP", "Test"), null);
    c.replaceLines(List.of(line(BalanceSide.DEBIT, "10.00"), line(BalanceSide.CREDIT, "10.00")));
    assertThat(c.isBalanced()).isTrue();
    assertThat(c.totalDebit()).isEqualByComparingTo("10.00");
    c.replaceLines(List.of(line(BalanceSide.DEBIT, "10.00"), line(BalanceSide.CREDIT, "9.99")));
    assertThat(c.isBalanced()).isFalse();
    assertThat(c.getLines()).extracting(l -> l.getLineNo()).containsExactly(1, 2);
    assertThat(c.getLines().get(1).values().amount()).isEqualByComparingTo("9.99");
  }

  @Test
  void aDebitRaisesAReceivableComponentAndLowersAPayableOne() {
    BigDecimal ten = BigDecimal.TEN;
    assertThat(CorrectionLineRules.ledgerChange(LedgerComponent.BASIC, ten)).isEqualTo(ten);
    assertThat(CorrectionLineRules.ledgerChange(LedgerComponent.DTIP, ten)).isEqualTo(ten.negate());
    assertThat(CorrectionLineRules.ledgerChange(LedgerComponent.COMMISSION_VAT, ten.negate()))
        .isEqualTo(ten);
  }

  @Test
  void aProposalReusesTheComponentUnlessAnotherIsGiven() {
    assertThat(
            new CorrectionService.Proposal("B", 1, "A", null, "BASIC", null)
                .targetComponentOrSame())
        .isEqualTo("BASIC");
    assertThat(
            new CorrectionService.Proposal("B", 1, "A", null, "BASIC", " ").targetComponentOrSame())
        .isEqualTo("BASIC");
    assertThat(
            new CorrectionService.Proposal("B", 1, "A", null, "BASIC", "OTHER")
                .targetComponentOrSame())
        .isEqualTo("OTHER");
  }

  @Test
  void aControlSplitsItsListsAndARunCountsItsBuckets() {
    GlSlControl control =
        new GlSlControl(
            1L,
            "2210",
            new GlSlControl.Setting(SlSource.OPS_LEDGER, "DTIP, WTAX,", null, "PHP", true));
    assertThat(control.componentList()).containsExactly("DTIP", "WTAX");
    assertThat(control.documentTypeList()).isEmpty();
    ReconRun run = new ReconRun(1L, 1, Instant.now(), "acsl");
    run.counted(Map.of(ReconBucket.REMITTED, 2, ReconBucket.NOT_FOUND, 1), 1);
    assertThat(run.getRemitted()).isEqualTo(2);
    assertThat(run.getNotFound()).isEqualTo(1);
    assertThat(run.getOutstanding()).isZero();
    assertThat(run.getWithVariance()).isEqualTo(1);
  }

  @Test
  void soaValuesAreReadWithSeparatorsNegativesAndBothDateFormats() {
    assertThat(SoaRowParser.amount("1,234.5")).isEqualByComparingTo("1234.50");
    assertThat(SoaRowParser.amount("(1,000.00)")).isEqualByComparingTo("-1000.00");
    assertThat(SoaRowParser.amount(" ")).isNull();
    assertThat(SoaRowParser.date("12/31/2026")).hasToString("2026-12-31");
    assertThat(SoaRowParser.date("2026-01-02")).hasToString("2026-01-02");
    assertThat(SoaRowParser.date(null)).isNull();
    assertThatThrownBy(() -> SoaRowParser.amount("abc")).isInstanceOf(NumberFormatException.class);
  }
}
