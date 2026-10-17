package com.iortatechnxt.brokerverse.migration.mapping.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MaskingRule;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MaskingRuleRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MigRule;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MigRuleRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The data-quality rule catalogue and the masking rules (DATA_MIGRATION_DESIGN sections 5.3 and 8):
 * the Data Steward sets the severity and activation of a rule and maintains the columns masked
 * outside production.
 */
@Service
@Transactional
public class RuleCatalogueService {

  private static final String ENTITY_RULE = "MigRule";
  private static final String ENTITY_MASKING = "MigMaskingRule";

  private final MigRuleRepository rules;
  private final MaskingRuleRepository masking;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param rules rules
   * @param masking masking rules
   * @param audit audit trail
   */
  public RuleCatalogueService(
      MigRuleRepository rules, MaskingRuleRepository masking, AuditTrailService audit) {
    this.rules = rules;
    this.masking = masking;
    this.audit = audit;
  }

  /**
   * Every rule.
   *
   * @return rules by code
   */
  @Transactional(readOnly = true)
  public List<MigRule> rules() {
    return rules.findAllByOrderByCodeAsc();
  }

  /**
   * The rules by code (rule engine).
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public Map<String, MigRule> byCode() {
    return rules.findAll().stream()
        .collect(Collectors.toMap(MigRule::getCode, Function.identity()));
  }

  /**
   * Changes the severity and activation of a rule.
   *
   * @param code rule
   * @param severity ERROR or WARNING
   * @param active active
   * @return the rule
   */
  public MigRule configure(String code, String severity, boolean active) {
    if (!"ERROR".equals(severity) && !"WARNING".equals(severity)) {
      throw new BusinessRuleException("MIG_RULE_SEVERITY", "The severity is ERROR or WARNING");
    }
    MigRule rule =
        rules.findByCode(code).orElseThrow(() -> new ResourceNotFoundException(ENTITY_RULE, code));
    String before = rule.getSeverity() + (rule.isActive() ? " active" : " inactive");
    rule.configure(severity, active);
    audit.record(
        ENTITY_RULE,
        code,
        AuditAction.UPDATE,
        "From " + before + " to " + severity + (active ? " active" : " inactive"));
    return rule;
  }

  /**
   * Every masking rule.
   *
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<MaskingRule> masking() {
    return masking.findAllByOrderByLayoutCodeAscColumnNameAsc();
  }

  /**
   * Adds a masking rule.
   *
   * @param layoutCode layout
   * @param column column
   * @param kind masking kind
   * @return the rule
   */
  public MaskingRule addMasking(String layoutCode, String column, MaskingRule.Kind kind) {
    boolean exists =
        masking.findAll().stream()
            .anyMatch(
                m -> m.getLayoutCode().equals(layoutCode) && m.getColumnName().equals(column));
    if (exists) {
      throw new BusinessRuleException(
          "MIG_MASKING_EXISTS", "Column " + column + " of " + layoutCode + " is already masked");
    }
    MaskingRule saved = masking.save(new MaskingRule(layoutCode, column, kind));
    audit.record(ENTITY_MASKING, layoutCode + "." + column, AuditAction.CREATE, kind.name());
    return saved;
  }

  /**
   * Switches a masking rule on or off.
   *
   * @param id rule
   * @param active active
   * @return the rule
   */
  public MaskingRule toggleMasking(Long id, boolean active) {
    MaskingRule rule =
        masking.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY_MASKING, id));
    rule.setActive(active);
    audit.record(
        ENTITY_MASKING,
        rule.getLayoutCode() + "." + rule.getColumnName(),
        AuditAction.UPDATE,
        active ? "Active" : "Inactive");
    return rule;
  }
}
