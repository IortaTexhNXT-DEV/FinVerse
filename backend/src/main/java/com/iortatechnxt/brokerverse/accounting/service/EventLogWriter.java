package com.iortatechnxt.brokerverse.accounting.service;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventLog;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventLogRepository;
import com.iortatechnxt.brokerverse.accounting.domain.EventLogEntry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the event register. Successful events are logged in the business transaction; failures are
 * logged in a separate transaction so they survive the rollback of the business operation.
 */
@Component
public class EventLogWriter {

  private final AccountingEventLogRepository repository;

  /**
   * Creates the writer.
   *
   * @param repository repository
   */
  public EventLogWriter(AccountingEventLogRepository repository) {
    this.repository = repository;
  }

  /**
   * Logs within the current transaction.
   *
   * @param entry entry
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void logPosted(EventLogEntry entry) {
    repository.save(new AccountingEventLog(entry));
  }

  /**
   * Logs in an independent transaction.
   *
   * @param entry entry
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logFailed(EventLogEntry entry) {
    repository.save(new AccountingEventLog(entry));
  }
}
