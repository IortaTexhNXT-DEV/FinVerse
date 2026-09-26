package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportKind;
import com.iortatechnxt.brokerverse.placement.domain.ReportedPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the rows of a payment report (BRNB.067/068). The CLPC layout is parked (Q28), so the
 * columns are recognised by name, ignoring case, spaces and punctuation:
 *
 * <ul>
 *   <li>reference: {@code PN No.} or {@code Loan Application No.} (CLPC), {@code ARN} or {@code
 *       Reference} (other segments);
 *   <li>{@code Status} (PAID / UNPAID, Y / N); every row is paid when the column is missing;
 *   <li>{@code Amount} and {@code Payment Date}, optional.
 * </ul>
 */
public final class PaymentReportParser {

  private static final List<String> CLPC_REFERENCES =
      List.of("pnno", "pnnumber", "pn", "loanapplicationno", "loanappno", "loanapplicationnumber");
  private static final List<String> ARN_REFERENCES =
      List.of("arn", "reference", "referenceno", "accountreferencenumber");
  private static final List<String> STATUS = List.of("status", "paymentstatus", "paid");
  private static final List<String> AMOUNT = List.of("amount", "amountpaid", "premiumpaid");
  private static final List<String> DATE = List.of("paymentdate", "datepaid", "paidon", "date");
  private static final Set<String> UNPAID = Set.of("UNPAID", "N", "NO", "FALSE", "0", "NOT PAID");

  private PaymentReportParser() {}

  /**
   * Reads the reported payments.
   *
   * @param file parsed file
   * @param kind report kind (decides the reference columns)
   * @return rows with a reference
   */
  public static List<ReportedPayment> read(ParsedFile file, PaymentReportKind kind) {
    List<String> referenceKeys = kind == PaymentReportKind.CLPC ? CLPC_REFERENCES : ARN_REFERENCES;
    List<String> referenceColumns = columns(file.headers(), referenceKeys);
    if (referenceColumns.isEmpty()) {
      throw new BusinessRuleException(
          "PAYMENT_REPORT_LAYOUT",
          kind == PaymentReportKind.CLPC
              ? "The CLPC report needs a 'PN No.' or 'Loan Application No.' column"
              : "The payment report needs an 'ARN' or 'Reference' column");
    }
    Optional<String> status = columns(file.headers(), STATUS).stream().findFirst();
    Optional<String> amount = columns(file.headers(), AMOUNT).stream().findFirst();
    Optional<String> date = columns(file.headers(), DATE).stream().findFirst();
    List<ReportedPayment> rows = new ArrayList<>();
    for (RawRow row : file.rows()) {
      firstValue(row.values(), referenceColumns)
          .ifPresent(
              reference ->
                  rows.add(
                      new ReportedPayment(
                          row.rowNo(),
                          reference.toUpperCase(Locale.ROOT),
                          status.map(c -> paid(row.values().get(c))).orElse(true),
                          amount.map(c -> amount(row, c)).orElse(null),
                          date.map(c -> date(row, c)).orElse(null))));
    }
    if (rows.isEmpty()) {
      throw new BusinessRuleException("PAYMENT_REPORT_EMPTY", "The payment report has no rows");
    }
    return rows;
  }

  /**
   * Normalised header: lower case letters and digits only.
   *
   * @param header header
   * @return key
   */
  static String key(String header) {
    return header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  private static List<String> columns(List<String> headers, List<String> keys) {
    List<String> found = new ArrayList<>();
    for (String wanted : keys) {
      headers.stream().filter(h -> key(h).equals(wanted)).findFirst().ifPresent(found::add);
    }
    return found;
  }

  private static Optional<String> firstValue(Map<String, String> values, List<String> columns) {
    return columns.stream()
        .map(values::get)
        .filter(v -> v != null && !v.isBlank())
        .map(String::strip)
        .findFirst();
  }

  private static boolean paid(String value) {
    return value == null || !UNPAID.contains(value.strip().toUpperCase(Locale.ROOT));
  }

  private static BigDecimal amount(RawRow row, String column) {
    String value = row.values().get(column);
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value.replace(",", "").strip());
    } catch (NumberFormatException e) {
      throw new BusinessRuleException(
          "PAYMENT_REPORT_VALUE", "Row " + row.rowNo() + ": '" + value + "' is not an amount", e);
    }
  }

  private static LocalDate date(RawRow row, String column) {
    String value = row.values().get(column);
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.strip());
    } catch (DateTimeParseException e) {
      throw new BusinessRuleException(
          "PAYMENT_REPORT_VALUE",
          "Row " + row.rowNo() + ": '" + value + "' is not a date (yyyy-mm-dd)",
          e);
    }
  }
}
