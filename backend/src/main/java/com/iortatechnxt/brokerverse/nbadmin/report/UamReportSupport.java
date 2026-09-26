package com.iortatechnxt.brokerverse.nbadmin.report;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared parameters and helpers of the five user access reports (USER_ACCESS_DESIGN section 11.1;
 * FRS BRD-11 section 6): category Control &amp; Audit, run and export with {@code UAM_REPORT_VIEW},
 * runs and exports archived; dates in Philippine time.
 */
final class UamReportSupport {

  /** As-of date parameter. */
  static final String AS_OF = "asOf";

  /** Start of a period. */
  static final String FROM = "from";

  /** End of a period. */
  static final String TO = "to";

  /** Group profile (role code) filter. */
  static final String GROUP_PROFILE = "groupProfile";

  /** "Any" option of the select filters. */
  static final String ALL = "ALL";

  /** Business time zone of BDOI. */
  static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  /** Text of an absent value in the from / to columns (sample D). */
  static final String NULL_TEXT = "Null";

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);

  /** Activity of a change log row as the audit report sample D words it (BRD 4.003.1). */
  private static final Map<String, String> ACTIVITIES =
      Map.ofEntries(
          Map.entry("CREATE_USER", "Enroll New User"),
          Map.entry("MODIFY_USER", "Modify User Data of"),
          Map.entry("ROLES_CHANGED", "Modify User Group Profile of"),
          Map.entry("DISABLE_USER", "Deactivate User"),
          Map.entry("ENABLE_USER", "Reactivate User"),
          Map.entry("UNLOCK", "Unlock User"),
          Map.entry("PASSWORD_RESET", "Reset Password of"),
          Map.entry("CREATE_ROLE", "Create Group Profile"),
          Map.entry("ROLE_PERMISSIONS", "Modify Group Profile Access of"),
          Map.entry("DEACTIVATE_ROLE", "Deactivate Group Profile"),
          Map.entry("REACTIVATE_ROLE", "Reactivate Group Profile"));

  private UamReportSupport() {}

  /**
   * The words of a change log activity ("Modify User Group Profile of a013000101").
   *
   * @param activity activity code
   * @param subject user or group profile
   * @return words
   */
  static String activity(String activity, String subject) {
    return ACTIVITIES.getOrDefault(activity, words(activity)) + " " + subject;
  }

  /**
   * The words of a change log activity without its subject ("Modify User Group Profile").
   *
   * @param activity activity code
   * @return words
   */
  static String activityName(String activity) {
    String words = ACTIVITIES.getOrDefault(activity, words(activity));
    return words.endsWith(" of") ? words.substring(0, words.length() - " of".length()) : words;
  }

  /**
   * Metadata of a user access report.
   *
   * @param code report code
   * @param title title
   * @param description one line purpose citing the BRD ID
   * @param params parameters
   * @return metadata
   */
  static ReportMetadata metadata(
      String code, String title, String description, List<ParameterSpec> params) {
    return new ReportMetadata(
        code,
        title,
        ReportCategory.CONTROL,
        description,
        params,
        Permission.UAM_REPORT_VIEW,
        Permission.UAM_REPORT_VIEW,
        true);
  }

  /**
   * The as-of date parameter (default today).
   *
   * @return spec
   */
  static ParameterSpec asOfParam() {
    return ParameterSpec.required(AS_OF, "As of Date", ParameterType.DATE).withDefault("TODAY");
  }

  /**
   * The optional group profile filter.
   *
   * @return spec
   */
  static ParameterSpec groupProfileParam() {
    return ParameterSpec.optional(GROUP_PROFILE, "Group Profile (code)", ParameterType.TEXT);
  }

  /**
   * The period parameters (default: this month).
   *
   * @return specs
   */
  static List<ParameterSpec> periodParams() {
    return List.of(
        ParameterSpec.required(FROM, "Date From", ParameterType.DATE).withDefault("MONTH_START"),
        ParameterSpec.required(TO, "Date To", ParameterType.DATE).withDefault("TODAY"));
  }

  /**
   * The as-of date, refused when in the future (FR-UA-060).
   *
   * @param p parameters
   * @param clock clock
   * @return as-of date
   */
  static LocalDate asOf(ReportParameters p, Clock clock) {
    LocalDate asOf = p.date(AS_OF);
    if (asOf.isAfter(LocalDate.now(clock.withZone(MANILA)))) {
      throw new BusinessRuleException("AS_OF_IN_FUTURE", "The as-of date cannot be in the future");
    }
    return asOf;
  }

  /**
   * The period, refused when the end is before the start (FR-UA-063).
   *
   * @param p parameters
   * @return start and end dates
   */
  static LocalDate[] period(ReportParameters p) {
    LocalDate from = p.date(FROM);
    LocalDate to = p.date(TO);
    if (to.isBefore(from)) {
      throw new BusinessRuleException(
          "DATE_RANGE_REVERSED", "The end date must be on or after the start date");
    }
    return new LocalDate[] {from, to};
  }

  /**
   * The first instant after a Philippine date.
   *
   * @param date date
   * @return start of the next day
   */
  static Instant endOf(LocalDate date) {
    return date.plusDays(1).atStartOfDay(MANILA).toInstant();
  }

  /**
   * The first instant of a Philippine date.
   *
   * @param date date
   * @return start of the day
   */
  static Instant startOf(LocalDate date) {
    return date.atStartOfDay(MANILA).toInstant();
  }

  /**
   * An instant as a Philippine date.
   *
   * @param instant instant, may be null
   * @return date or null
   */
  static LocalDate date(Instant instant) {
    return instant == null ? null : instant.atZone(MANILA).toLocalDate();
  }

  /**
   * An instant as Philippine date and time text.
   *
   * @param instant instant
   * @return text
   */
  static String dateTime(Instant instant) {
    return DATE_TIME.format(instant.atZone(MANILA));
  }

  /**
   * An instant of a SQL row.
   *
   * @param value timestamp value
   * @return instant, null when absent
   */
  static Instant instant(Object value) {
    if (value instanceof Timestamp t) {
      return t.toInstant();
    }
    return value instanceof Instant i ? i : null;
  }

  /**
   * A text value of a SQL row.
   *
   * @param row row
   * @param key column
   * @return text or null
   */
  static String text(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? null : value.toString();
  }

  /**
   * A value for the from / to columns: "Null" when absent.
   *
   * @param value value
   * @return text
   */
  static String orNull(String value) {
    return value == null || value.isBlank() ? NULL_TEXT : value;
  }

  /**
   * Role codes of a comma separated list, replaced by the role names (group profile names).
   *
   * @param codes codes, may be null
   * @param names role name by code
   * @return names joined with ", "
   */
  static String roleNames(String codes, Map<String, String> names) {
    if (codes == null || codes.isBlank()) {
      return "";
    }
    return Arrays.stream(codes.split(","))
        .map(String::trim)
        .filter(c -> !c.isEmpty())
        .map(c -> names.getOrDefault(c, c))
        .collect(Collectors.joining(", "));
  }

  /**
   * An UPPER_SNAKE code as words ("ROLES_CHANGED" becomes "Roles Changed").
   *
   * @param code code
   * @return words
   */
  static String words(String code) {
    if (code == null) {
      return "";
    }
    return Arrays.stream(code.toLowerCase(Locale.ROOT).split("_"))
        .filter(w -> !w.isEmpty())
        .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
        .collect(Collectors.joining(" "));
  }

  /**
   * Whether an optional text filter matches a value (case-insensitive equality).
   *
   * @param filter filter, null for none
   * @param value value
   * @return true when it matches
   */
  static boolean matches(String filter, String value) {
    return filter == null || filter.isBlank() || same(filter.trim(), value);
  }

  /**
   * Whether two codes or user names are the same, ignoring case.
   *
   * @param a first, may be null
   * @param b second, may be null
   * @return true when both are present and equal ignoring case
   */
  static boolean same(String a, String b) {
    return a != null && b != null && String.CASE_INSENSITIVE_ORDER.compare(a, b) == 0;
  }
}
