package com.iortatechnxt.finverse.payables.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * An open item of a creditor as of a date, with its balance after the matches dated up to that date
 * (rule R-OPENITEM).
 *
 * @param id open item id
 * @param partyCode party (sub account)
 * @param partyName party name
 * @param partyType party type
 * @param phone phone
 * @param address address
 * @param mainAccount control (main) account the item was posted to
 * @param credit true for payables (CREDIT items), false for payments / advances on account
 * @param documentType document type
 * @param documentNo document number
 * @param documentDate document date
 * @param dueDate due date
 * @param currency currency
 * @param amount original amount (item currency)
 * @param balance balance as of the date (item currency)
 * @param baseBalance balance in base currency (pro-rated historical rate, rule R-FX)
 * @param narration narration
 */
public record CreditorItem(
    Long id,
    String partyCode,
    String partyName,
    String partyType,
    String phone,
    String address,
    String mainAccount,
    boolean credit,
    String documentType,
    String documentNo,
    LocalDate documentDate,
    LocalDate dueDate,
    String currency,
    BigDecimal amount,
    BigDecimal balance,
    BigDecimal baseBalance,
    String narration) {

  /**
   * Age in days on the chosen basis.
   *
   * @param asOf ageing date
   * @param byDueDate true = due date basis, false = document date basis
   * @return days (negative when not yet due)
   */
  public long age(LocalDate asOf, boolean byDueDate) {
    return ChronoUnit.DAYS.between(byDueDate ? dueDate : documentDate, asOf);
  }

  /**
   * Signed balance from the creditor's point of view: payables positive, amounts on account
   * negative.
   *
   * @param base base currency (true) or item currency (false)
   * @return signed balance
   */
  public BigDecimal signed(boolean base) {
    BigDecimal value = base ? baseBalance : balance;
    return credit ? value : value.negate();
  }
}
