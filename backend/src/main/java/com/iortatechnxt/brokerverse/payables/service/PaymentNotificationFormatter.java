package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.payables.domain.NotificationFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Writes the payment notification file sent to the bank (FIN-BRS-PAYNOTIFY, positive pay). The
 * source book does not document the layout, so BrokerVerse uses the layouts below; each bank
 * account chooses one ({@link NotificationFormat}).
 *
 * <p><b>FIXED_WIDTH</b> (no delimiters, CRLF line ends, text left-aligned and space padded, numbers
 * right-aligned and zero padded, amounts in minor units without decimal point):
 *
 * <pre>
 * Record 01 - payment detail (163 characters)
 *   pos   1-2    2  record type "01"
 *   pos   3-22  20  company bank account number
 *   pos  23-42  20  payment voucher number
 *   pos  43-62  20  cheque number (blank for transfers)
 *   pos  63-70   8  payment date DDMMYYYY
 *   pos  71-85  15  vendor code
 *   pos  86-125 40  vendor name
 *   pos 126-145 20  vendor bank account number
 *   pos 146-148  3  currency
 *   pos 149-163 15  amount in minor units
 * Record 02 - trailer (54 characters)
 *   pos   1-2    2  record type "02"
 *   pos   3-22  20  company bank account number
 *   pos  23-30   8  file date DDMMYYYY
 *   pos  31-36   6  number of 01 records
 *   pos  37-54  18  total amount in minor units
 * </pre>
 *
 * <p><b>CSV</b>: header line, one line per payment with the same fields (amount with a decimal
 * point, date ISO yyyy-MM-dd), then a trailer line {@code 02,<account>,<file
 * date>,<count>,<total>}.
 */
public final class PaymentNotificationFormatter {

  private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("ddMMyyyy");
  private static final String CRLF = "\r\n";
  private static final int ACCOUNT = 20;
  private static final int DOCUMENT = 20;
  private static final int VENDOR_CODE = 15;
  private static final int VENDOR_NAME = 40;
  private static final int CURRENCY = 3;
  private static final int AMOUNT = 15;
  private static final int COUNT = 6;
  private static final int TOTAL = 18;
  private static final String CSV_HEADER =
      "record_type,company_account,voucher_no,cheque_no,payment_date,vendor_code,vendor_name,"
          + "vendor_account,currency,amount";

  private PaymentNotificationFormatter() {}

  /**
   * Formats the file.
   *
   * @param format layout
   * @param companyAccountNo company bank account number
   * @param fileDate file date
   * @param records payments
   * @return file text
   */
  public static String format(
      NotificationFormat format,
      String companyAccountNo,
      LocalDate fileDate,
      List<NotificationRecord> records) {
    return format == NotificationFormat.CSV
        ? csv(companyAccountNo, fileDate, records)
        : fixedWidth(companyAccountNo, fileDate, records);
  }

  private static String fixedWidth(
      String account, LocalDate fileDate, List<NotificationRecord> records) {
    StringBuilder out = new StringBuilder();
    for (NotificationRecord r : records) {
      out.append("01")
          .append(text(account, ACCOUNT))
          .append(text(r.voucherNo(), DOCUMENT))
          .append(text(r.chequeNo(), DOCUMENT))
          .append(r.paymentDate().format(DDMMYYYY))
          .append(text(r.vendorCode(), VENDOR_CODE))
          .append(text(r.vendorName(), VENDOR_NAME))
          .append(text(r.vendorAccountNo(), ACCOUNT))
          .append(text(r.currency(), CURRENCY))
          .append(number(minorUnits(r.amount()), AMOUNT))
          .append(CRLF);
    }
    out.append("02")
        .append(text(account, ACCOUNT))
        .append(fileDate.format(DDMMYYYY))
        .append(number(records.size(), COUNT))
        .append(number(minorUnits(total(records)), TOTAL))
        .append(CRLF);
    return out.toString();
  }

  private static String csv(String account, LocalDate fileDate, List<NotificationRecord> records) {
    StringBuilder out = new StringBuilder(CSV_HEADER).append(CRLF);
    for (NotificationRecord r : records) {
      out.append(
              String.join(
                  ",",
                  "01",
                  quote(account),
                  quote(r.voucherNo()),
                  quote(r.chequeNo()),
                  r.paymentDate().toString(),
                  quote(r.vendorCode()),
                  quote(r.vendorName()),
                  quote(r.vendorAccountNo()),
                  quote(r.currency()),
                  r.amount().setScale(2, RoundingMode.HALF_EVEN).toPlainString()))
          .append(CRLF);
    }
    out.append(
            String.join(
                ",",
                "02",
                quote(account),
                fileDate.toString(),
                String.valueOf(records.size()),
                total(records).setScale(2, RoundingMode.HALF_EVEN).toPlainString()))
        .append(CRLF);
    return out.toString();
  }

  /**
   * Total amount of the records.
   *
   * @param records records
   * @return total
   */
  public static BigDecimal total(List<NotificationRecord> records) {
    return records.stream()
        .map(NotificationRecord::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Left-aligned, space padded, truncated field; non-printable and non-ASCII characters become
   * spaces so the bank's parser never shifts columns.
   *
   * @param value value (null = blank)
   * @param width width
   * @return field
   */
  static String text(String value, int width) {
    String clean =
        value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^\\x20-\\x7E]", " ");
    String cut = clean.length() > width ? clean.substring(0, width) : clean;
    return cut + " ".repeat(width - cut.length());
  }

  /**
   * Right-aligned, zero padded number.
   *
   * @param value non-negative value
   * @param width width
   * @return field
   */
  static String number(long value, int width) {
    String digits = String.valueOf(value);
    if (digits.length() > width) {
      throw new IllegalArgumentException(
          "Value " + value + " does not fit in " + width + " digits");
    }
    return "0".repeat(width - digits.length()) + digits;
  }

  private static long minorUnits(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_EVEN).movePointRight(2).longValueExact();
  }

  private static String quote(String value) {
    String v = value == null ? "" : value;
    return v.contains(",") || v.contains("\"") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
  }
}
