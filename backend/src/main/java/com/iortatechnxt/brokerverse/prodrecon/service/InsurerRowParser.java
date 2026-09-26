package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem.InsurerRow;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSide;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a row of an insurer production report in the register layout ({@link RegisterLayout},
 * PRCID.009/022): headers are matched without regard to case and spaces, dates are ISO (yyyy-MM-dd)
 * or MM/dd/yyyy, amounts may carry thousands separators, the production month is yyyy-MM (or any
 * date of the month).
 */
public final class InsurerRowParser {

  private static final List<DateTimeFormatter> DATES =
      List.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
  private static final int YEAR_MONTH_LENGTH = 7;

  private InsurerRowParser() {}

  /**
   * The insurer code of a row.
   *
   * @param row raw row
   * @return insurer code upper-cased, null when blank
   */
  public static String insurer(RawRow row) {
    String value = value(row, RegisterLayout.INSURER);
    return value == null ? null : value.toUpperCase(Locale.ROOT);
  }

  /**
   * The production month of a row.
   *
   * @param row raw row
   * @return first day of the month, null when blank or unreadable
   */
  public static LocalDate month(RawRow row) {
    String value = value(row, RegisterLayout.MONTH);
    if (value == null) {
      return null;
    }
    try {
      return YearMonth.parse(
              value.length() > YEAR_MONTH_LENGTH ? value.substring(0, YEAR_MONTH_LENGTH) : value)
          .atDay(1);
    } catch (DateTimeParseException ex) {
      LocalDate date = dateOrNull(value);
      return date == null ? null : date.withDayOfMonth(1);
    }
  }

  /**
   * The insurer line of a row.
   *
   * @param uploadId upload attempt
   * @param row raw row
   * @return insurer line
   */
  public static InsurerRow parse(Long uploadId, RawRow row) {
    String invoice = value(row, RegisterLayout.INVOICE);
    String policy = value(row, RegisterLayout.POLICY);
    if (invoice == null && policy == null) {
      throw new BusinessRuleException(
          "RECON_ROW_INCOMPLETE", "Row " + row.rowNo() + " has neither invoice nor policy number");
    }
    ReconSide side =
        new ReconSide(
            policy,
            invoice,
            value(row, RegisterLayout.PN),
            date(row, RegisterLayout.INCEPTION),
            date(row, RegisterLayout.EXPIRY),
            value(row, RegisterLayout.ASSURED),
            amount(row, RegisterLayout.COMMISSION),
            amount(row, RegisterLayout.BASIC),
            amount(row, RegisterLayout.GROSS));
    return new InsurerRow(
        uploadId,
        row.rowNo(),
        side,
        amount(row, RegisterLayout.INCENTIVE),
        value(row, RegisterLayout.REMARKS),
        false);
  }

  private static String value(RawRow row, String header) {
    for (Map.Entry<String, String> e : row.values().entrySet()) {
      if (key(e.getKey()).equals(key(header))) {
        String v = e.getValue();
        return v == null || v.isBlank() ? null : v.strip();
      }
    }
    return null;
  }

  /**
   * A header without case, spaces or dots.
   *
   * @param header header
   * @return comparison key
   */
  static String key(String header) {
    return header.replaceAll("[\\s.]", "").toLowerCase(Locale.ROOT);
  }

  private static LocalDate date(RawRow row, String header) {
    String v = value(row, header);
    if (v == null) {
      return null;
    }
    LocalDate date = dateOrNull(v);
    if (date == null) {
      throw new BusinessRuleException(
          "RECON_ROW_DATE", "Row " + row.rowNo() + ": " + header + " '" + v + "' is not a date");
    }
    return date;
  }

  private static LocalDate dateOrNull(String v) {
    for (DateTimeFormatter f : DATES) {
      LocalDate date = parseOrNull(v, f);
      if (date != null) {
        return date;
      }
    }
    return null;
  }

  private static LocalDate parseOrNull(String v, DateTimeFormatter format) {
    try {
      return LocalDate.parse(v, format);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private static BigDecimal amount(RawRow row, String header) {
    String v = value(row, header);
    if (v == null) {
      return null;
    }
    try {
      return new BigDecimal(v.replace(",", ""));
    } catch (NumberFormatException ex) {
      throw new BusinessRuleException(
          "RECON_ROW_AMOUNT",
          "Row " + row.rowNo() + ": " + header + " '" + v + "' is not an amount",
          ex);
    }
  }
}
