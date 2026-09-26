package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.RecurringLine;
import com.iortatechnxt.brokerverse.journal.domain.RecurringLine.LineDetails;
import com.iortatechnxt.brokerverse.journal.domain.RecurringLine.Posting;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Conversions between journal line requests and recurring template lines. */
public final class RecurringLines {

  private RecurringLines() {}

  /**
   * Converts a request line to a template line.
   *
   * @param r request line
   * @return template line
   */
  public static RecurringLine fromRequest(JournalLineRequest r) {
    return new RecurringLine(
        new Posting(r.accountCode(), r.side(), r.amount()),
        r.currency(),
        r.exchangeRate(),
        r.branchId(),
        new LineDetails(
            r.costCenter(), r.businessLine(), r.partyCode(), r.reference(), r.narration()));
  }

  /**
   * Converts a template line to a request line.
   *
   * @param l template line
   * @return request line
   */
  public static JournalLineRequest toRequest(RecurringLine l) {
    return toRequest(l, l.getSide());
  }

  /**
   * Converts a template line to a request line with the side swapped (reversing journal).
   *
   * @param l template line
   * @return reversed request line
   */
  public static JournalLineRequest toReversedRequest(RecurringLine l) {
    return toRequest(l, l.getSide() == BalanceSide.DEBIT ? BalanceSide.CREDIT : BalanceSide.DEBIT);
  }

  /**
   * Requires debits to equal credits per transaction currency.
   *
   * @param headerCurrency header currency (default for lines without one)
   * @param lines lines
   */
  public static void requireBalanced(String headerCurrency, List<JournalLineRequest> lines) {
    Map<String, BigDecimal> net = new HashMap<>();
    for (JournalLineRequest l : lines) {
      String ccy = l.currency() != null ? l.currency() : headerCurrency;
      BigDecimal signed = l.side() == BalanceSide.DEBIT ? l.amount() : l.amount().negate();
      net.merge(ccy, signed, BigDecimal::add);
    }
    net.forEach(
        (ccy, diff) -> {
          if (diff.signum() != 0) {
            throw new BusinessRuleException(
                "UNBALANCED_TEMPLATE", "Debits and credits in " + ccy + " differ by " + diff.abs());
          }
        });
  }

  private static JournalLineRequest toRequest(RecurringLine l, BalanceSide side) {
    return new JournalLineRequest(
        l.getAccountCode(),
        side,
        l.getAmount(),
        l.getCurrency(),
        l.getExchangeRate(),
        l.getBranchId(),
        l.getCostCenter(),
        l.getBusinessLine(),
        l.getPartyCode(),
        l.getReference(),
        l.getNarration());
  }
}
