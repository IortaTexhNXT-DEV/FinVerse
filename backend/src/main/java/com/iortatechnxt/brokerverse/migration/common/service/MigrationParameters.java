package com.iortatechnxt.brokerverse.migration.common.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Data Migration parameters (sys_parameter category DATA_MIGRATION, V1080; DATA_MIGRATION_DESIGN
 * section 20). Every threshold, date and switch of the migration services is read here, so a change
 * on the parameter screen applies at the next run.
 */
@Component
public class MigrationParameters {

  /** PRODUCTION or NON_PRODUCTION. */
  public static final String ENVIRONMENT_CLASS = "MIG_ENVIRONMENT_CLASS";

  /** Go-live date. */
  public static final String CUTOVER_DATE = "MIG_CUTOVER_DATE";

  /** Staging retention days. */
  public static final String RETENTION_DAYS = "MIG_STAGING_RETENTION_DAYS";

  /** Rows per load transaction. */
  public static final String CHUNK_SIZE = "MIG_CHUNK_SIZE";

  /** Parallel partitions of a load. */
  public static final String PARTITIONS = "MIG_PARTITIONS";

  /** Amount tolerance of the reconciliation. */
  public static final String AMOUNT_TOLERANCE = "MIG_AMOUNT_TOLERANCE";

  /** Error rate limit of master data (percent). */
  public static final String MAX_ERROR_RATE_MASTER = "MIG_MAX_ERROR_RATE_MASTER";

  /** Error rate limit of financial objects (percent). */
  public static final String MAX_ERROR_RATE_FINANCIAL = "MIG_MAX_ERROR_RATE_FINANCIAL";

  /** Auto-merge score of client matching. */
  public static final String CLIENT_MATCH_AUTO = "MIG_CLIENT_MATCH_AUTO";

  /** Review score of client matching. */
  public static final String CLIENT_MATCH_REVIEW = "MIG_CLIENT_MATCH_REVIEW";

  /** Prefix colliding invoice numbers with the source system. */
  public static final String INVOICE_NO_COLLISION_PREFIX = "MIG_INVOICE_NO_COLLISION_PREFIX";

  /** Largest archive export. */
  public static final String ARCHIVE_EXPORT_MAX_ROWS = "MIG_ARCHIVE_EXPORT_MAX_ROWS";

  /** Archive rows exported in a day that raise the unusual-access alert. */
  public static final String ACCESS_EXPORT_ALERT_ROWS = "MIG_ACCESS_EXPORT_ALERT_ROWS";

  /** A reason is required before the archive is searched. */
  public static final String ACCESS_REASON_REQUIRED = "MIG_LEGACY_ACCESS_REASON_REQUIRED";

  /** Value date of the opening entries. */
  public static final String OPENING_VALUE_DATE = "MIG_OPENING_VALUE_DATE";

  /** Last expiry of the go-live renewal window. */
  public static final String GOLIVE_RENEWAL_TO = "MIG_GOLIVE_RENEWAL_TO";

  /** Last expiry flagged urgent in the go-live renewal window. */
  public static final String RENEWAL_URGENT_TO = "MIG_RENEWAL_URGENT_TO";

  /** Last approval time of a resubmission. */
  public static final String RESUBMIT_DEADLINE = "MIG_RESUBMIT_DEADLINE";

  /** Last legacy business day. */
  public static final String LAST_LEGACY_BUSINESS_DAY = "MIG_LAST_LEGACY_BUSINESS_DAY";

  /** Start of the legacy business freeze. */
  public static final String FREEZE_AT = "MIG_FREEZE_AT";

  /** Year-end option (A = provisional opening and true-ups). */
  public static final String YEAR_END_OPTION = "MIG_YEAR_END_OPTION";

  /** Largest lead time of an advice already sent before a warning. */
  public static final String RA_MAX_LEAD_DAYS = "MIG_RA_MAX_LEAD_DAYS";

  /** Issue a BIBS acknowledgement receipt per migrated unapplied payment. */
  public static final String UPP_ISSUE_AR = "MIG_UPP_ISSUE_AR";

  /** Legacy invoice number patterns. */
  public static final String LEGACY_INVOICE_NO_PATTERN = "MIG_LEGACY_INVOICE_NO_PATTERN";

  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final int DEFAULT_RETENTION = 5;
  private static final int DEFAULT_CHUNK = 500;
  private static final int DEFAULT_PARTITIONS = 4;
  private static final int DEFAULT_AUTO = 90;
  private static final int DEFAULT_REVIEW = 60;
  private static final int DEFAULT_EXPORT_MAX = 1000;
  private static final int DEFAULT_EXPORT_ALERT = 5000;
  private static final int DEFAULT_RA_LEAD = 140;
  private static final LocalDate DEFAULT_CUTOVER = LocalDate.of(2028, 1, 3);
  private static final LocalDate DEFAULT_OPENING = LocalDate.of(2028, 1, 1);
  private static final LocalDate DEFAULT_RENEWAL_TO = LocalDate.of(2028, 5, 31);
  private static final LocalDate DEFAULT_URGENT_TO = LocalDate.of(2028, 1, 31);
  private static final LocalDate DEFAULT_LAST_DAY = LocalDate.of(2027, 12, 29);
  private static final LocalDateTime DEFAULT_RESUBMIT = LocalDateTime.of(2028, 1, 2, 12, 0);
  private static final LocalDateTime DEFAULT_FREEZE = LocalDateTime.of(2027, 12, 31, 22, 0);
  private static final String PRODUCTION = "PRODUCTION";
  private static final String TRUE = "true";

  private final SystemParameterService parameters;

  /**
   * Creates the parameters.
   *
   * @param parameters business parameters
   */
  public MigrationParameters(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Whether this environment is production (no masking).
   *
   * @return true in production
   */
  public boolean production() {
    return PRODUCTION.equalsIgnoreCase(parameters.text(ENVIRONMENT_CLASS, "NON_PRODUCTION").trim());
  }

  /**
   * Environment class stored on each batch.
   *
   * @return PRODUCTION or NON_PRODUCTION
   */
  public String environmentClass() {
    return production() ? PRODUCTION : "NON_PRODUCTION";
  }

  public LocalDate cutoverDate() {
    return date(CUTOVER_DATE, DEFAULT_CUTOVER);
  }

  public int retentionDays() {
    return parameters.intValue(RETENTION_DAYS, DEFAULT_RETENTION);
  }

  public int chunkSize() {
    return Math.max(1, parameters.intValue(CHUNK_SIZE, DEFAULT_CHUNK));
  }

  public int partitions() {
    return Math.max(1, parameters.intValue(PARTITIONS, DEFAULT_PARTITIONS));
  }

  public BigDecimal amountTolerance() {
    return decimal(AMOUNT_TOLERANCE, BigDecimal.ZERO);
  }

  /**
   * Error rate limit of an object class.
   *
   * @param financial open items, unapplied payments or trial balance
   * @return limit in percent
   */
  public BigDecimal maxErrorRate(boolean financial) {
    return financial
        ? decimal(MAX_ERROR_RATE_FINANCIAL, BigDecimal.ZERO)
        : decimal(MAX_ERROR_RATE_MASTER, new BigDecimal("0.5"));
  }

  public int clientMatchAuto() {
    return parameters.intValue(CLIENT_MATCH_AUTO, DEFAULT_AUTO);
  }

  public int clientMatchReview() {
    return parameters.intValue(CLIENT_MATCH_REVIEW, DEFAULT_REVIEW);
  }

  public boolean invoiceNoCollisionPrefix() {
    return flag(INVOICE_NO_COLLISION_PREFIX, true);
  }

  public int archiveExportMaxRows() {
    return parameters.intValue(ARCHIVE_EXPORT_MAX_ROWS, DEFAULT_EXPORT_MAX);
  }

  public int accessExportAlertRows() {
    return parameters.intValue(ACCESS_EXPORT_ALERT_ROWS, DEFAULT_EXPORT_ALERT);
  }

  public boolean accessReasonRequired() {
    return flag(ACCESS_REASON_REQUIRED, true);
  }

  public LocalDate openingValueDate() {
    return date(OPENING_VALUE_DATE, DEFAULT_OPENING);
  }

  public LocalDate goLiveRenewalTo() {
    return date(GOLIVE_RENEWAL_TO, DEFAULT_RENEWAL_TO);
  }

  public LocalDate renewalUrgentTo() {
    return date(RENEWAL_URGENT_TO, DEFAULT_URGENT_TO);
  }

  public LocalDateTime resubmitDeadline() {
    return stamp(RESUBMIT_DEADLINE, DEFAULT_RESUBMIT);
  }

  public LocalDate lastLegacyBusinessDay() {
    return date(LAST_LEGACY_BUSINESS_DAY, DEFAULT_LAST_DAY);
  }

  public LocalDateTime freezeAt() {
    return stamp(FREEZE_AT, DEFAULT_FREEZE);
  }

  public String yearEndOption() {
    return parameters.text(YEAR_END_OPTION, "A").trim().toUpperCase(Locale.ROOT);
  }

  public int raMaxLeadDays() {
    return parameters.intValue(RA_MAX_LEAD_DAYS, DEFAULT_RA_LEAD);
  }

  public boolean uppIssueAr() {
    return flag(UPP_ISSUE_AR, false);
  }

  /**
   * Legacy invoice number patterns (comma separated regular expressions).
   *
   * @return patterns
   */
  public List<String> legacyInvoiceNoPatterns() {
    String raw = parameters.text(LEGACY_INVOICE_NO_PATTERN, "^I\\d{8}$");
    return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  private boolean flag(String key, boolean fallback) {
    return TRUE.equalsIgnoreCase(parameters.text(key, String.valueOf(fallback)).trim());
  }

  private BigDecimal decimal(String key, BigDecimal fallback) {
    String raw = parameters.text(key, "").trim();
    if (raw.isEmpty()) {
      return fallback;
    }
    try {
      return new BigDecimal(raw);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private LocalDate date(String key, LocalDate fallback) {
    String raw = parameters.text(key, "").trim();
    return raw.isEmpty() ? fallback : LocalDate.parse(raw);
  }

  private LocalDateTime stamp(String key, LocalDateTime fallback) {
    String raw = parameters.text(key, "").trim();
    return raw.isEmpty() ? fallback : LocalDateTime.parse(raw, STAMP);
  }
}
