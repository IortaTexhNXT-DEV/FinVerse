package com.iortatechnxt.finverse.accounting.service;

import com.iortatechnxt.finverse.accounting.domain.AccountingEventType;
import com.iortatechnxt.finverse.accounting.domain.AccountingEventTypeRepository;
import com.iortatechnxt.finverse.accounting.domain.AccountingRule;
import com.iortatechnxt.finverse.accounting.domain.EventLogEntry;
import com.iortatechnxt.finverse.accounting.domain.EventStatus;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.finverse.journal.service.SystemJournalService;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event-driven accounting engine: event validation, rule selection, journal generation and posting,
 * with every outcome recorded in the event register.
 */
@Service
public class AccountingEngine implements AccountingEventPublisher {

  private static final int MAX_ERROR = 1000;

  private final AccountingEventTypeRepository eventTypes;
  private final RuleResolver resolver;
  private final JournalLineBuilder lineBuilder;
  private final SystemJournalService journals;
  private final EventLogWriter log;
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
   * @param currentUser current user
   * @param clock clock
   */
  public AccountingEngine(
      AccountingEventTypeRepository eventTypes,
      RuleResolver resolver,
      JournalLineBuilder lineBuilder,
      SystemJournalService journals,
      EventLogWriter log,
      CurrentUser currentUser,
      Clock clock) {
    this.eventTypes = eventTypes;
    this.resolver = resolver;
    this.lineBuilder = lineBuilder;
    this.journals = journals;
    this.log = log;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Override
  @Transactional
  public JournalBatch publish(BusinessEvent event) {
    try {
      AccountingEventType type = requireEventType(event.eventType());
      AccountingRule rule = resolver.resolve(event);
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
                  lineBuilder.build(rule, event)));
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
