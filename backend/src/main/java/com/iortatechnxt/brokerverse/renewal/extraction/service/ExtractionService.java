package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.check.service.CheckEngine;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalExtractionRunRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyHeader;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Extraction of the expiring policies as renewal candidates (BRRN.002/005/030; FR-RN-010, 011): the
 * booked root invoices of the Operations ledger and the migrated policy headers whose expiry falls
 * in a range. Each new candidate gets its renewal reference, a work case in stage EXTRACTED and its
 * checks; it does nothing more until it is initiated (BRRN.021). One candidate per expiring invoice
 * or legacy reference: a second run finds them as already extracted. Employee Benefits lines
 * ({@code RNW_EXCLUDED_LINES}) and the years of a multi-year term before the last are skipped. Each
 * candidate is created in its own transaction, so one bad record does not stop the run.
 */
@Service("renewalExtractionService")
public class ExtractionService {

  private final ExpiringPolicies policies;
  private final CandidateFactory factory;
  private final RenewalCandidateRepository candidates;
  private final RenewalExtractionRunRepository runs;
  private final CheckEngine engine;
  private final RenewalFlow flow;
  private final LegacyPolicySource legacy;
  private final RenewalParameters parameters;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param policies expiring invoices
   * @param factory candidate factory
   * @param candidates candidates
   * @param runs extraction runs
   * @param engine check engine
   * @param flow workflow
   * @param legacy migrated policy headers
   * @param parameters renewal parameters
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param transactions transaction manager
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ExtractionService(
      ExpiringPolicies policies,
      CandidateFactory factory,
      RenewalCandidateRepository candidates,
      RenewalExtractionRunRepository runs,
      CheckEngine engine,
      RenewalFlow flow,
      LegacyPolicySource legacy,
      RenewalParameters parameters,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.policies = policies;
    this.factory = factory;
    this.candidates = candidates;
    this.runs = runs;
    this.engine = engine;
    this.flow = flow;
    this.legacy = legacy;
    this.parameters = parameters;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.tx = new TransactionTemplate(transactions);
    this.clock = clock;
  }

  /**
   * Extracts the policies expiring in a range (Generate Expiry List, FR-RN-011): recorded as an
   * extraction with trigger MANUAL_RANGE, never as an initiation.
   *
   * @param companyId company
   * @param from first expiry date
   * @param to last expiry date
   * @return the run
   */
  public ExtractionRun extractRange(Long companyId, LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new BusinessRuleException("RNW_RANGE_REQUIRED", "Enter a valid start and end date");
    }
    if (to.isBefore(from)) {
      throw new BusinessRuleException(
          "RNW_RANGE_INVALID", "The end date must be on or after the start date");
    }
    return extract(companyId, new Window(from, to, ExtractionTrigger.MANUAL_RANGE, i -> true));
  }

  /**
   * The daily extraction (FR-RN-010): the policies whose expiry is the business date plus the lead
   * days of their segment, and any date a failed or missed run left behind (the duplicate guard
   * keeps them unique).
   *
   * @param companyId company
   * @param today business date
   * @return the run
   */
  public ExtractionRun extractDue(Long companyId, LocalDate today) {
    int longest = parameters.allLeadDays().stream().max(Integer::compare).orElse(0);
    Predicate<ExpiringInvoice> due =
        i ->
            !i.facts().expiry().isAfter(today.plusDays(parameters.leadDays(i.product().segment())));
    return extract(
        companyId, new Window(today, today.plusDays(longest), ExtractionTrigger.SCHEDULED, due));
  }

  private ExtractionRun extract(Long companyId, Window window) {
    ExtractionRun run = start(companyId, window.trigger(), window.from(), window.to());
    Tally tally = new Tally();
    List<String> excluded =
        parameters.excludedLines().stream().map(l -> l.toUpperCase(Locale.ROOT)).toList();
    for (ExpiringInvoice invoice : policies.between(companyId, window.from(), window.to())) {
      tally.read++;
      if (invoice.extracted()) {
        tally.existing++;
      } else if (!invoice.lastYear()
          || excluded(excluded, invoice.product().line())
          || !window.due().test(invoice)) {
        tally.skipped++;
      } else {
        createInvoice(companyId, invoice, run.getId());
        tally.created++;
      }
    }
    for (LegacyHeader header : legacy.expiringHeaders(companyId, window.from(), window.to())) {
      tally.read++;
      if (candidates
          .findByCompanyIdAndSourceAndSourceRef(
              companyId, CandidateSource.LEGACY, header.legacyRef())
          .isPresent()) {
        tally.existing++;
      } else {
        createLegacy(companyId, header, run.getId(), false);
        tally.created++;
      }
    }
    return complete(run.getId(), tally.counts());
  }

  private static boolean excluded(List<String> lines, String line) {
    return line != null && lines.contains(line.toUpperCase(Locale.ROOT));
  }

  /**
   * Starts a run in its own transaction.
   *
   * @param companyId company
   * @param trigger trigger
   * @param from first expiry
   * @param to last expiry
   * @return the run
   */
  public ExtractionRun start(
      Long companyId, ExtractionTrigger trigger, LocalDate from, LocalDate to) {
    return tx.execute(
        s ->
            runs.save(
                new ExtractionRun(
                    companyId,
                    numbers.next("RXR-" + BusinessClock.today(clock).getYear()),
                    trigger,
                    from,
                    to,
                    currentUser.username(),
                    clock.instant())));
  }

  /**
   * Completes a run in its own transaction.
   *
   * @param runId run
   * @param counts counts
   * @return the run
   */
  public ExtractionRun complete(Long runId, ExtractionRun.Counts counts) {
    return tx.execute(
        s -> {
          ExtractionRun run = runs.findById(runId).orElseThrow();
          run.complete(counts, clock.instant());
          audit.record(
              RenewalCodes.ENTITY,
              run.getRunNo(),
              AuditAction.CREATE,
              "Extraction "
                  + run.getTrigger()
                  + " "
                  + run.getExpiryFrom()
                  + " to "
                  + run.getExpiryTo()
                  + ": "
                  + counts.summary());
          return run;
        });
  }

  /**
   * Marks a run failed in its own transaction.
   *
   * @param runId run
   * @param reason error
   */
  public void fail(Long runId, String reason) {
    tx.executeWithoutResult(
        s -> runs.findById(runId).ifPresent(r -> r.fail(reason, clock.instant())));
  }

  private void createInvoice(Long companyId, ExpiringInvoice invoice, Long runId) {
    tx.executeWithoutResult(
        s -> {
          RenewalCandidate c = candidates.save(factory.fromInvoice(companyId, invoice, runId));
          flow.start(c);
          engine.run(c, CheckTrigger.EXTRACTION);
        });
  }

  /**
   * Creates the candidate of a migrated policy header in its own transaction.
   *
   * @param companyId company
   * @param header header
   * @param runId run, null for an upload
   * @param urgent flag urgent (go-live window)
   * @return the candidate
   */
  public RenewalCandidate createLegacy(
      Long companyId, LegacyHeader header, Long runId, boolean urgent) {
    return tx.execute(
        s -> {
          RenewalCandidate c = candidates.save(factory.fromLegacy(companyId, header, runId));
          c.getFlags().setUrgent(urgent || header.urgent());
          flow.start(c);
          engine.run(c, CheckTrigger.EXTRACTION);
          return c;
        });
  }

  /** Range, trigger and due-date rule of a run. */
  private record Window(
      LocalDate from, LocalDate to, ExtractionTrigger trigger, Predicate<ExpiringInvoice> due) {}

  /** Counts of a run in progress. */
  private static final class Tally {
    private int read;
    private int created;
    private int existing;
    private int skipped;

    ExtractionRun.Counts counts() {
      return new ExtractionRun.Counts(read, created, existing, skipped, 0);
    }
  }
}
