package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinition;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleDefinitionRepository;
import com.iortatechnxt.brokerverse.finreport.domain.ScheduleValues;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment;
import com.iortatechnxt.brokerverse.finreport.domain.StatementComment.CommentKey;
import com.iortatechnxt.brokerverse.finreport.domain.StatementCommentRepository;
import java.util.List;
import java.util.Locale;
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
    ScheduleRules.validate(values);
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
    ScheduleRules.validate(values);
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
    String clean = ScheduleRules.comment(def, key, text);
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
}
