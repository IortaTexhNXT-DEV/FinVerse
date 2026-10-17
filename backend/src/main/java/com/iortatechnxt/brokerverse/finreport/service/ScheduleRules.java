package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleColumn;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Basis;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Comparative;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleValues;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment.CommentKey;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The rules of an account schedule definition and of its commentary (FRBS 3.2.0): complete content,
 * at least one account entry, a 3-letter currency, distinct figures with headings, a comparative
 * period for comparative figures, ageing only on a balance schedule with valid buckets; a comment
 * on a commentary schedule, for a month and a row, of at most 1000 characters.
 */
public final class ScheduleRules {

  private static final Pattern PERIOD = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
  private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
  private static final Set<Measure> COMPARED =
      EnumSet.of(Measure.COMPARATIVE, Measure.VARIANCE, Measure.VARIANCE_PCT);
  private static final int MAX_COMMENT = 1000;
  private static final String COLUMNS = "SCHEDULE_COLUMNS";

  private ScheduleRules() {}

  /**
   * Checks a definition's content.
   *
   * @param v content
   */
  public static void validate(ScheduleValues v) {
    requireComplete(v);
    if (v.selectorEntries().isEmpty()) {
      throw new BusinessRuleException(
          "SCHEDULE_ACCOUNTS", "List at least one account prefix or report group");
    }
    if (v.currency() != null && !CURRENCY.matcher(v.currency()).matches()) {
      throw new BusinessRuleException("SCHEDULE_CURRENCY", "Use a 3-letter currency code");
    }
    validateColumns(v);
    validateAgeing(v);
  }

  /**
   * Checks a comment and returns its trimmed text (empty to remove it).
   *
   * @param def schedule
   * @param key company, schedule, month and row
   * @param text comment
   * @return trimmed text
   */
  public static String comment(ScheduleDefinition def, CommentKey key, String text) {
    if (!def.isCommentary()) {
      throw new BusinessRuleException(
          "SCHEDULE_NO_COMMENTARY", "Schedule " + def.getCode() + " has no commentary column");
    }
    requirePeriod(key.period());
    if (key.rowKey() == null || key.rowKey().isBlank()) {
      throw new BusinessRuleException("COMMENT_ROW", "Give the row of the comment");
    }
    String clean = text == null ? "" : text.trim();
    if (clean.length() > MAX_COMMENT) {
      throw new BusinessRuleException(
          "COMMENT_TOO_LONG", "A comment has at most " + MAX_COMMENT + " characters");
    }
    return clean;
  }

  private static void requirePeriod(String period) {
    if (period == null || !PERIOD.matcher(period).matches()) {
      throw new BusinessRuleException("COMMENT_PERIOD", "Give the month as yyyy-MM");
    }
  }

  private static void requireComplete(ScheduleValues v) {
    boolean kinds = v.family() != null && v.selectorKind() != null && v.grouping() != null;
    boolean figures = v.side() != null && v.basis() != null;
    if (v.name() == null || !kinds || !figures) {
      throw new BusinessRuleException(
          "SCHEDULE_INCOMPLETE", "Give the name, family, selector, grouping, side and basis");
    }
  }

  private static void validateAgeing(ScheduleValues v) {
    if (v.ageingSlots() == null) {
      return;
    }
    if (v.basis() != Basis.BALANCE) {
      throw new BusinessRuleException(
          "SCHEDULE_AGEING_BASIS", "Only a balance schedule can be aged");
    }
    AgeingSlots.parse(v.ageingSlots(), AgeingSlots.STANDARD);
  }

  private static void validateColumns(ScheduleValues v) {
    if (v.columns().isEmpty()) {
      throw new BusinessRuleException(COLUMNS, "Choose at least one figure");
    }
    Set<Measure> seen = EnumSet.noneOf(Measure.class);
    for (ScheduleColumn c : v.columns()) {
      requireHeading(c);
      if (!seen.add(c.measure())) {
        throw new BusinessRuleException(COLUMNS, "Figure " + c.measure() + " is chosen twice");
      }
    }
    boolean compared = seen.stream().anyMatch(COMPARED::contains);
    if (compared && v.comparative() == Comparative.NONE) {
      throw new BusinessRuleException(
          "SCHEDULE_COMPARATIVE", "Choose a comparative period for the comparative figures");
    }
  }

  private static void requireHeading(ScheduleColumn c) {
    if (c.measure() == null || c.label() == null || c.label().isBlank()) {
      throw new BusinessRuleException(COLUMNS, "Every figure needs a heading");
    }
  }
}
