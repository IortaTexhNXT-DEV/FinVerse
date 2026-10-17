package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalStatus;
import com.iortatechnxt.brokerverse.period.domain.AccountingPeriod;
import com.iortatechnxt.brokerverse.period.service.PeriodCloseGuard;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Blocks period close while journals dated in the period are still in draft, rejected or pending
 * authorization (end-of-period incomplete transaction check).
 */
@Component
public class PendingJournalCloseGuard implements PeriodCloseGuard {

  private static final Set<JournalStatus> OPEN_STATUSES =
      EnumSet.of(JournalStatus.DRAFT, JournalStatus.PENDING_APPROVAL, JournalStatus.REJECTED);

  private final JournalBatchRepository batches;

  /**
   * Creates the guard.
   *
   * @param batches batch repository
   */
  public PendingJournalCloseGuard(JournalBatchRepository batches) {
    this.batches = batches;
  }

  @Override
  public List<String> blockingIssues(AccountingPeriod period) {
    long open =
        batches.countByCompanyIdAndStatusInAndValueDateBetween(
            period.getCompanyId(), OPEN_STATUSES, period.getStartDate(), period.getEndDate());
    return open == 0
        ? List.of()
        : List.of(
            open + " journal(s) dated in " + period.getName() + " are not yet posted or cancelled");
  }
}
