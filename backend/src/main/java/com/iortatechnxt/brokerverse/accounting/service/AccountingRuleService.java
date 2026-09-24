package com.iortatechnxt.brokerverse.accounting.service;

import com.iortatechnxt.brokerverse.accounting.api.dto.RuleLineDto;
import com.iortatechnxt.brokerverse.accounting.api.dto.RuleRequest;
import com.iortatechnxt.brokerverse.accounting.api.dto.SimulationRequest;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventLog;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventLogRepository;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventType;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventTypeRepository;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingRule;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingRuleRepository;
import com.iortatechnxt.brokerverse.accounting.domain.EventStatus;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration of the accounting engine: event types, rules (maker-checker) and a simulator that
 * previews the journal a rule would generate without posting anything.
 */
@Service
@Transactional
public class AccountingRuleService {

  private static final String ENTITY = "AccountingRule";

  private final AccountingRuleRepository rules;
  private final AccountingEventTypeRepository eventTypes;
  private final AccountingEventLogRepository eventLog;
  private final RuleResolver resolver;
  private final JournalLineBuilder lineBuilder;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rules rule repository
   * @param eventTypes event type repository
   * @param eventLog event register
   * @param resolver rule resolver
   * @param lineBuilder line builder
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AccountingRuleService(
      AccountingRuleRepository rules,
      AccountingEventTypeRepository eventTypes,
      AccountingEventLogRepository eventLog,
      RuleResolver resolver,
      JournalLineBuilder lineBuilder,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.rules = rules;
    this.eventTypes = eventTypes;
    this.eventLog = eventLog;
    this.resolver = resolver;
    this.lineBuilder = lineBuilder;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists event types.
   *
   * @return event types
   */
  @Transactional(readOnly = true)
  public List<AccountingEventType> eventTypes() {
    return eventTypes.findAllByOrderByCategoryAscCodeAsc();
  }

  /**
   * Lists the rules of a company.
   *
   * @param companyId company
   * @return rules
   */
  @Transactional(readOnly = true)
  public List<AccountingRule> list(Long companyId) {
    return rules.findByCompanyIdOrderByEventTypeAscPriorityAsc(companyId);
  }

  /**
   * Gets a rule.
   *
   * @param id id
   * @return rule
   */
  @Transactional(readOnly = true)
  public AccountingRule get(Long id) {
    return rules.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Creates a rule (pending authorization).
   *
   * @param r request
   * @return rule
   */
  public AccountingRule create(RuleRequest r) {
    if (!eventTypes.existsById(r.eventType())) {
      throw new BusinessRuleException("UNKNOWN_EVENT_TYPE", "Unknown event type " + r.eventType());
    }
    AccountingRule rule =
        new AccountingRule(r.companyId(), r.eventType(), r.name(), r.effectiveFrom());
    apply(rule, r);
    AccountingRule saved = rules.save(rule);
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        "Rule " + saved.getName() + " for " + saved.getEventType());
    return saved;
  }

  /**
   * Updates a rule; it returns to pending authorization.
   *
   * @param id id
   * @param r request
   * @return rule
   */
  public AccountingRule update(Long id, RuleRequest r) {
    AccountingRule rule = get(id);
    rule.setName(r.name());
    rule.setEffectiveFrom(r.effectiveFrom());
    apply(rule, r);
    rule.markModified();
    audit.record(ENTITY, rule.getId(), AuditAction.UPDATE, "Rule " + rule.getName() + " updated");
    return rule;
  }

  /**
   * Authorizes a rule.
   *
   * @param id id
   * @return rule
   */
  public AccountingRule authorize(Long id) {
    AccountingRule rule = get(id);
    rule.authorize(currentUser.username(), clock.instant());
    audit.record(
        ENTITY, rule.getId(), AuditAction.AUTHORIZE, "Rule " + rule.getName() + " authorized");
    return rule;
  }

  /**
   * Previews the journal lines a sample event would generate.
   *
   * @param s sample event
   * @return rule selected and generated lines
   */
  @Transactional(readOnly = true)
  public Simulation simulate(SimulationRequest s) {
    BusinessEvent event =
        new BusinessEvent(
            s.eventType(),
            s.companyId(),
            s.branchId(),
            s.valueDate(),
            s.currency(),
            "SIMULATION",
            "SIMULATION",
            "SIMULATION",
            s.partyCode(),
            s.businessLine(),
            null,
            "Simulation of " + s.eventType(),
            s.amounts(),
            s.accounts());
    return preview(event);
  }

  /**
   * Previews the journal lines an event would generate, without posting (e.g. the pre-booking
   * confirmation of a broker booking, BRNB.036).
   *
   * @param event event as it would be published
   * @return rule selected and generated lines
   */
  @Transactional(readOnly = true)
  public Simulation preview(BusinessEvent event) {
    AccountingRule rule = resolver.resolve(event);
    return Simulation.of(rule.getId(), rule.getName(), lineBuilder.build(rule, event));
  }

  /**
   * Searches the event register.
   *
   * @param companyId company
   * @param status status filter
   * @param eventType event type filter
   * @param from value date from
   * @param to value date to
   * @param pageable paging
   * @return page of events
   */
  @Transactional(readOnly = true)
  public Page<AccountingEventLog> events(
      Long companyId,
      EventStatus status,
      String eventType,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    return eventLog.search(companyId, status, eventType, from, to, pageable);
  }

  private static void apply(AccountingRule rule, RuleRequest r) {
    rule.setBusinessLine(blankToNull(r.businessLine()));
    rule.setCurrency(blankToNull(r.currency()));
    rule.setPriority(r.priority());
    rule.setEffectiveTo(r.effectiveTo());
    rule.replaceLines(r.lines().stream().map(RuleLineDto::toEntity).toList());
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s;
  }

  /**
   * Simulation result with its balance, so the preview can warn when the real posting would be
   * rejected as unbalanced (e.g. sample amounts that do not add up).
   *
   * @param ruleId rule selected
   * @param ruleName rule name
   * @param lines journal lines that would be generated
   * @param totalDebit sum of the debit lines (event currency)
   * @param totalCredit sum of the credit lines (event currency)
   * @param difference debits minus credits
   * @param balanced true when there are lines and debits equal credits
   */
  public record Simulation(
      Long ruleId,
      String ruleName,
      List<JournalLineRequest> lines,
      BigDecimal totalDebit,
      BigDecimal totalCredit,
      BigDecimal difference,
      boolean balanced) {

    /**
     * Result with the totals of the generated lines.
     *
     * @param ruleId rule selected
     * @param ruleName rule name
     * @param lines generated lines
     * @return simulation
     */
    public static Simulation of(Long ruleId, String ruleName, List<JournalLineRequest> lines) {
      BigDecimal debit = total(lines, BalanceSide.DEBIT);
      BigDecimal credit = total(lines, BalanceSide.CREDIT);
      BigDecimal difference = debit.subtract(credit);
      return new Simulation(
          ruleId,
          ruleName,
          lines,
          debit,
          credit,
          difference,
          debit.signum() > 0 && difference.signum() == 0);
    }

    private static BigDecimal total(List<JournalLineRequest> lines, BalanceSide side) {
      return lines.stream()
          .filter(l -> l.side() == side)
          .map(JournalLineRequest::amount)
          .reduce(Money.zero(), BigDecimal::add)
          .setScale(Money.SCALE, Money.ROUNDING);
    }
  }
}
