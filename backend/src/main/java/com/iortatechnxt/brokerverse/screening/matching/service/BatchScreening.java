package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRun;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRun.RunStart;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRunRepository;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRunStatus;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListedEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistDirectory;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Screening of many clients (SNSRP-602; FR-SS-030): the delta screening of the in-scope clients
 * against changed watchlist entries (trigger LIST_CHANGE, SNSRP-204 "approval ... triggers a delta
 * screening of the entry") and the batch window (trigger PERIODIC): the entries changed since the
 * last batch, and the whole ACTIVE list on day {@code SCR_FULL_RESCREEN_DAY} of the month or when
 * no batch ran before. Each company is one run in one transaction.
 */
@Service
@Transactional
public class BatchScreening {

  /** Parameter: day of the month of the full rescreen. */
  public static final String FULL_RESCREEN_DAY = "SCR_FULL_RESCREEN_DAY";

  private final ScreeningEngine engine;
  private final NameKeyIndex index;
  private final WatchlistDirectory directory;
  private final ClientService clients;
  private final ScreeningScope scope;
  private final ScreeningRunRepository runs;
  private final SystemParameterService parameters;

  /**
   * Creates the service.
   *
   * @param engine engine
   * @param index blocking keys
   * @param directory watchlist entries
   * @param clients client master (read)
   * @param scope clients in scope
   * @param runs runs
   * @param parameters system parameters
   */
  public BatchScreening(
      ScreeningEngine engine,
      NameKeyIndex index,
      WatchlistDirectory directory,
      ClientService clients,
      ScreeningScope scope,
      ScreeningRunRepository runs,
      SystemParameterService parameters) {
    this.engine = engine;
    this.index = index;
    this.directory = directory;
    this.clients = clients;
    this.scope = scope;
    this.runs = runs;
    this.parameters = parameters;
  }

  /**
   * Rebuilds the keys of changed entries and returns the ACTIVE ones (first step of the delta
   * screening).
   *
   * @param entryIds the entries that changed
   * @return the changed entries still ACTIVE
   */
  public List<ListedEntry> rekeyEntries(Collection<Long> entryIds) {
    List<ListedEntry> entries = directory.entries(entryIds);
    index.rebuildEntries(entries);
    return entries.stream().filter(e -> e.status() == EntryStatus.ACTIVE).toList();
  }

  /**
   * The in-scope clients sharing a key with some entries, by company.
   *
   * @param entries the entries
   * @return client ids by company
   */
  @Transactional(readOnly = true)
  public Map<Long, List<Long>> candidatesByCompany(Collection<ListedEntry> entries) {
    Map<Long, List<Long>> byCompany = new TreeMap<>();
    for (Long id : index.clientsFor(entries)) {
      Client c = clients.get(id);
      if (scope.includes(c)) {
        byCompany.computeIfAbsent(c.getCompanyId(), k -> new ArrayList<>()).add(id);
      }
    }
    return byCompany;
  }

  /**
   * Screens some clients of a company against changed entries (trigger LIST_CHANGE).
   *
   * @param companyId the company
   * @param clientIds the candidate clients
   * @param entries the changed ACTIVE entries
   * @param cause what changed the entries (list change or run reference)
   * @return the result, empty when no criteria are in force
   */
  public Optional<ScreeningResult> screenChange(
      Long companyId, List<Long> clientIds, List<ListedEntry> entries, String cause) {
    Optional<ScreeningRunContext> started =
        engine.start(
            companyId, new RunStart(ScreeningTrigger.LIST_CHANGE, cause, scope.label(), false));
    if (started.isEmpty()) {
      return Optional.empty();
    }
    List<ScreeningSubject> subjects =
        clientIds.stream().map(id -> ScreeningSubject.of(clients.get(id))).toList();
    engine.screen(started.get(), subjects, EntryPool.of(entries), false);
    return Optional.of(engine.finish(started.get(), entries.size()));
  }

  /**
   * The batch run of one company (trigger PERIODIC).
   *
   * @param companyId the company
   * @param businessDate the business date (decides the full rescreen)
   * @param jobRunId the job run, may be {@code null}
   * @return the result, empty when the company has no client in scope or no criteria in force
   */
  public Optional<ScreeningResult> periodic(Long companyId, LocalDate businessDate, Long jobRunId) {
    if (!scope.hasClients(companyId)) {
      return Optional.empty();
    }
    Optional<ScreeningRun> last =
        runs.findFirstByCompanyIdAndTriggerAndStatusOrderByStartedAtDesc(
            companyId, ScreeningTrigger.PERIODIC, ScreeningRunStatus.SUCCESS);
    boolean full =
        last.isEmpty() || businessDate.getDayOfMonth() == parameters.intValue(FULL_RESCREEN_DAY, 1);
    Optional<ScreeningRunContext> started =
        engine.start(
            companyId,
            new RunStart(ScreeningTrigger.PERIODIC, businessDate.toString(), scope.label(), full));
    if (started.isEmpty()) {
      return Optional.empty();
    }
    ScreeningRunContext ctx = started.get();
    ctx.run().linkJob(jobRunId);
    index.ensureEntries();
    List<ListedEntry> entries =
        full
            ? directory.active(List.of())
            : directory.entries(index.entriesChangedSince(last.get().getStartedAt()));
    EntryPool pool = EntryPool.of(entries);
    scope.forEachPage(
        companyId,
        page -> {
          List<ScreeningSubject> subjects = page.stream().map(ScreeningSubject::of).toList();
          index.ensureClients(subjects);
          engine.screen(ctx, subjects, pool, false);
        });
    return Optional.of(engine.finish(ctx, pool.size()));
  }
}
