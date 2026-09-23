package com.iortatechnxt.finverse.finreport.service;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * A financial statement format (PREMIA "Format ID") and its evaluation.
 *
 * <p>Evaluation: every account balance is added to the first ACCOUNTS line covering the account,
 * multiplied by the line sign; RESULT lines receive the unclosed income less expenses; ACCOUNTS and
 * RESULT lines are rounded; TOTAL lines are then computed in line order from the (rounded) lines
 * they reference, so totals always add up on the printed statement.
 *
 * @param code format code
 * @param description description
 * @param statementType "BS" (balance sheet) or "IE" (income and expense)
 * @param lines ordered lines
 */
public record StatementFormat(
    String code, String description, String statementType, List<FormatLine> lines) {

  /** Canonical constructor copying the lines. */
  public StatementFormat {
    lines = List.copyOf(lines);
  }

  /**
   * Finds the ACCOUNTS line an account is mapped to.
   *
   * @param account account
   * @return line, if mapped
   */
  public Optional<FormatLine> lineOf(AccountNode account) {
    return lines.stream().filter(l -> l.covers(account)).findFirst();
  }

  /**
   * Evaluates the statement.
   *
   * @param hierarchy chart of accounts
   * @param netDebitByAccount net base balances (debit positive) of postable accounts
   * @param rounding rounding option
   * @return line values and unmapped accounts
   */
  public Evaluation evaluate(
      AccountHierarchy hierarchy, Map<Long, BigDecimal> netDebitByAccount, Rounding rounding) {
    Map<Integer, BigDecimal> raw = new LinkedHashMap<>();
    Set<String> unmapped = new TreeSet<>();
    BigDecimal result = BigDecimal.ZERO;
    for (Map.Entry<Long, BigDecimal> e : netDebitByAccount.entrySet()) {
      AccountNode account = hierarchy.node(e.getKey());
      if (!account.accountClass().isBalanceSheet()) {
        result = result.subtract(e.getValue());
      }
      map(account, e.getValue(), raw, unmapped);
    }
    return new Evaluation(lineValues(raw, result, rounding), new ArrayList<>(unmapped));
  }

  private void map(
      AccountNode account, BigDecimal value, Map<Integer, BigDecimal> raw, Set<String> unmapped) {
    Optional<FormatLine> line = lineOf(account);
    if (line.isPresent()) {
      BigDecimal signed = value.multiply(BigDecimal.valueOf(line.get().sign()));
      raw.merge(line.get().lineNo(), signed, BigDecimal::add);
    } else if (isReportable(account, value)) {
      unmapped.add(account.code());
    }
  }

  private Map<Integer, BigDecimal> lineValues(
      Map<Integer, BigDecimal> raw, BigDecimal result, Rounding rounding) {
    Map<Integer, BigDecimal> values = new LinkedHashMap<>();
    for (FormatLine line : lines) {
      BigDecimal value =
          switch (line.type()) {
            case HEADING -> null;
            case ACCOUNTS -> rounding.apply(raw.getOrDefault(line.lineNo(), BigDecimal.ZERO));
            case RESULT -> rounding.apply(result.multiply(BigDecimal.valueOf(line.sign())));
            case TOTAL -> total(line, values);
          };
      if (value != null) {
        values.put(line.lineNo(), value);
      }
    }
    return values;
  }

  private boolean isReportable(AccountNode account, BigDecimal value) {
    boolean relevantClass =
        "BS".equals(statementType)
            ? account.accountClass().isBalanceSheet()
                && account.accountClass() != AccountClass.MEMORANDUM
            : !account.accountClass().isBalanceSheet();
    return relevantClass && value.signum() != 0;
  }

  private static BigDecimal total(FormatLine line, Map<Integer, BigDecimal> values) {
    BigDecimal sum = BigDecimal.ZERO;
    for (Integer term : line.terms()) {
      BigDecimal v = values.getOrDefault(Math.abs(term), BigDecimal.ZERO);
      sum = term < 0 ? sum.subtract(v) : sum.add(v);
    }
    return sum.multiply(BigDecimal.valueOf(line.sign()));
  }

  /**
   * Evaluated statement.
   *
   * @param values value by line number (headings omitted)
   * @param unmappedAccounts codes of accounts with a balance that no line covers
   */
  public record Evaluation(Map<Integer, BigDecimal> values, List<String> unmappedAccounts) {

    /** Canonical constructor copying the collections. */
    public Evaluation {
      values = new LinkedHashMap<>(values);
      unmappedAccounts = List.copyOf(unmappedAccounts);
    }

    /**
     * Value of a line.
     *
     * @param lineNo line number
     * @return value or null for headings
     */
    public BigDecimal value(int lineNo) {
      return values.get(lineNo);
    }
  }

  /** Rounding option of the MIS statements; totals are recomputed from rounded lines. */
  public enum Rounding {
    NONE(1),
    THOUSANDS(1_000),
    LAKHS(100_000),
    MILLIONS(1_000_000);

    private final BigDecimal divisor;

    Rounding(long divisor) {
      this.divisor = BigDecimal.valueOf(divisor);
    }

    /**
     * Rounds an amount to the unit of this option.
     *
     * @param amount amount
     * @return amount expressed in the unit (whole units unless NONE)
     */
    public BigDecimal apply(BigDecimal amount) {
      return this == NONE ? amount : amount.divide(divisor, 0, RoundingMode.HALF_UP);
    }
  }
}
