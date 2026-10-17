package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Numbers of the creation, cancellation and reinstatement records (FRS.CSH.02.01.09; Appendix R,
 * C3): {@code <Prefix>-<Receipt Type>-<Sequence>} by default, one sequence per prefix and receipt
 * type that never gives a number twice, even to replace a cancelled record. The format is the
 * setting {@code CASH_RECORD_NO_FORMAT}; with the token {YEAR} the sequence restarts every year.
 */
@Component
public class RecordNumbers {

  private static final String YEAR = "{YEAR}";

  private final DocumentNumberService numbers;
  private final CashieringDecisions decisions;
  private final Clock clock;

  /**
   * Creates the numbering.
   *
   * @param numbers document sequences
   * @param decisions settings
   * @param clock clock
   */
  public RecordNumbers(DocumentNumberService numbers, CashieringDecisions decisions, Clock clock) {
    this.numbers = numbers;
    this.decisions = decisions;
    this.clock = clock;
  }

  /**
   * The next number of a record.
   *
   * @param kind creation, cancellation or reinstatement
   * @param receipt AR or OR
   * @return for example CR-AR-000001
   */
  public String next(RecordKind kind, ReceiptKind receipt) {
    String format = decisions.recordNumberFormat();
    String year = String.valueOf(BusinessClock.today(clock).getYear());
    String key = kind.prefix() + "-" + receipt.name() + (format.contains(YEAR) ? "-" + year : "");
    String allocated = numbers.next(key);
    String sequence = allocated.substring(allocated.lastIndexOf('-') + 1);
    return format(format, kind, receipt, year, sequence);
  }

  /**
   * Fills a record number format.
   *
   * @param format format
   * @param kind record kind
   * @param receipt AR or OR
   * @param year year
   * @param sequence zero-filled sequence
   * @return number
   */
  static String format(
      String format, RecordKind kind, ReceiptKind receipt, String year, String sequence) {
    return format
        .replace("{PREFIX}", kind.prefix())
        .replace("{TYPE}", receipt.name())
        .replace(YEAR, year)
        .replace("{SEQ}", sequence);
  }
}
