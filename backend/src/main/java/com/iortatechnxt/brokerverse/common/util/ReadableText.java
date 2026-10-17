package com.iortatechnxt.brokerverse.common.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-built sentences (alert messages, notifications) as users read them: ISO dates and times
 * become dd-MMM-yyyy (HH:mm), plain amounts get thousands separators, status codes written in
 * capitals with underscores become words ("COMPLIANCE_REVIEW" becomes "compliance review") and "1
 * invoice(s)" becomes "1 invoice". References such as PRF-2026-000001 or file names are left as
 * they are.
 */
public final class ReadableText {

  private static final Pattern INSTANT =
      Pattern.compile("(?<![\\w-])\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?::\\d{2})?(?:\\.\\d{1,9})?Z");
  private static final Pattern DATE = Pattern.compile("(?<![\\w-])\\d{4}-\\d{2}-\\d{2}(?![\\w-])");
  private static final Pattern AMOUNT = Pattern.compile("(?<![\\w.,-])-?\\d{4,15}\\.\\d{2}(?!\\w)");
  private static final Pattern CODE =
      Pattern.compile("(?<![\\w.-])[A-Z]{2,20}(?:_[A-Z0-9]{1,20}){1,6}(?![\\w-]|\\.\\w)");
  private static final Pattern COUNTED =
      Pattern.compile("(?<![\\w.,])(\\d{1,9}) ([a-z]{2,30})\\(s\\)");

  private ReadableText() {}

  /**
   * The sentence with dates, amounts and status codes written for users.
   *
   * @param text sentence, may be null
   * @return readable sentence, null when null
   */
  public static String of(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    String out = replace(INSTANT, text, ReadableText::instant);
    out = replace(DATE, out, ReadableText::date);
    out = replace(AMOUNT, out, a -> DisplayFormat.amount(new BigDecimal(a)));
    out = replace(COUNTED, out, ReadableText::counted);
    return replace(CODE, out, DisplayFormat::words);
  }

  private static String instant(String iso) {
    try {
      // A time without seconds ("2026-10-08T18:46Z") gets them for the parser.
      String full = iso.indexOf(':') == iso.lastIndexOf(':') ? iso.replace("Z", ":00Z") : iso;
      return DisplayFormat.dateTime(Instant.parse(full));
    } catch (DateTimeParseException e) {
      return iso;
    }
  }

  private static String counted(String text) {
    int space = text.indexOf(' ');
    String noun = text.substring(space + 1, text.length() - "(s)".length());
    return DisplayFormat.countOf(Long.parseLong(text.substring(0, space)), noun);
  }

  private static String date(String iso) {
    try {
      return DisplayFormat.date(LocalDate.parse(iso));
    } catch (DateTimeParseException e) {
      return iso;
    }
  }

  private static String replace(Pattern pattern, String text, Function<String, String> rewrite) {
    Matcher m = pattern.matcher(text);
    StringBuilder out = new StringBuilder();
    while (m.find()) {
      m.appendReplacement(out, Matcher.quoteReplacement(rewrite.apply(m.group())));
    }
    m.appendTail(out);
    return out.toString();
  }
}
