package com.iortatechnxt.brokerverse.receivables.domain;

import java.math.BigDecimal;

/**
 * Bank Reconciliation Statement arithmetic (book-side signs: a debit in the bank GL account is
 * money in the bank).
 *
 * <pre>
 * Balance per bank = Book balance
 *                    - (1) book debits not accounted by the bank   (deposits in transit)
 *                    + (2) book credits not accounted by the bank  (unpresented cheques)
 *                    - (3) bank debits not accounted in the book   (charges, returned cheques)
 *                    + (4) bank credits not accounted in the book  (interest, direct credits)
 * Difference = balance per bank statement - computed balance per bank (must be zero)
 * </pre>
 *
 * @param bookBalance GL balance of the bank account (debit positive)
 * @param bookDebitsNotInBank (1) as a positive amount
 * @param bookCreditsNotInBank (2) as a positive amount
 * @param bankDebitsNotInBook (3) as a positive amount
 * @param bankCreditsNotInBook (4) as a positive amount
 * @param computedBankBalance balance per bank derived from the book
 * @param statementBalance balance per imported bank statement
 * @param difference unexplained difference
 */
public record BrsFigures(
    BigDecimal bookBalance,
    BigDecimal bookDebitsNotInBank,
    BigDecimal bookCreditsNotInBank,
    BigDecimal bankDebitsNotInBook,
    BigDecimal bankCreditsNotInBook,
    BigDecimal computedBankBalance,
    BigDecimal statementBalance,
    BigDecimal difference) {

  /**
   * Computes the derived figures.
   *
   * @param bookBalance book balance
   * @param bookDebits (1)
   * @param bookCredits (2)
   * @param bankDebits (3)
   * @param bankCredits (4)
   * @param statementBalance statement balance
   * @return figures
   */
  public static BrsFigures of(
      BigDecimal bookBalance,
      BigDecimal bookDebits,
      BigDecimal bookCredits,
      BigDecimal bankDebits,
      BigDecimal bankCredits,
      BigDecimal statementBalance) {
    BigDecimal computed =
        bookBalance.subtract(bookDebits).add(bookCredits).subtract(bankDebits).add(bankCredits);
    return new BrsFigures(
        bookBalance,
        bookDebits,
        bookCredits,
        bankDebits,
        bankCredits,
        computed,
        statementBalance,
        statementBalance.subtract(computed));
  }
}
