package com.iortatechnxt.brokerverse.tax.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aggregates income payments into BIR Form 2307 lines: per payee, per ATC, the income of each month
 * of the quarter and the tax withheld for the quarter. Payees and ATCs are returned in code order
 * so certificates and the QAP list are deterministic. Negative entries (e.g. commission recovered
 * on a return premium) reduce the month in which they occur.
 */
public final class Certificate2307Aggregator {

  private static final int MONTHS = 3;

  private Certificate2307Aggregator() {}

  /**
   * Aggregates the entries of one quarter.
   *
   * @param quarter the calendar quarter
   * @param entries income payments dated inside the quarter
   * @return lines by payee code (sorted), each list sorted by ATC
   * @throws BusinessRuleException when the period is not a quarter or an entry lies outside it
   */
  public static Map<String, List<AtcQuarterAmounts>> aggregate(
      TaxPeriod quarter, List<WithholdingEntry> entries) {
    if (!quarter.isQuarter()) {
      throw new BusinessRuleException(
          "NOT_A_QUARTER", "Form 2307 covers a calendar quarter, not " + quarter.label());
    }
    Map<String, Map<String, Accumulator>> byPayee = new TreeMap<>();
    for (WithholdingEntry e : entries) {
      int month = quarter.monthIndex(e.date());
      byPayee
          .computeIfAbsent(e.partyCode(), k -> new TreeMap<>())
          .computeIfAbsent(e.atc(), k -> new Accumulator(e.incomeNature()))
          .add(month, e.income(), e.tax());
    }
    Map<String, List<AtcQuarterAmounts>> out = new TreeMap<>();
    byPayee.forEach(
        (payee, atcs) -> {
          List<AtcQuarterAmounts> lines = new ArrayList<>();
          atcs.forEach((atc, acc) -> lines.add(acc.toLine(atc)));
          out.put(payee, lines);
        });
    return out;
  }

  /** Running totals of one payee and ATC. */
  private static final class Accumulator {
    private final String nature;
    private final BigDecimal[] months = {Money.zero(), Money.zero(), Money.zero()};
    private BigDecimal tax = Money.zero();

    Accumulator(String nature) {
      this.nature = nature;
    }

    void add(int month, BigDecimal income, BigDecimal withheld) {
      months[month - 1] = months[month - 1].add(Money.nz(income));
      tax = tax.add(Money.nz(withheld));
    }

    AtcQuarterAmounts toLine(String atc) {
      return new AtcQuarterAmounts(atc, nature, months[0], months[1], months[MONTHS - 1], tax);
    }
  }
}
