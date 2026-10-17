package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The one-line descriptions of the records on the client's Records tab, as users read them: names
 * instead of codes, amounts with their currency, thousands separators and two decimals ("PHP
 * 1,234.50"), dates as dd-MMM-yyyy and sentence-case wording ("Not rated").
 */
public final class RecordDescriptions {

  private static final String SEPARATOR = " - ";

  private RecordDescriptions() {}

  /**
   * A quotation: "ARN-2026-000001 - Motor Comprehensive - Gross premium PHP 1,234.50".
   *
   * @param arn account reference
   * @param product product name
   * @param currency currency code, may be null
   * @param gross gross premium, null when not rated yet
   * @return description
   */
  public static String quotation(String arn, String product, String currency, BigDecimal gross) {
    String premium = gross == null ? "Not rated" : "Gross premium " + money(currency, gross);
    return join(arn, product, premium);
  }

  /**
   * A proposal request: "ARN-2026-000001 - Fire - Mabuhay General Insurance".
   *
   * @param arn account reference
   * @param product product name
   * @param insurer chosen insurer's name, null while not chosen
   * @return description
   */
  public static String proposal(String arn, String product, String insurer) {
    return join(arn, product, blank(insurer) ? "Insurer to be chosen" : insurer);
  }

  /**
   * An account: "Motor Comprehensive - Mabuhay General Insurance - Sum insured PHP 1,000,000.00".
   *
   * @param product product name
   * @param insurer insurer's name, null while not selected
   * @param currency currency code, may be null
   * @param sumInsured total sum insured, may be null
   * @return description
   */
  public static String account(
      String product, String insurer, String currency, BigDecimal sumInsured) {
    String insurerText = blank(insurer) ? "Insurer to be selected" : insurer;
    if (sumInsured == null) {
      return join(product, insurerText);
    }
    return join(product, insurerText, "Sum insured " + money(currency, sumInsured));
  }

  /**
   * A booked invoice: "Debit Note - ARN-2026-000001 - Mabuhay General Insurance - Gross premium PHP
   * 12,000.00".
   *
   * @param kind invoice kind code
   * @param arn account reference
   * @param insurer insurer's name
   * @param currency currency code, may be null
   * @param gross gross premium
   * @return description
   */
  public static String bookedInvoice(
      Object kind, String arn, String insurer, String currency, BigDecimal gross) {
    return join(DisplayFormat.label(kind), arn, insurer, "Gross premium " + money(currency, gross));
  }

  /**
   * A claim: "ARN-2026-000001 - Motor Comprehensive - Loss on 05-Sep-2026".
   *
   * @param arn account reference
   * @param product product name
   * @param lossDate date of loss, may be null
   * @return description
   */
  public static String claim(String arn, String product, LocalDate lossDate) {
    return lossDate == null
        ? join(arn, product)
        : join(arn, product, "Loss on " + DisplayFormat.date(lossDate));
  }

  /**
   * A renewal: "POL-123 - Expires 05-Sep-2026".
   *
   * @param policy expiring policy number or ARN
   * @param expiry expiry date
   * @return description
   */
  public static String renewal(String policy, LocalDate expiry) {
    return join(policy, "Expires " + DisplayFormat.date(expiry));
  }

  /**
   * An insurance advice: "ARN-2026-000001 - Mortgagee" and the bank's name.
   *
   * @param arn account reference
   * @param mortgagee mortgagee bank's name, may be null
   * @return description
   */
  public static String insuranceAdvice(String arn, String mortgagee) {
    return blank(mortgagee) ? arn : join(arn, "Mortgagee " + mortgagee);
  }

  /**
   * An amount with its currency: "PHP 1,234.50"; the amount alone when the currency is unknown.
   *
   * @param currency currency code, may be null
   * @param amount amount, may be null
   * @return text, empty when the amount is null
   */
  public static String money(String currency, BigDecimal amount) {
    String figure = DisplayFormat.amount(amount);
    return blank(currency) || figure.isEmpty() ? figure : currency + " " + figure;
  }

  private static boolean blank(String text) {
    return text == null || text.isBlank();
  }

  private static String join(String... parts) {
    StringBuilder out = new StringBuilder();
    for (String part : parts) {
      if (blank(part)) {
        continue;
      }
      if (!out.isEmpty()) {
        out.append(SEPARATOR);
      }
      out.append(part);
    }
    return out.toString();
  }
}
