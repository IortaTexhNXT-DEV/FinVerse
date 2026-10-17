package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.SoaLayout;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLine;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLine.SoaValues;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Reads the rows of an insurer SOA with a layout (ACSL 2.4.0): the invoice or policy number is
 * mandatory, amounts may carry thousands separators and accounting negatives "(1,000.00)", dates
 * are ISO (yyyy-MM-dd) or MM/dd/yyyy (the file naming of ACSL 2.14.1). A row that cannot be read is
 * kept as failed with the reason, so the upload log accounts for every row.
 */
@Component
public class SoaRowParser {

  private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final int MAX_TEXT = 250;
  private static final int MAX_KEY = 40;

  /**
   * Refuses a file that does not have the layout's key columns.
   *
   * @param layout layout
   * @param headers headers of the file
   */
  public void requireHeaders(SoaLayout layout, List<String> headers) {
    List<String> missing =
        Stream.of(layout.getInvoiceHeader(), layout.getBalanceHeader())
            .filter(h -> !headers.contains(h))
            .toList();
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "ACSL_SOA_LAYOUT_MISMATCH",
          "The file does not follow the layout '"
              + layout.getName()
              + "'; missing column(s): "
              + String.join(", ", missing));
    }
  }

  /**
   * Reads one row.
   *
   * @param uploadId upload
   * @param layout layout
   * @param row raw row
   * @return a loaded or failed line
   */
  public SoaLine parse(Long uploadId, SoaLayout layout, ParsedFile.RawRow row) {
    Map<String, String> v = row.values();
    String invoice = text(v.get(layout.getInvoiceHeader()), MAX_KEY);
    String policy = text(v.get(layout.getPolicyHeader()), MAX_KEY);
    if (invoice == null && policy == null) {
      return SoaLine.failed(uploadId, row.rowNo(), null, "No invoice or policy number");
    }
    try {
      return SoaLine.loaded(
          uploadId,
          row.rowNo(),
          new SoaValues(
              invoice,
              policy,
              text(v.get(layout.getAssuredHeader()), MAX_TEXT),
              date(v.get(layout.getInceptionHeader())),
              date(v.get(layout.getExpiryHeader())),
              amount(v.get(layout.getGrossHeader())),
              amount(v.get(layout.getBalanceHeader())),
              amount(v.get(layout.getPaidHeader()))));
    } catch (NumberFormatException | DateTimeParseException e) {
      return SoaLine.failed(uploadId, row.rowNo(), invoice, "Unreadable value: " + e.getMessage());
    }
  }

  /**
   * An amount as written by the insurer.
   *
   * @param raw raw text
   * @return amount at scale 2, or null when blank
   */
  public static BigDecimal amount(String raw) {
    String text = raw == null ? "" : raw.strip().replace(",", "");
    if (text.isEmpty()) {
      return null;
    }
    boolean negative = text.startsWith("(") && text.endsWith(")");
    String digits = negative ? text.substring(1, text.length() - 1) : text;
    BigDecimal value = new BigDecimal(digits).setScale(2, RoundingMode.HALF_EVEN);
    return negative ? value.negate() : value;
  }

  /**
   * A date as written by the insurer.
   *
   * @param raw raw text
   * @return date, or null when blank
   */
  public static LocalDate date(String raw) {
    String text = raw == null ? "" : raw.strip();
    if (text.isEmpty()) {
      return null;
    }
    return text.contains("/") ? LocalDate.parse(text, US_DATE) : LocalDate.parse(text);
  }

  private static String text(String raw, int max) {
    String text = raw == null ? "" : raw.strip();
    if (text.isEmpty()) {
      return null;
    }
    return text.length() > max ? text.substring(0, max) : text;
  }
}
