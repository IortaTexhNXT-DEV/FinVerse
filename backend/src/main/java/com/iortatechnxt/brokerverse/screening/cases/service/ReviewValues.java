package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.screening.cases.domain.CaseAnswer.AnswerValue;
import com.iortatechnxt.brokerverse.screening.config.domain.FieldDataType;
import com.iortatechnxt.brokerverse.screening.config.service.ReviewTemplate.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses and checks the values of review fields (SNSRP-501; FR-SS-050): each value is read by the
 * field's data type and a wrong value is reported next to the field ("Annual income must be a
 * number"). Pure functions.
 */
final class ReviewValues {

  /** Longest text answer. */
  static final int MAX_TEXT = 4000;

  private static final String TRUE = "TRUE";
  private static final String FALSE = "FALSE";

  /** The accepted spellings of a checkbox value. */
  private static final Map<String, String> CHECKBOX_VALUES =
      Map.of(TRUE, TRUE, "true", TRUE, "True", TRUE, FALSE, FALSE, "false", FALSE, "False", FALSE);

  private ReviewValues() {}

  /**
   * Parses a raw value.
   *
   * @param field the template field
   * @param raw the value entered, may be blank
   * @param listCodes the valid codes of the field's list (LOV fields)
   * @return the value, or the error message
   */
  static Parsed parse(Field field, String raw, Set<String> listCodes) {
    if (raw == null || raw.isBlank()) {
      return Parsed.ok(AnswerValue.BLANK);
    }
    return parseValue(field, raw.strip(), listCodes);
  }

  private static Parsed parseValue(Field field, String value, Set<String> listCodes) {
    return switch (field.dataType()) {
      case NUMBER, AMOUNT -> number(field, value);
      case DATE -> date(field, value);
      case LOV -> listValue(field, value, listCodes);
      case CHECKBOX -> checkbox(field, value);
      case ATTACHMENT -> attachment(field, value);
      default ->
          value.length() > MAX_TEXT
              ? Parsed.error(field.label() + " is limited to " + MAX_TEXT + " characters")
              : Parsed.ok(text(value));
    };
  }

  private static Parsed listValue(Field field, String value, Set<String> listCodes) {
    return listCodes.contains(value)
        ? Parsed.ok(text(value))
        : Parsed.error(field.label() + " must be a value of the list");
  }

  private static Parsed number(Field field, String value) {
    try {
      return Parsed.ok(new AnswerValue(null, new BigDecimal(value.replace(",", "")), null, null));
    } catch (NumberFormatException ex) {
      return Parsed.error(
          field.label()
              + " must be a"
              + (field.dataType() == FieldDataType.AMOUNT ? "n amount" : " number"));
    }
  }

  private static Parsed date(Field field, String value) {
    try {
      return Parsed.ok(new AnswerValue(null, null, LocalDate.parse(value), null));
    } catch (DateTimeParseException ex) {
      return Parsed.error(field.label() + " must be a date");
    }
  }

  private static Parsed checkbox(Field field, String value) {
    String flag = CHECKBOX_VALUES.get(value);
    if (flag == null) {
      return Parsed.error(field.label() + " must be ticked or not");
    }
    return Parsed.ok(text(flag));
  }

  private static Parsed attachment(Field field, String value) {
    try {
      return Parsed.ok(new AnswerValue(null, null, null, Long.valueOf(value)));
    } catch (NumberFormatException ex) {
      return Parsed.error(field.label() + " must be a document of the case");
    }
  }

  private static AnswerValue text(String value) {
    return new AnswerValue(value, null, null, null);
  }

  /**
   * Whether a mandatory field is answered (a mandatory check box must be ticked).
   *
   * @param field the field
   * @param value the answer
   * @return true when complete
   */
  static boolean complete(Field field, AnswerValue value) {
    if (!field.mandatory()) {
      return true;
    }
    if (field.dataType() == FieldDataType.CHECKBOX) {
      return TRUE.equals(value.text());
    }
    return value.filled();
  }

  /**
   * The raw form of a stored answer (shown in the form).
   *
   * @param value the answer
   * @return text, or empty
   */
  static Optional<String> raw(AnswerValue value) {
    if (value.text() != null) {
      return Optional.of(value.text());
    }
    if (value.number() != null) {
      return Optional.of(value.number().stripTrailingZeros().toPlainString());
    }
    if (value.date() != null) {
      return Optional.of(value.date().toString());
    }
    return Optional.ofNullable(value.attachmentId()).map(String::valueOf);
  }

  /**
   * A parsed value or its error.
   *
   * @param value the value, null on error
   * @param error the message, null when valid
   */
  record Parsed(AnswerValue value, String error) {

    static Parsed ok(AnswerValue value) {
      return new Parsed(value, null);
    }

    static Parsed error(String message) {
      return new Parsed(null, message);
    }
  }
}
