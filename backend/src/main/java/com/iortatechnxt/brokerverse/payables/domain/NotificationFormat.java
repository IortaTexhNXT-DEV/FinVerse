package com.iortatechnxt.brokerverse.payables.domain;

/** Layout of the payment notification file expected by a bank. */
public enum NotificationFormat {
  /** Fixed-width records 01 (detail) and 02 (trailer), no delimiters. */
  FIXED_WIDTH,
  /** Comma separated values with a header line. */
  CSV,
  /**
   * Direct Credit Transaction File of BDO TPD for ACA processing (DIS 2.16.1, Appendix B p.147):
   * header with the file date and name, then per credit the 12-digit account, 30-character payee,
   * 12 spaces, the system reference and the amount {@code 000000000000.00} (full layout AQ09).
   */
  DCTF
}
