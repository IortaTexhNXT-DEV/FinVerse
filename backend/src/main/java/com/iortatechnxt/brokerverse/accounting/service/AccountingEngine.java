package com.iortatechnxt.brokerverse.accounting.service;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventType;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventTypeRepository;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingRule;
import com.iortatechnxt.brokerverse.accounting.domain.EventLogEntry;
import com.iortatechnxt.brokerverse.accounting.domain.EventStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalService;
import com.iortatechnxt.brokerverse.period.domain.PeriodModuleLock;
import com.iortatechnxt.brokerverse.period.service.PeriodModuleLockService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event-driven accounting engine: event validation, cut-off of the broking books (FRBS 3.4.0), rule
 * selection, journal generation, cost-centre derivation (FRBS 3.1.1) and posting, with every
 * outcome recorded in the event register.
 */
@Service
public class AccountingEngine implements AccountingEventPublisher {

  private static final int MAX_ERROR = 1000;
  private static final String BROKING_SOURCE_MODULES = "BROKING_SOURCE_MODULES";

  private final AccountingEventTypeRepository eventTypes;
  private final RuleResolver resolver;
  private final JournalLineBuilder lineBuilder;
  private final SystemJournalService journals;
  private final EventLogWriter log;
  private final CostCenterRuleService costCenters;
  private final PeriodModuleLockService bookLocks;
  private final SystemParameterService parameters;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the engine.
   *
   * @param eventTypes event type repository
   * @param resolver rule resolver
   * @param lineBuilder line builder
   * @param journals system journal service
   * @param log event register writer
   * @param costCenters cost-centre rules (FRBS 3.1.1)
   * @param bookLocks cut-off of the broking books (FRBS 3.4.0)
   * @param parameters system parameters (source modules of the broking books)
   * @param currentUser current user
   * @param clock clock
   */
  public AccountingEngine(
      AccountingEventTypeRepository eventTypes,
      RuleResolver resolver,
      JournalLineBuilder lineBuilder,
      SystemJournalService journals,
      EventLogWriter log,
      CostCenterRuleService costCenters,
      PeriodModuleLockService bookLocks,
      SystemParameterService parameters,
      CurrentUser currentUser,
      Clock clock) {
    this.eventTypes = eventTypes;
    this.resolver = resolver;
    this.lineBuilder = lineBuilder;
    this.journals = journals;
    this.log = log;
    this.costCenters = costCenters;
    this.bookLocks = bookLocks;
    this.parameters = parameters;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  @Transactional
  public JournalBatch publish(BusinessEvent event) {
    try {
      AccountingEventType type = requireEventType(event.eventType());
      if (parameters.items(BROKING_SOURCE_MODULES).contains(event.sourceModule())) {
        bookLocks.requireOpen(event.companyId(), event.valueDate(), PeriodModuleLock.BROKING);
      }
      AccountingRule rule = resolver.resolve(event);
      List<JournalLineRequest> lines = costCenters.applyTo(event, lineBuilder.build(rule, event));
      JournalBatch batch =
          journals.post(
              new SystemJournalRequest(
                  event.companyId(),
                  event.branchId(),
                  type.getJournalType(),
                  event.valueDate(),
                  event.currency(),
                  event.narration(),
                  event.reference(),
                  event.sourceModule(),
                  event.sourceReference(),
                  lines));
      log.logPosted(entry(event, EventStatus.POSTED, rule.getId(), batch.getBatchNo(), null));
      return batch;
    } catch (BusinessRuleException ex) {
      log.logFailed(
          entry(event, EventStatus.FAILED, null, null, ex.getCode() + ": " + ex.getMessage()));
      throw ex;
    }
  }

  private AccountingEventType requireEventType(String code) {
    AccountingEventType type =
        eventTypes
            .findById(code)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "UNKNOWN_EVENT_TYPE", "Unknown accounting event " + code));
    if (!type.isActive()) {
      throw new BusinessRuleException(
          "INACTIVE_EVENT_TYPE", "Accounting event " + code + " is inactive");
    }
    return type;
  }

  private EventLogEntry entry(
      BusinessEvent e, EventStatus status, Long ruleId, String batchNo, String error) {
    return new EventLogEntry(
        e.companyId(),
        e.eventType(),
        e.sourceModule(),
        e.sourceReference(),
        e.reference(),
        e.valueDate(),
        status,
        ruleId,
        batchNo,
        error == null || error.length() <= MAX_ERROR ? error : error.substring(0, MAX_ERROR),
        e.amountsText(),
        clock.instant(),
        currentUser.username());
  }
}
