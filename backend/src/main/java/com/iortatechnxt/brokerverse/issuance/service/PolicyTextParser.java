package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.issuance.domain.ExtractionField;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reads policy data from the text of an e-policy with configurable patterns (BRNB.104): per field,
 * the rules are tried in order (the insurer's first, then the defaults) and the first capture group
 * of the first match is the value. Policy numbers collect every distinct match (multi-year,
 * BRNB.112). Account Reference Numbers printed in the document are also collected (matching).
 */
public final class PolicyTextParser {

  /** The ARN format (BRNB.102). */
  public static final Pattern ARN = Pattern.compile("ARN-\\d{4}-\\d{6}");

  private PolicyTextParser() {}

  /**
   * Parses a text.
   *
   * @param text document text
   * @param rules patterns, insurer-specific first
   * @return values found, with a note of what is missing
   */
  public static ExtractedPolicy parse(String text, List<Rule> rules) {
    List<String> notes = new ArrayList<>();
    List<String> numbers = policyNumbers(text, rules, notes);
    LocalDate from =
        first(text, rules, ExtractionField.PERIOD_FROM, notes)
            .flatMap(v -> date(v, notes))
            .orElse(null);
    LocalDate to =
        first(text, rules, ExtractionField.PERIOD_TO, notes)
            .flatMap(v -> date(v, notes))
            .orElse(null);
    BigDecimal premium =
        first(text, rules, ExtractionField.PREMIUM, notes)
            .flatMap(v -> amount(v, notes))
            .orElse(null);
    missing(numbers.isEmpty(), "policy number", notes);
    missing(from == null, "period start", notes);
    missing(to == null, "period end", notes);
    missing(premium == null, "premium", notes);
    return new ExtractedPolicy(
        numbers, from, to, premium, arns(text), notes.isEmpty() ? null : String.join("; ", notes));
  }

  /**
   * Account Reference Numbers found in a text, in order.
   *
   * @param text text (document or file name)
   * @return distinct ARNs
   */
  public static List<String> arns(String text) {
    Set<String> found = new LinkedHashSet<>();
    Matcher m = ARN.matcher(text.toUpperCase(Locale.ROOT));
    while (m.find()) {
      found.add(m.group());
    }
    return List.copyOf(found);
  }

  private static List<String> policyNumbers(String text, List<Rule> rules, List<String> notes) {
    for (Rule rule : forField(rules, ExtractionField.POLICY_NUMBER)) {
      Set<String> found = new LinkedHashSet<>();
      compile(rule, notes)
          .ifPresent(
              p -> {
                Matcher m = p.matcher(text);
                while (m.find()) {
                  found.add(value(m).toUpperCase(Locale.ROOT));
                }
              });
      if (!found.isEmpty()) {
        return List.copyOf(found);
      }
    }
    return List.of();
  }

  private static Optional<Match> first(
      String text, List<Rule> rules, ExtractionField field, List<String> notes) {
    for (Rule rule : forField(rules, field)) {
      Optional<Matcher> m = compile(rule, notes).map(p -> p.matcher(text)).filter(Matcher::find);
      if (m.isPresent()) {
        return Optional.of(new Match(value(m.get()), rule.dateFormat()));
      }
    }
    return Optional.empty();
  }

  private static List<Rule> forField(List<Rule> rules, ExtractionField field) {
    return rules.stream().filter(r -> r.field() == field).toList();
  }

  private static Optional<Pattern> compile(Rule rule, List<String> notes) {
    try {
      return Optional.of(Pattern.compile(rule.regex()));
    } catch (PatternSyntaxException e) {
      notes.add("invalid " + rule.field() + " pattern ignored");
      return Optional.empty();
    }
  }

  private static String value(Matcher m) {
    return (m.groupCount() > 0 && m.group(1) != null ? m.group(1) : m.group()).strip();
  }

  private static Optional<LocalDate> date(Match match, List<String> notes) {
    Optional<LocalDate> parsed =
        parseDate(match.value(), null)
            .or(
                () ->
                    match.dateFormat() == null
                        ? Optional.empty()
                        : parseDate(match.value(), match.dateFormat()));
    if (parsed.isEmpty()) {
      notes.add("date '" + match.value() + "' not understood");
    }
    return parsed;
  }

  private static Optional<LocalDate> parseDate(String value, String format) {
    try {
      DateTimeFormatter formatter =
          format == null
              ? DateTimeFormatter.ISO_LOCAL_DATE
              : DateTimeFormatter.ofPattern(format, Locale.ROOT);
      return Optional.of(LocalDate.parse(value, formatter));
    } catch (DateTimeParseException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static Optional<BigDecimal> amount(Match match, List<String> notes) {
    try {
      return Optional.of(new BigDecimal(match.value().replace(",", "")));
    } catch (NumberFormatException e) {
      notes.add("premium '" + match.value() + "' not understood");
      return Optional.empty();
    }
  }

  private static void missing(boolean absent, String what, List<String> notes) {
    if (absent) {
      notes.add(what + " not found");
    }
  }

  /**
   * A pattern for one field.
   *
   * @param field field
   * @param regex regular expression; group 1 is the value
   * @param dateFormat date format of a date field besides ISO, may be null
   */
  public record Rule(ExtractionField field, String regex, String dateFormat) {}

  private record Match(String value, String dateFormat) {}
}
