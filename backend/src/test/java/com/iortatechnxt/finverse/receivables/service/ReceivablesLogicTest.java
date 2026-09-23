package com.iortatechnxt.finverse.receivables.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.receivables.domain.BrsFigures;
import com.iortatechnxt.finverse.receivables.domain.PayerType;
import com.iortatechnxt.finverse.receivables.domain.PdcStatus;
import com.iortatechnxt.finverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.finverse.receivables.service.AutoMatcher.Item;
import com.iortatechnxt.finverse.receivables.service.AutoMatcher.Proposal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure calculation logic of the receivables module. */
class ReceivablesLogicTest {

  private static final LocalDate D = LocalDate.of(2026, 9, 1);

  private static Item item(long id, int dayOffset, String amount, String ref, String text) {
    return new Item(id, D.plusDays(dayOffset), new BigDecimal(amount), ref, text);
  }

  @Test
  void csvSplitterHandlesQuotesAndEscapes() {
    assertThat(BankStatementParser.splitCsv("a,\"b,c\",\"say \"\"hi\"\"\","))
        .containsExactly("a", "b,c", "say \"hi\"", "");
  }

  @Test
  void parserComputesRunningBalancesAndTotals() {
    var parsed =
        BankStatementParser.parse(
            "date,description,reference,debit,credit\n\n2026-09-01,Dep,R1,,100.00\n2026-09-03,Chq,C1,40.50,\n",
            new BigDecimal("1000"));
    assertThat(parsed.lines()).hasSize(2);
    assertThat(parsed.lines().get(1).balance()).isEqualByComparingTo("1059.50");
    assertThat(parsed.summary().closingBalance()).isEqualByComparingTo("1059.50");
    assertThat(parsed.summary().totalDebit()).isEqualByComparingTo("40.50");
    assertThat(parsed.summary().totalCredit()).isEqualByComparingTo("100.00");
    assertThat(parsed.summary().periodFrom()).isEqualTo(D);
    assertThat(parsed.summary().periodTo()).isEqualTo(D.plusDays(2));
    assertThat(parsed.lines().get(0).reference()).isEqualTo("R1");
  }

  @Test
  void autoMatcherPrefersReferenceThenClosestDate() {
    List<Item> book =
        List.of(
            item(1, 0, "100.00", "OR-1", "Receipt OR-1"),
            item(2, 1, "100.00", "OR-2", "Receipt OR-2 CHEQUE 555"),
            item(3, 0, "-40.00", "700201", "Rent"),
            item(4, 20, "100.00", "OR-3", "far away"));
    List<Item> bank =
        List.of(
            item(10, 2, "100.00", "555", "CHQ DEPOSIT"),
            item(11, 1, "100.00", null, "DEPOSIT"),
            item(12, 3, "-40.00", "700201", "CHEQUE PAID"),
            item(13, 2, "999.00", "X", "unknown"));
    List<Proposal> proposals = AutoMatcher.match(book, bank, 7, Map.of());
    assertThat(proposals)
        .containsExactlyInAnyOrder(
            new Proposal(List.of(2L), List.of(10L)),
            new Proposal(List.of(1L), List.of(11L)),
            new Proposal(List.of(3L), List.of(12L)));
  }

  @Test
  void autoMatcherGroupsDepositSlipsAndSplitReceipts() {
    List<Item> book =
        List.of(
            item(1, 0, "60.00", "OR-1", "a"),
            item(2, 1, "40.00", "OR-2", "b"),
            item(3, 1, "30.00", "OR-3", "applied"),
            item(4, 1, "20.00", "OR-3", "unapplied"));
    List<Item> bank =
        List.of(item(10, 2, "100.00", "DS-1", "DEPOSIT"), item(11, 2, "50.00", "OR-3", "TRANSFER"));
    List<Proposal> proposals =
        AutoMatcher.match(book, bank, 7, Map.of("DS-1", Set.of("OR-1", "OR-2")));
    assertThat(proposals)
        .containsExactlyInAnyOrder(
            new Proposal(List.of(1L, 2L), List.of(10L)),
            new Proposal(List.of(3L, 4L), List.of(11L)));
    assertThat(
            AutoMatcher.match(
                book,
                List.of(item(20, 2, "99.00", "DS-1", "x")),
                7,
                Map.of("DS-1", Set.of("OR-1"))))
        .isEmpty();
    assertThat(AutoMatcher.referenceMatches(item(1, 0, "1", null, null), item(2, 0, "1", "", "")))
        .isFalse();
  }

  @Test
  void brsFiguresFollowTheFormula() {
    BrsFigures f =
        BrsFigures.of(
            new BigDecimal("10000"),
            new BigDecimal("1500"),
            new BigDecimal("700"),
            new BigDecimal("50"),
            new BigDecimal("20"),
            new BigDecimal("9170"));
    assertThat(f.computedBankBalance()).isEqualByComparingTo("9170");
    assertThat(f.difference()).isZero();
  }

  @Test
  void enumsDescribeTheirRules() {
    assertThat(PdcStatus.ON_HAND.isHeld()).isTrue();
    assertThat(PdcStatus.CLEARED.isHeld()).isFalse();
    assertThat(PdcStatus.DEPOSITED.next()).contains(PdcStatus.CLEARED, PdcStatus.BOUNCED);
    assertThat(PdcStatus.RETURNED.next()).isEmpty();
    assertThat(ReceiptMode.PDC.isCheque()).isTrue();
    assertThat(ReceiptMode.CARD.requiresDeposit()).isFalse();
    assertThat(PayerType.of(PartyType.BROKER)).isEqualTo(PayerType.INTERMEDIARY);
    assertThat(PayerType.of(PartyType.SUPPLIER)).isEqualTo(PayerType.OTHER);
    assertThat(PayerType.debtorPartyTypes())
        .contains(PartyType.REINSURER)
        .doesNotContain(PartyType.SUPPLIER);
    assertThat(PayerType.OTHER.partyTypes()).isEmpty();
  }
}
