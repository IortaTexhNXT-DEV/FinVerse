package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleColumn;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinitionRepository;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Basis;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Comparative;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleEnums.Measure;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleValues;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment.CommentKey;
import com.iortatechnxt.brokerverse.finreport.domain.StatementCommentRepository;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the account schedule definitions and their commentary (FRBS 3.2.0; Appendix A II-45):
 * FRBS adds or changes a schedule as configuration once BDOI confirms its layout (AQ05), and keeps
 * a comment per row and month for the variance analyses.
 */
@Service
@Transactional
public class ScheduleDefinitionService {

  private static final String ENTITY = "ScheduleDefinition";
  private static final Pattern CODE = Pattern.compile("^[A-Z0-9][A-Z0-9-]{1,39}$");
  private static final Pattern PERIOD = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");
  private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
  private static final Set<Measure> COMPARED =
      EnumSet.of(Measure.COMPARATIVE, Measure.VARIANCE, Measure.VARIANCE_PCT);
  private static final int MAX_COMMENT = 1000;

  private final ScheduleDefinitionRepository definitions;
  private final StatementCommentRepository comments;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param definitions definitions
   * @param comments commentary
   * @param audit audit trail
   */
  public ScheduleDefinitionService(
      ScheduleDefinitionRepository definitions,
      StatementCommentRepository comments,
      AuditTrailService audit) {
    this.definitions = definitions;
    this.comments = comments;
    this.audit = audit;
  }

  /**
   * Every definition in code order.
   *
   * @param activeOnly only the active ones
   * @return definitions
   */
  @Transactional(readOnly = true)
  public List<ScheduleDefinition> list(boolean activeOnly) {
    return definitions.findAllByOrderByCodeAsc().stream()
        .filter(d -> !activeOnly || d.isActive())
        .toList();
  }

  /**
   * One definition.
   *
   * @param code code
   * @return definition
   */
  @Transactional(readOnly = true)
  public ScheduleDefinition get(String code) {
    return definitions
        .findByCode(code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
        .orElseThrow(() -> new ResourceNotFoundException("Account schedule", code));
  }

  /**
   * Adds a schedule.
   *
   * @param code unique code (upper case letters, digits and dashes)
   * @param values content
   * @return definition
   */
  public ScheduleDefinition create(String code, ScheduleValues values) {
    String clean = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    if (!CODE.matcher(clean).matches()) {
      throw new BusinessRuleException(
          "SCHEDULE_CODE", "Use 2 to 40 upper-case letters, digits or dashes for the code");
    }
    if (definitions.existsByCode(clean)) {
      throw new DuplicateResourceException("Account schedule", clean);
    }
    validate(values);
    ScheduleDefinition def = definitions.save(new ScheduleDefinition(clean, values));
    audit.record(ENTITY, clean, AuditAction.CREATE, "Schedule " + values.name());
    return def;
  }

  /**
   * Changes a schedule.
   *
   * @param code code
   * @param values content
   * @return definition
   */
  public ScheduleDefinition update(String code, ScheduleValues values) {
    ScheduleDefinition def = get(code);
    validate(values);
    def.apply(values);
    audit.record(
        ENTITY,
        def.getCode(),
        AuditAction.UPDATE,
        "Schedule "
            + values.name()
            + " ("
            + values.layoutStatus()
            + ", active "
            + values.active()
            + ")");
    return def;
  }

  /**
   * The commentary of a schedule for a month.
   *
   * @param companyId company
   * @param code schedule
   * @param period month {@code yyyy-MM}
   * @return comments
   */
  @Transactional(readOnly = true)
  public List<StatementComment> comments(Long companyId, String code, String period) {
    return comments.findByCompanyIdAndScheduleCodeAndPeriod(companyId, get(code).getCode(), period);
  }

  /**
   * Keeps the comment of one row for a month; a blank text removes it.
   *
   * @param key company, schedule, month and row
   * @param text comment
   * @return the comment, null when removed
   */
  public StatementComment comment(CommentKey key, String text) {
    ScheduleDefinition def = get(key.scheduleCode());
    if (!def.isCommentary()) {
      throw new BusinessRuleException(
          "SCHEDULE_NO_COMMENTARY", "Schedule " + def.getCode() + " has no commentary column");
    }
    if (key.period() == null || !PERIOD.matcher(key.period()).matches()) {
      throw new BusinessRuleException("COMMENT_PERIOD", "Give the month as yyyy-MM");
    }
    if (key.rowKey() == null || key.rowKey().isBlank()) {
      throw new BusinessRuleException("COMMENT_ROW", "Give the row of the comment");
    }
    String clean = text == null ? "" : text.trim();
    if (clean.length() > MAX_COMMENT) {
      throw new BusinessRuleException(
          "COMMENT_TOO_LONG", "A comment has at most " + MAX_COMMENT + " characters");
    }
    CommentKey normalised =
        new CommentKey(key.companyId(), def.getCode(), key.period(), key.rowKey().trim());
    StatementComment existing =
        comments
            .findByCompanyIdAndScheduleCodeAndPeriodAndRowKey(
                normalised.companyId(),
                normalised.scheduleCode(),
                normalised.period(),
                normalised.rowKey())
            .orElse(null);
    String subject = def.getCode() + " " + normalised.period() + " " + normalised.rowKey();
    if (clean.isEmpty()) {
      if (existing != null) {
        comments.delete(existing);
        audit.record(ENTITY, def.getCode(), AuditAction.UPDATE, "Comment removed: " + subject);
      }
      return null;
    }
    StatementComment saved = existing;
    if (saved == null) {
      saved = comments.save(new StatementComment(normalised, clean));
    } else {
      saved.change(clean);
    }
    audit.record(ENTITY, def.getCode(), AuditAction.UPDATE, "Comment on " + subject);
    return saved;
  }

  private static void validate(ScheduleValues v) {
    if (v.name() == null
        || v.family() == null
        || v.selectorKind() == null
        || v.grouping() == null
        || v.side() == null
        || v.basis() == null) {
      throw new BusinessRuleException(
          "SCHEDULE_INCOMPLETE", "Give the name, family, selector, grouping, side and basis");
    }
    if (v.selectorEntries().isEmpty()) {
      throw new BusinessRuleException(
          "SCHEDULE_ACCOUNTS", "List at least one account prefix or report group");
    }
    if (v.currency() != null && !CURRENCY.matcher(v.currency()).matches()) {
      throw new BusinessRuleException("SCHEDULE_CURRENCY", "Use a 3-letter currency code");
    }
    validateColumns(v);
    if (v.ageingSlots() != null) {
      if (v.basis() != Basis.BALANCE) {
        throw new BusinessRuleException(
            "SCHEDULE_AGEING_BASIS", "Only a balance schedule can be aged");
      }
      AgeingSlots.parse(v.ageingSlots(), AgeingSlots.STANDARD);
    }
  }

  private static void validateColumns(ScheduleValues v) {
    List<ScheduleColumn> columns = v.columns();
    if (columns.isEmpty()) {
      throw new BusinessRuleException("SCHEDULE_COLUMNS", "Choose at least one figure");
    }
    Set<Measure> seen = EnumSet.noneOf(Measure.class);
    for (ScheduleColumn c : columns) {
      if (c.measure() == null || c.label() == null || c.label().isBlank()) {
        throw new BusinessRuleException("SCHEDULE_COLUMNS", "Every figure needs a heading");
      }
      if (!seen.add(c.measure())) {
        throw new BusinessRuleException(
            "SCHEDULE_COLUMNS", "Figure " + c.measure() + " is chosen twice");
      }
    }
    boolean compared = seen.stream().anyMatch(COMPARED::contains);
    if (compared && v.comparative() == Comparative.NONE) {
      throw new BusinessRuleException(
          "SCHEDULE_COMPARATIVE", "Choose a comparative period for the comparative figures");
    }
  }
}
