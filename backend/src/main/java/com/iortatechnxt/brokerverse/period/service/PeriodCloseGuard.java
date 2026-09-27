package com.iortatechnxt.brokerverse.period.service;

import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import java.util.List;

/**
 * Pre-close check contributed by another module (e.g. "no journals pending authorization").
 *
 * <p>Every Spring bean implementing this interface is consulted before a period is closed; any
 * returned message blocks the close. This keeps the period module independent of its consumers.
 */
public interface PeriodCloseGuard {

  /**
   * Returns blocking issues for closing a period.
   *
   * @param period period to close
   * @return blocking messages; empty when the period may close
   */
  List<String> blockingIssues(AccountingPeriod period);
}
