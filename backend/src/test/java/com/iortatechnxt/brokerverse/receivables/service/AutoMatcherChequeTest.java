package com.iortatechnxt.brokerverse.receivables.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.receivables.service.AutoMatcher.Item;
import com.iortatechnxt.brokerverse.receivables.service.AutoMatcher.Proposal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Cheque number and amount matching (FRBS 3.3.2). */
class AutoMatcherChequeTest {

  private static final LocalDate MAY = LocalDate.of(2030, 5, 2);

  @Test
  void chequeNumbersAreNormalised() {
    assertThat(AutoMatcher.chequeNo(null)).isEmpty();
    assertThat(AutoMatcher.chequeNo("chk-004567")).isEqualTo("CHK004567");
    assertThat(AutoMatcher.chequeNo("004567")).isEqualTo("4567");
    assertThat(AutoMatcher.chequeNo("0000")).isEqualTo("0");
  }

  @Test
  void chequePassMatchesAcrossTheDateWindowOnlyWhenAsked() {
    List<Item> book =
        List.of(
            new Item(1L, MAY, new BigDecimal("-500.00"), "PV-1", "Check 004567 to landlord"),
            new Item(2L, MAY, new BigDecimal("-80.00"), "007788", "Utilities"),
            new Item(3L, MAY.plusDays(40), new BigDecimal("200.00"), "OR-9", "Receipt"));
    List<Item> bank =
        List.of(
            new Item(11L, MAY.plusDays(34), new BigDecimal("-500.00"), "4567", "Check encashed"),
            new Item(12L, MAY.plusDays(34), new BigDecimal("-80.00"), "7788", "Check encashed"),
            new Item(13L, MAY.plusDays(41), new BigDecimal("200.00"), "OR-9", "Deposit"),
            new Item(14L, MAY.plusDays(3), new BigDecimal("-9.00"), "12", "Short reference"));

    List<Proposal> standard = AutoMatcher.match(book, bank, 7, Map.of(), false);
    assertThat(standard).extracting(Proposal::bankIds).containsExactly(List.of(13L));

    List<Proposal> cheques = AutoMatcher.match(book, bank, 7, Map.of(), true);
    assertThat(cheques)
        .extracting(Proposal::bankIds)
        .containsExactlyInAnyOrder(List.of(11L), List.of(12L), List.of(13L));
  }
}
