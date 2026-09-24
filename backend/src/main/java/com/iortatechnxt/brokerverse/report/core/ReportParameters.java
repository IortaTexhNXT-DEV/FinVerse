package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Validated, typed access to report parameter values.
 *
 * <p>Default keywords for dates: {@code TODAY}, {@code MONTH_START}, {@code YEAR_START}.
 */
public final class ReportParameters {

  private static final String TODAY = "TODAY";
  private static final String MONTH_START = "MONTH_START";
  private static final String YEAR_START = "YEAR_START";

  private final ReportMetadata metadata;
  private final Map<String, String> values;
  private final ParameterDisplay display;

  private ReportParameters(
      ReportMetadata metadata, Map<String, String> values, ParameterDisplay display) {
    this.metadata = metadata;
    this.values = Map.copyOf(values);
    this.display = display;
  }

  /**
   * Validates parameters whose echo shows the raw values (see {@link #validate(ReportMetadata, Map,
   * Clock, ParameterDisplay)}).
   *
   * @param metadata report metadata
   * @param raw raw values from the request
   * @param clock clock for date keywords
   * @return validated parameters
   */
  public static ReportParameters validate(
      ReportMetadata metadata, Map<String, String> raw, Clock clock) {
    return validate(metadata, raw, clock, ParameterDisplay.RAW);
  }

  /**
   * Applies defaults and validates raw values against the report's parameter specs.
   *
   * <p>Defaults apply to parameters the request leaves out. A required parameter sent blank (the
   * user cleared the field) is an error rather than silently replaced by its default, and a range
   * whose To value is before its From value is rejected (see {@link ParameterRanges}).
   *
   * @param metadata report metadata
   * @param raw raw values from the request
   * @param clock clock for date keywords
   * @param display renders values in the echo (company and branch ids as code and name)
   * @return validated parameters
   */
  public static ReportParameters validate(
      ReportMetadata metadata, Map<String, String> raw, Clock clock, ParameterDisplay display) {
    Map<String, String> resolved = new HashMap<>();
    List<String> errors = new ArrayList<>();
    for (ParameterSpec spec : metadata.parameters()) {
      String value = resolve(spec, raw, clock);
      if (value == null) {
        if (spec.required()) {
          errors.add(spec.label() + " is required");
        }
        continue;
      }
      checkType(spec, value, errors);
      resolved.put(spec.name(), value);
    }
    ParameterRanges.check(metadata.parameters(), resolved, errors);
    if (!errors.isEmpty()) {
      throw new BusinessRuleException("INVALID_REPORT_PARAMETERS", String.join("; ", errors));
    }
    return new ReportParameters(metadata, resolved, display);
  }

  /**
   * Returns a mandatory date.
   *
   * @param name parameter
   * @return date
   */
  public LocalDate date(String name) {
    return optionalDate(name).orElseThrow(() -> missing(name));
  }

  /**
   * Returns an optional date.
   *
   * @param name parameter
   * @return date if supplied
   */
  public Optional<LocalDate> optionalDate(String name) {
    return optionalText(name).map(LocalDate::parse);
  }

  /**
   * Returns a mandatory long (ids).
   *
   * @param name parameter
   * @return value
   */
  public long longValue(String name) {
    return optionalLong(name).orElseThrow(() -> missing(name));
  }

  /**
   * Returns an optional long.
   *
   * @param name parameter
   * @return value if supplied
   */
  public Optional<Long> optionalLong(String name) {
    return optionalText(name).map(Long::valueOf);
  }

  /**
   * Returns an optional decimal.
   *
   * @param name parameter
   * @return value if supplied
   */
  public Optional<BigDecimal> optionalDecimal(String name) {
    return optionalText(name).map(BigDecimal::new);
  }

  /**
   * Returns a mandatory text value.
   *
   * @param name parameter
   * @return value
   */
  public String text(String name) {
    return optionalText(name).orElseThrow(() -> missing(name));
  }

  /**
   * Returns an optional text value.
   *
   * @param name parameter
   * @return value if supplied
   */
  public Optional<String> optionalText(String name) {
    return Optional.ofNullable(values.get(name));
  }

  /**
   * Returns a boolean flag (false when absent).
   *
   * @param name parameter
   * @return flag
   */
  public boolean flag(String name) {
    return optionalText(name).map(Boolean::parseBoolean).orElse(false);
  }

  /**
   * Human readable "Label : value" lines of supplied parameters, in declaration order. Ids are
   * shown as the user knows them ("Company : FVI – BrokerVerse Insurance"), not as database ids.
   *
   * @return echo lines
   */
  public List<String> echo() {
    List<String> lines = new ArrayList<>();
    for (ParameterSpec spec : metadata.parameters()) {
      String value = values.get(spec.name());
      if (value != null) {
        lines.add(spec.label() + " : " + display.display(spec, value));
      }
    }
    return lines;
  }

  public ReportMetadata metadata() {
    return metadata;
  }

  private BusinessRuleException missing(String name) {
    return new BusinessRuleException("MISSING_PARAMETER", "Parameter " + name + " is required");
  }

  private static void checkType(ParameterSpec spec, String value, List<String> errors) {
    try {
      switch (spec.type()) {
        case DATE -> LocalDate.parse(value);
        case NUMBER -> new BigDecimal(value);
        case COMPANY, BRANCH -> Long.parseLong(value);
        case SELECT -> requireOption(spec, value, errors);
        default -> {
          // Free text, account, currency and boolean values need no format validation.
        }
      }
    } catch (DateTimeParseException | NumberFormatException ex) {
      errors.add(spec.label() + " has an invalid value '" + value + "'");
    }
  }

  private static void requireOption(ParameterSpec spec, String value, List<String> errors) {
    if (!spec.options().isEmpty() && !spec.options().contains(value)) {
      errors.add(spec.label() + " must be one of " + spec.options());
    }
  }

  /** Supplied value, else the default of an omitted parameter (never of a cleared required one). */
  private static String resolve(ParameterSpec spec, Map<String, String> raw, Clock clock) {
    boolean supplied = raw != null && raw.containsKey(spec.name());
    if (!supplied) {
      return resolveDefault(spec.defaultValue(), clock);
    }
    String value = trimToNull(raw.get(spec.name()));
    if (value != null || spec.required()) {
      return value;
    }
    return resolveDefault(spec.defaultValue(), clock);
  }

  private static String resolveDefault(String keyword, Clock clock) {
    if (keyword == null) {
      return null;
    }
    LocalDate today = LocalDate.now(clock);
    return switch (keyword) {
      case TODAY -> today.toString();
      case MONTH_START -> today.withDayOfMonth(1).toString();
      case YEAR_START -> today.withDayOfYear(1).toString();
      default -> keyword;
    };
  }

  private static String trimToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
