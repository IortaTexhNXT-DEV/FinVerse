package com.iortatechnxt.finverse.finreport.service;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import java.util.List;
import java.util.Objects;

/**
 * One line of a financial statement format.
 *
 * <p>Line types:
 *
 * <ul>
 *   <li>{@code HEADING} – caption only;
 *   <li>{@code ACCOUNTS} – Σ balances of the mapped accounts (code range, or account class and
 *       report group for the built-in STANDARD format), multiplied by {@code sign} (+1 = debit
 *       positive, -1 = credit positive);
 *   <li>{@code RESULT} – income less expenses not yet closed to retained earnings (keeps the MIS
 *       balance sheet in balance before year-end closing);
 *   <li>{@code TOTAL} – signed sum of other lines ({@code terms}: line numbers, negative =
 *       subtract).
 * </ul>
 *
 * @param lineNo line number (ordering and total references)
 * @param caption caption
 * @param type line type
 * @param scheduleRef schedule number printed on the statement, or null
 * @param accountFrom lowest mapped account code (code-range formats)
 * @param accountTo highest mapped account code (code-range formats)
 * @param accountClass mapped class (STANDARD format)
 * @param reportGroup mapped report group (STANDARD format)
 * @param sign +1 or -1
 * @param terms signed line numbers of a TOTAL line
 */
public record FormatLine(
    int lineNo,
    String caption,
    Type type,
    String scheduleRef,
    String accountFrom,
    String accountTo,
    AccountClass accountClass,
    String reportGroup,
    int sign,
    List<Integer> terms) {

  /** Canonical constructor copying the terms. */
  public FormatLine {
    terms = terms == null ? List.of() : List.copyOf(terms);
  }

  /**
   * Whether an account is mapped to this line.
   *
   * @param account account
   * @return true for ACCOUNTS lines covering the account
   */
  public boolean covers(AccountNode account) {
    if (type != Type.ACCOUNTS) {
      return false;
    }
    if (accountFrom != null) {
      return account.code().compareTo(accountFrom) >= 0 && account.code().compareTo(accountTo) <= 0;
    }
    return account.accountClass() == accountClass
        && Objects.equals(account.reportGroup(), reportGroup);
  }

  /** Statement format line types. */
  public enum Type {
    HEADING,
    ACCOUNTS,
    RESULT,
    TOTAL
  }
}
