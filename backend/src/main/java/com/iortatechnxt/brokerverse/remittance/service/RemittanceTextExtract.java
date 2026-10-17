package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine.MallAssurance;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The pipe-delimited text extract of a batch (ICD IF-RMT-02): an H record with the control totals
 * (number of D records and the sums of their amount fields), then one D record per account.
 */
final class RemittanceTextExtract {

  /** Field delimiter. */
  static final String DELIMITER = "|";

  private static final int REFERENCE_DIGITS = 10;
  private static final String CRLF = "\r\n";
  private static final DateTimeFormatter DETAIL_DATE =
      DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.ENGLISH);

  private RemittanceTextExtract() {}

  /**
   * The text extract (IF-RMT-02): H record with the control totals, then one D record per account.
   *
   * @param batch batch with lines
   * @param extractedOn date of the extraction
   * @return UTF-8 bytes with a byte order mark, lines ended by CR LF
   */
  static byte[] write(RemittanceBatch batch, LocalDate extractedOn) {
    List<BatchLine> lines = batch.included();
    BigDecimal[] sums = new BigDecimal[TextLayout.SUMMED];
    Arrays.fill(sums, BigDecimal.ZERO);
    List<String> details = new ArrayList<>();
    for (BatchLine l : lines) {
      List<BigDecimal> amounts = TextLayout.amounts(l);
      for (int i = 0; i < amounts.size(); i++) {
        sums[i] = sums[i].add(amounts.get(i));
      }
      details.add(detail(l, amounts));
    }
    List<String> header = new ArrayList<>();
    header.add("H");
    header.add("M");
    header.add(extractedOn.format(DateTimeFormatter.BASIC_ISO_DATE));
    header.add("01");
    header.add(String.valueOf(lines.size()));
    for (int i = 0; i < sums.length; i++) {
      header.add(TextLayout.format(i, sums[i]));
    }
    StringBuilder sb = new StringBuilder("﻿");
    sb.append(String.join(DELIMITER, header)).append(CRLF);
    details.forEach(d -> sb.append(d).append(CRLF));
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static String detail(BatchLine l, List<BigDecimal> amounts) {
    List<String> f = new ArrayList<>();
    f.add("D");
    f.add(l.getInvoiceNo());
    f.add(text(l.getPolicyNo()));
    f.add(text(l.getEndorsementNo()));
    f.add(padded(l.getInvoiceNo()));
    f.add(text(l.getAssuredName()));
    f.add(text(l.getProductLine()));
    f.add("");
    f.add(detailDate(l.getInceptionDate()));
    f.add(detailDate(l.getExpiryDate()));
    for (int i = 0; i < amounts.size(); i++) {
      f.add(TextLayout.format(i, amounts.get(i)));
    }
    f.add("N");
    f.add(detailDate(l.getInsurerOrDate()));
    f.add(text(l.getInsurerOrNo()));
    f.add(l.getInsurerOrAmount() == null ? "" : l.getInsurerOrAmount().toPlainString());
    f.add("");
    f.add("N");
    return String.join(DELIMITER, f);
  }

  /** Amount fields 11 to 21 of the D record and their decimals (header fields 6 to 16). */
  static final class TextLayout {

    /** Number of summed amount fields. */
    static final int SUMMED = 11;

    private static final int[] SCALE = {2, 2, 2, 4, 2, 4, 2, 2, 2, 2, 2};

    private TextLayout() {}

    static List<BigDecimal> amounts(BatchLine l) {
      RemittanceAmounts a = l.getAmounts();
      MallAssurance m = l.getMallAssurance();
      return List.of(
          a.paidAr(),
          a.commission(),
          a.commissionVat(),
          a.wtax(),
          a.dtip(),
          a.netDue(),
          BigDecimal.ZERO,
          m.vat(),
          m.wtax(),
          m.totalDeductions(),
          RemittanceExtractFiles.amountDue(l));
    }

    static String format(int index, BigDecimal value) {
      return value.setScale(SCALE[index], RoundingMode.HALF_UP).toPlainString();
    }
  }

  private static String padded(String invoiceNo) {
    String digits = invoiceNo.replaceAll("\\D", "");
    return digits.length() >= REFERENCE_DIGITS
        ? digits
        : "0".repeat(REFERENCE_DIGITS - digits.length()) + digits;
  }

  private static String text(String value) {
    return value == null ? "" : value.replace(DELIMITER, " ");
  }

  private static String detailDate(LocalDate value) {
    return value == null ? "" : value.format(DETAIL_DATE);
  }
}
