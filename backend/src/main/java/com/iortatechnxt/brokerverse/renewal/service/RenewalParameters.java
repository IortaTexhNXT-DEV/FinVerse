package com.iortatechnxt.brokerverse.renewal.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Renewal parameters (sys_parameter category RENEWAL, V1010; RENEWAL_DESIGN section 9): lead days,
 * thresholds, notice days and switches. Every value is read at use, so a change applies from the
 * next action or run without a release (BRRN.030 AC 2). The defaults are placeholders until RQ04,
 * RQ09, RQ11, RQ18 and RQ25 are answered.
 */
@Component
public class RenewalParameters {

  /** Lead days of the extraction. */
  public static final String LEAD_DAYS = "RNW_EXTRACTION_LEAD_DAYS";

  /** Lead days per segment. */
  public static final String LEAD_DAYS_BY_SEGMENT = "RNW_EXTRACTION_LEAD_DAYS_BY_SEGMENT";

  /** Last expiry of the go-live window (Data Migration parameter). */
  public static final String GOLIVE_TO = "MIG_GOLIVE_RENEWAL_TO";

  /** Last urgent expiry of the go-live window (Data Migration parameter). */
  public static final String URGENT_TO = "MIG_RENEWAL_URGENT_TO";

  /** Segments initiated in bulk. */
  public static final String BULK_SEGMENTS = "RNW_BULK_INITIATION_SEGMENTS";

  /** Segments of the loan-driven straight-through path. */
  public static final String CBG_SEGMENTS = "RNW_CBG_SEGMENTS";

  /** Lines of the loan-driven straight-through path. */
  public static final String CBG_STP_LINES = "RNW_CBG_STP_LINES";

  /** Lines that need the PN. */
  public static final String PN_LINES = "RNW_PN_REQUIRED_LINES";

  /** Automatic dispositions skip the TL review. */
  public static final String STP_SKIP_REVIEW = "RNW_STP_SKIP_TL_REVIEW";

  /** Outstanding premium threshold. */
  public static final String OUTSTANDING_THRESHOLD = "RNW_OUTSTANDING_THRESHOLD";

  /** Financial impact tolerance. */
  public static final String FIN_IMPACT_TOLERANCE = "RNW_FIN_IMPACT_TOLERANCE";

  /** Minimum notice of a Renewal Advice. */
  public static final String RA_MIN_NOTICE = "RNW_RA_MIN_NOTICE_DAYS";

  /** Days before a second notice. */
  public static final String RA_SECOND_NOTICE = "RNW_RA_SECOND_NOTICE_DAYS";

  /** NRNS checkpoint. */
  public static final String NRNS_DAYS = "RNW_NRNS_REMINDER_DAYS";

  /** Non-acceptance days after expiry. */
  public static final String NON_ACCEPTANCE_DAYS = "RNW_NON_ACCEPTANCE_DAYS";

  /** Re-open days after expiry. */
  public static final String REOPEN_DAYS = "RNW_REOPEN_DAYS";

  /** Escalation days per segment. */
  public static final String ESCALATION_DAYS = "RNW_ESCALATION_DAYS";

  /** Segments of the KYC flag. */
  public static final String KYC_SEGMENTS = "RNW_KYC_SEGMENTS";

  /** Unit receiving RMU accounts. */
  public static final String RMU_UNIT = "RNW_RMU_UNIT";

  /** Automatic placement after acceptance. */
  public static final String AUTO_PLACEMENT = "RNW_AUTO_PLACEMENT";

  /** Prefix of the renewal reference. */
  public static final String REFERENCE_PREFIX = "RNW_REFERENCE_PREFIX";

  /** Lines excluded from extraction. */
  public static final String EXCLUDED_LINES = "RNW_EXCLUDED_LINES";

  /** Reasons answered with a No Advice Letter. */
  public static final String NAL_REASONS = "RNW_NAL_REASONS";

  /** Reasons that need the new invoice number. */
  public static final String INVOICE_NO_REASONS = "RNW_INVOICE_NO_REASONS";

  /** Reply days of an insurer batch. */
  public static final String INSURER_REPLY_DAYS = "RNW_INSURER_REPLY_DAYS";

  /** Exception ageing days. */
  public static final String EXCEPTION_AGEING_DAYS = "RNW_EXCEPTION_AGEING_DAYS";

  /** Parameter: priority portfolio segments of the attention flags (BRRN.036). */
  public static final String PRIORITY_SEGMENTS = "RNW_PRIORITY_SEGMENTS";

  /** Parameter: durations of a renewal hold cover (BRRN.042). */
  public static final String HOLD_COVER_DAYS = "RNW_HOLD_COVER_DAYS";

  /** Parameter: waiting days after the effective expiry before the NRNS tag (BRRN.037). */
  public static final String NRNS_WAITING_DAYS = "RNW_NRNS_WAITING_DAYS";

  private static final int DEFAULT_HOLD_COVER = 30;
  private static final int DEFAULT_LEAD = 140;
  private static final int DEFAULT_MIN_NOTICE = 30;
  private static final int DEFAULT_SECOND_NOTICE = 15;
  private static final int DEFAULT_NRNS = 90;
  private static final int DEFAULT_REOPEN = 30;
  private static final int DEFAULT_ESCALATION = 30;
  private static final int DEFAULT_REPLY = 10;
  private static final int DEFAULT_AGEING = 7;
  private static final LocalDate DEFAULT_GOLIVE_TO = LocalDate.of(2028, 5, 31);
  private static final LocalDate DEFAULT_URGENT_TO = LocalDate.of(2028, 1, 31);
  private static final String ANY_SEGMENT = "*";
  private static final String TRUE = "true";

  private final SystemParameterService parameters;

  /**
   * Creates the parameters.
   *
   * @param parameters business parameters
   */
  public RenewalParameters(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Lead days of a market segment: the per-segment value, else the general one.
   *
   * @param segment market segment, may be null
   * @return lead days
   */
  public int leadDays(String segment) {
    Integer bySegment = pairs(LEAD_DAYS_BY_SEGMENT).get(key(segment));
    return bySegment != null ? bySegment : parameters.intValue(LEAD_DAYS, DEFAULT_LEAD);
  }

  /**
   * Every lead-day value in use (general and per segment), for the extraction window.
   *
   * @return distinct lead days
   */
  public List<Integer> allLeadDays() {
    List<Integer> all = new ArrayList<>(pairs(LEAD_DAYS_BY_SEGMENT).values());
    all.add(parameters.intValue(LEAD_DAYS, DEFAULT_LEAD));
    return all.stream().distinct().sorted().toList();
  }

  /**
   * Last expiry of the go-live window.
   *
   * @return date
   */
  public LocalDate goLiveTo() {
    return date(GOLIVE_TO, DEFAULT_GOLIVE_TO);
  }

  /**
   * Last urgent expiry of the go-live window.
   *
   * @return date
   */
  public LocalDate urgentTo() {
    return date(URGENT_TO, DEFAULT_URGENT_TO);
  }

  /**
   * Whether a segment is initiated in bulk.
   *
   * @param segment market segment
   * @return true when listed
   */
  public boolean bulkInitiation(String segment) {
    return listed(BULK_SEGMENTS, segment);
  }

  /**
   * Whether a renewal follows the loan-driven straight-through path (BRRN.039): a segment and line
   * of the STP lists.
   *
   * @param segment market segment
   * @param line product line
   * @return true for the STP path
   */
  public boolean loanDriven(String segment, String line) {
    return listed(CBG_SEGMENTS, segment) && listed(CBG_STP_LINES, line);
  }

  /**
   * Whether a line needs the PN when mortgaged or on the straight-through path.
   *
   * @param line product line
   * @return true when listed
   */
  public boolean pnRequired(String line) {
    return listed(PN_LINES, line);
  }

  /**
   * Whether automatic dispositions skip the TL review.
   *
   * @return switch
   */
  public boolean stpSkipsReview() {
    return TRUE.equals(parameters.text(STP_SKIP_REVIEW, TRUE));
  }

  /**
   * Outstanding premium threshold.
   *
   * @return amount
   */
  public BigDecimal outstandingThreshold() {
    return decimal(OUTSTANDING_THRESHOLD);
  }

  /**
   * Financial impact tolerance.
   *
   * @return amount
   */
  public BigDecimal financialTolerance() {
    return decimal(FIN_IMPACT_TOLERANCE);
  }

  /**
   * Minimum notice of a Renewal Advice.
   *
   * @return days
   */
  public int raMinNoticeDays() {
    return parameters.intValue(RA_MIN_NOTICE, DEFAULT_MIN_NOTICE);
  }

  /**
   * Days after the first notice before a second notice.
   *
   * @return days
   */
  public int raSecondNoticeDays() {
    return parameters.intValue(RA_SECOND_NOTICE, DEFAULT_SECOND_NOTICE);
  }

  /**
   * NRNS checkpoint.
   *
   * @return days before expiry
   */
  public int nrnsDays() {
    return parameters.intValue(NRNS_DAYS, DEFAULT_NRNS);
  }

  /**
   * Non-acceptance point.
   *
   * @return days after expiry
   */
  public int nonAcceptanceDays() {
    return parameters.intValue(NON_ACCEPTANCE_DAYS, 0);
  }

  /**
   * Re-open window.
   *
   * @return days after expiry
   */
  public int reopenDays() {
    return parameters.intValue(REOPEN_DAYS, DEFAULT_REOPEN);
  }

  /**
   * Escalation days of a segment (BRRN.036): the segment's value, else the {@code *} value.
   *
   * @param segment market segment
   * @return days before expiry
   */
  public int escalationDays(String segment) {
    Map<String, Integer> values = pairs(ESCALATION_DAYS);
    Integer days = values.get(key(segment));
    if (days == null) {
      days = values.get(ANY_SEGMENT);
    }
    return days == null ? DEFAULT_ESCALATION : days;
  }

  /**
   * Whether the KYC flag applies to a segment.
   *
   * @param segment market segment
   * @return true when the list is empty or names it
   */
  public boolean kycApplies(String segment) {
    List<String> segments = parameters.items(KYC_SEGMENTS);
    return segments.isEmpty() || listed(KYC_SEGMENTS, segment);
  }

  /**
   * Unit receiving RMU accounts.
   *
   * @return unit, empty to tag only
   */
  public Optional<String> rmuUnit() {
    String unit = parameters.text(RMU_UNIT, "");
    return unit == null || unit.isBlank() ? Optional.empty() : Optional.of(unit.strip());
  }

  /**
   * Whether accepted renewals are placed automatically; off by default, placement and booking stay
   * user actions (Walkthrough addendum p.4; FR-RN-084 R2).
   *
   * @return switch
   */
  public boolean autoPlacement() {
    return TRUE.equals(parameters.text(AUTO_PLACEMENT, "false"));
  }

  /**
   * Prefix of the renewal reference.
   *
   * @return prefix
   */
  public String referencePrefix() {
    String prefix = parameters.text(REFERENCE_PREFIX, "RNW");
    return prefix == null || prefix.isBlank() ? "RNW" : prefix.strip();
  }

  /**
   * Lines never extracted.
   *
   * @return lines
   */
  public List<String> excludedLines() {
    return parameters.items(EXCLUDED_LINES);
  }

  /**
   * Whether a reason for Not for Renewal is answered with a No Advice Letter.
   *
   * @param reason reason
   * @return true when listed
   */
  public boolean nalReason(String reason) {
    return listed(NAL_REASONS, reason);
  }

  /**
   * Whether a reason needs the new invoice number.
   *
   * @param reason reason
   * @return true when listed
   */
  public boolean invoiceNoReason(String reason) {
    return listed(INVOICE_NO_REASONS, reason);
  }

  /**
   * Reply days of an insurer batch.
   *
   * @return days
   */
  public int insurerReplyDays() {
    return parameters.intValue(INSURER_REPLY_DAYS, DEFAULT_REPLY);
  }

  /**
   * Days a renewal may stay in the Exception bucket.
   *
   * @return days
   */
  public int exceptionAgeingDays() {
    return parameters.intValue(EXCEPTION_AGEING_DAYS, DEFAULT_AGEING);
  }

  /**
   * Segments of the priority portfolio named in the attention flags (Annex BRRN.036).
   *
   * @return segments
   */
  public List<String> prioritySegments() {
    return parameters.items(PRIORITY_SEGMENTS);
  }

  /**
   * Durations offered for the hold cover of a renewal (BRRN.042), the default first.
   *
   * @return days, at least one
   */
  public List<Integer> holdCoverDays() {
    List<Integer> days =
        parameters.items(HOLD_COVER_DAYS).stream()
            .map(String::strip)
            .filter(v -> v.matches("\\d{1,3}"))
            .map(Integer::valueOf)
            .toList();
    return days.isEmpty() ? List.of(DEFAULT_HOLD_COVER) : days;
  }

  /**
   * Days after the effective expiry date before an unrenewed renewal of a segment is tagged NRNS
   * (Annex BRRN.037, CBG and non-CBG; CLR-RN-33).
   *
   * @param segment market segment
   * @return days, 0 by default
   */
  public int nrnsWaitingDays(String segment) {
    Map<String, Integer> values = pairs(NRNS_WAITING_DAYS);
    Integer days = values.get(key(segment));
    if (days == null) {
      days = values.get(ANY_SEGMENT);
    }
    return days == null ? 0 : days;
  }

  private boolean listed(String key, String value) {
    return value != null && parameters.items(key).stream().anyMatch(v -> v.equals(value.strip()));
  }

  private BigDecimal decimal(String key) {
    try {
      return new BigDecimal(parameters.text(key, "0.00").strip());
    } catch (NumberFormatException e) {
      return BigDecimal.ZERO;
    }
  }

  private LocalDate date(String key, LocalDate fallback) {
    try {
      String value = parameters.text(key, null);
      return value == null || value.isBlank() ? fallback : LocalDate.parse(value.strip());
    } catch (DateTimeParseException e) {
      return fallback;
    }
  }

  /** Reads "A=1,B=2" pairs; invalid pairs are ignored. */
  private Map<String, Integer> pairs(String key) {
    Map<String, Integer> map = new HashMap<>();
    String value = parameters.text(key, "");
    if (value == null || value.isBlank()) {
      return map;
    }
    for (String pair : value.split(",")) {
      String[] parts = pair.split("=");
      if (parts.length == 2 && parts[1].strip().matches("\\d{1,4}")) {
        map.put(key(parts[0]), Integer.valueOf(parts[1].strip()));
      }
    }
    return map;
  }

  private static String key(String segment) {
    return segment == null ? "" : segment.strip().toUpperCase(Locale.ROOT);
  }
}
