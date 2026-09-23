package com.iortatechnxt.finverse.journal.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalLine;
import com.iortatechnxt.finverse.ledger.domain.LedgerEntry;
import com.iortatechnxt.finverse.ledger.domain.LedgerEntryRepository;
import com.iortatechnxt.finverse.ledger.domain.LedgerEntryValues;
import com.iortatechnxt.finverse.ledger.service.LedgerBalanceStore;
import com.iortatechnxt.finverse.period.domain.AccountingPeriod;
import com.iortatechnxt.finverse.period.service.PeriodService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posting engine: turns an authorized journal into immutable ledger entries and balance updates.
 *
 * <p>Runs inside the caller's transaction, so ledger entries, balances, the batch status and the
 * audit record commit together or not at all (no partial posting).
 */
@Service
public class PostingService {

  private final PeriodService periods;
  private final LedgerEntryRepository ledger;
  private final LedgerBalanceStore balances;
  private final AuditTrailService audit;
  private final Clock clock;
  private final Counter postedCounter;
  private final List<JournalPostingListener> listeners;

  /**
   * Creates the engine.
   *
   * @param periods period service
   * @param ledger ledger repository
   * @param balances balance store
   * @param audit audit trail
   * @param clock clock
   * @param meters metrics registry
   * @param listeners post-posting listeners (alert rules)
   */
  public PostingService(
      PeriodService periods,
      LedgerEntryRepository ledger,
      LedgerBalanceStore balances,
      AuditTrailService audit,
      Clock clock,
      MeterRegistry meters,
      List<JournalPostingListener> listeners) {
    this.periods = periods;
    this.ledger = ledger;
    this.balances = balances;
    this.audit = audit;
    this.clock = clock;
    this.postedCounter =
        Counter.builder("finverse.journals.posted")
            .description("Journal batches posted to the general ledger")
            .register(meters);
    this.listeners = List.copyOf(listeners);
  }

  /**
   * Posts an authorized batch.
   *
   * @param batch batch in PENDING_APPROVAL with authorizer recorded
   * @param poster user recorded as poster
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void post(JournalBatch batch, String poster) {
    AccountingPeriod period =
        periods.requirePostingPeriod(
            batch.getCompanyId(), batch.getValueDate(), batch.getJournalType().isPrivileged());
    Instant now = clock.instant();
    for (JournalLine line : batch.getLines()) {
      LedgerEntryValues values = toEntry(batch, line, period.getId(), now, poster);
      ledger.save(new LedgerEntry(values));
      balances.apply(values);
    }
    batch.markPosted(period.getId(), now);
    postedCounter.increment();
    audit.record(
        "JournalBatch",
        batch.getBatchNo(),
        AuditAction.POST,
        "Posted " + batch.getJournalType() + " journal, total " + batch.getTotalDebit());
    listeners.forEach(l -> l.onPosted(batch));
  }

  private static LedgerEntryValues toEntry(
      JournalBatch batch, JournalLine line, Long periodId, Instant now, String poster) {
    boolean debit = line.getSide() == BalanceSide.DEBIT;
    BigDecimal zero = Money.zero();
    return new LedgerEntryValues(
        batch.getCompanyId(),
        line.getBranchId(),
        line.getAccount().getId(),
        periodId,
        batch.getValueDate(),
        batch.getId(),
        batch.getBatchNo(),
        line.getLineNo(),
        batch.getJournalType().name(),
        line.getCurrency(),
        debit ? line.getAmount() : zero,
        debit ? zero : line.getAmount(),
        debit ? line.getBaseAmount() : zero,
        debit ? zero : line.getBaseAmount(),
        line.getCostCenter(),
        line.getBusinessLine(),
        line.getPartyCode(),
        line.getReference() != null ? line.getReference() : batch.getReference(),
        line.getNarration() != null ? line.getNarration() : batch.getNarration(),
        now,
        poster);
  }
}
