package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.common.util.AmountInWords;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.reinsurance.domain.SoaFigures;
import java.math.BigDecimal;
import java.util.List;

/**
 * Printed layout of a statement of account: income and outgo columns, sub-totals, the balance
 * placed on the smaller side so both columns total the same, and the balance in words.
 *
 * @param lines statement lines
 * @param incomeSubtotal sum of the income column
 * @param outgoSubtotal sum of the outgo column
 * @param balance absolute balance
 * @param balanceLabel "Balance due to reinsurer" or "Balance due from reinsurer"
 * @param balanceOnIncome true when the balance is shown in the income column
 * @param total column total after the balance
 * @param amountInWords balance in words
 */
public record SoaLayout(
    List<Line> lines,
    BigDecimal incomeSubtotal,
    BigDecimal outgoSubtotal,
    BigDecimal balance,
    String balanceLabel,
    boolean balanceOnIncome,
    BigDecimal total,
    String amountInWords) {

  /** Canonical constructor copying the lines. */
  public SoaLayout {
    lines = List.copyOf(lines);
  }

  /**
   * Lays out a statement.
   *
   * @param f figures
   * @param currencyName currency name for the amount in words
   * @return layout
   */
  public static SoaLayout of(SoaFigures f, String currencyName) {
    BigDecimal zero = Money.zero();
    List<Line> lines =
        List.of(
            new Line("Premium ceded", f.premium(), zero),
            new Line("Commission", zero, f.commission()),
            new Line("Premium tax / levy", zero, f.levy()),
            new Line("Losses paid", zero, f.lossesPaid()),
            new Line("Recoveries (salvage / subrogation)", f.recoveries(), zero),
            new Line("Premium reserve retained", zero, f.premiumReserveRetained()),
            new Line("Premium reserve released", f.premiumReserveReleased(), zero),
            new Line("Interest on reserves", f.interest(), zero),
            new Line("O/S loss reserve retained", zero, f.lossReserveRetained()),
            new Line("O/S loss reserve released", f.lossReserveReleased(), zero));
    BigDecimal income = Money.round(f.income());
    BigDecimal outgo = Money.round(f.outgo());
    BigDecimal balance = income.subtract(outgo);
    boolean dueToReinsurer = balance.signum() >= 0;
    return new SoaLayout(
        lines,
        income,
        outgo,
        balance.abs(),
        dueToReinsurer ? "Balance due to reinsurer" : "Balance due from reinsurer",
        !dueToReinsurer,
        income.max(outgo),
        AmountInWords.spell(balance, currencyName));
  }

  /**
   * One line of the statement.
   *
   * @param label particulars
   * @param income amount credited to the reinsurer
   * @param outgo amount debited to the reinsurer
   */
  public record Line(String label, BigDecimal income, BigDecimal outgo) {}
}
