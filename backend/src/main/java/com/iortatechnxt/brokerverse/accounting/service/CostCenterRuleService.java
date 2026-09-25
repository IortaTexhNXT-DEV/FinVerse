package com.iortatechnxt.brokerverse.accounting.service;

import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRule;
import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRule.LineFacts;
import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRuleRepository;
import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRuleValues;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.domain.GlAccountRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cost-centre rules (FRBS 3.1.1, DIS 3.30.0; AQ26): maintained by Comptrollership and applied by
 * the accounting engine to every generated line whose account requires a cost centre and has none.
 * A line still without a cost centre stops the event (status FAILED in the event register, code
 * {@code COST_CENTER_MISSING}) and raises the alert of the same code, so the exception is flagged
 * and logged instead of being posted inconsistently.
 */
@Service
@Transactional
public class CostCenterRuleService {

  /** Error and alert code. */
  public static final String MISSING = "COST_CENTER_MISSING";

  private static final String ENTITY = "CostCenterRule";

  private final CostCenterRuleRepository rules;
  private final GlAccountRepository accounts;
  private final DimensionService dimensions;
  private final CostCenterAlerts alerts;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param rules rule repository
   * @param accounts accounts (cost-centre requirement)
   * @param dimensions dimension values (cost centre codes)
   * @param alerts alert raised when a cost centre is missing
   * @param audit audit trail
   */
  public CostCenterRuleService(
      CostCenterRuleRepository rules,
      GlAccountRepository accounts,
      DimensionService dimensions,
      CostCenterAlerts alerts,
      AuditTrailService audit) {
    this.rules = rules;
    this.accounts = accounts;
    this.dimensions = dimensions;
    this.alerts = alerts;
    this.audit = audit;
  }

  /**
   * Rules of a company in evaluation order.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<CostCenterRule> list(Long companyId) {
    return rules.findByCompanyIdOrderByPriorityAscIdAsc(companyId);
  }

  /**
   * Creates a rule.
   *
   * @param companyId company
   * @param values values
   * @return rule
   */
  public CostCenterRule create(Long companyId, CostCenterRuleValues values) {
    validate(companyId, values);
    CostCenterRule saved = rules.save(new CostCenterRule(companyId, values));
    audit.record(ENTITY, saved.getId(), AuditAction.CREATE, describe(saved));
    return saved;
  }

  /**
   * Changes a rule.
   *
   * @param id rule
   * @param values values
   * @return rule
   */
  public CostCenterRule update(Long id, CostCenterRuleValues values) {
    CostCenterRule rule =
        rules.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    validate(rule.getCompanyId(), values);
    rule.change(values);
    audit.record(ENTITY, id, AuditAction.UPDATE, describe(rule));
    return rule;
  }

  /**
   * Gives every line that needs a cost centre and has none the cost centre of the first matching
   * rule. Lines of accounts without the requirement are left as they are.
   *
   * @param event business event
   * @param lines lines built from the accounting rule
   * @return lines with the derived cost centres
   * @throws BusinessRuleException {@value #MISSING} when a line still has no cost centre (the alert
   *     is raised in its own transaction)
   */
  public List<JournalLineRequest> applyTo(BusinessEvent event, List<JournalLineRequest> lines) {
    Set<String> needing =
        accounts
            .findByCompanyIdAndCodeIn(
                event.companyId(), lines.stream().map(JournalLineRequest::accountCode).toList())
            .stream()
            .filter(GlAccount::isCostCenterRequired)
            .map(GlAccount::getCode)
            .collect(Collectors.toSet());
    if (lines.stream()
        .noneMatch(l -> l.costCenter() == null && needing.contains(l.accountCode()))) {
      return lines;
    }
    List<CostCenterRule> ordered = rules.findByCompanyIdOrderByPriorityAscIdAsc(event.companyId());
    List<String> missing = new ArrayList<>();
    List<JournalLineRequest> result =
        lines.stream()
            .map(
                l ->
                    l.costCenter() != null || !needing.contains(l.accountCode())
                        ? l
                        : derive(event, l, ordered, missing))
            .toList();
    if (!missing.isEmpty()) {
      String message =
          "No cost centre for account(s) "
              + String.join(", ", missing.stream().distinct().toList())
              + " of event "
              + event.eventType()
              + " "
              + event.sourceReference()
              + ": add a cost-centre rule";
      alerts.raiseMissing(event, message);
      throw new BusinessRuleException(MISSING, message);
    }
    return result;
  }

  private static JournalLineRequest derive(
      BusinessEvent event,
      JournalLineRequest line,
      List<CostCenterRule> ordered,
      List<String> missing) {
    LineFacts facts =
        new LineFacts(
            event.sourceModule(),
            event.eventType(),
            line.branchId(),
            line.partyCode(),
            line.accountCode());
    String costCenter =
        ordered.stream()
            .filter(r -> r.matches(facts))
            .map(CostCenterRule::getCostCenter)
            .findFirst()
            .orElse(null);
    if (costCenter == null) {
      missing.add(line.accountCode());
    }
    return withCostCenter(line, costCenter);
  }

  private void validate(Long companyId, CostCenterRuleValues values) {
    if (values.costCenter() == null || values.costCenter().isBlank()) {
      throw new BusinessRuleException("COST_CENTER_REQUIRED", "Enter the cost centre of the rule");
    }
    dimensions.validateOptional(companyId, DimensionType.COST_CENTER, values.costCenter());
    if (values.accountCode() != null
        && !accounts.existsByCompanyIdAndCode(companyId, values.accountCode())) {
      throw new ResourceNotFoundException("GL account", values.accountCode());
    }
  }

  private static JournalLineRequest withCostCenter(JournalLineRequest l, String costCenter) {
    return new JournalLineRequest(
        l.accountCode(),
        l.side(),
        l.amount(),
        l.currency(),
        l.exchangeRate(),
        l.branchId(),
        costCenter,
        l.businessLine(),
        l.partyCode(),
        l.reference(),
        l.narration());
  }

  private static String describe(CostCenterRule r) {
    return "Cost-centre rule "
        + r.getPriority()
        + ": "
        + Objects.toString(r.getSourceModule(), "*")
        + "/"
        + Objects.toString(r.getEventType(), "*")
        + "/"
        + Objects.toString(r.getAccountCode(), "*")
        + " -> "
        + r.getCostCenter()
        + (r.isActive() ? "" : " (inactive)");
  }
}
