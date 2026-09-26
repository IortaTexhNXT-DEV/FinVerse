package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.ConfigVersionRef;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchSuppressionRepository;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatch;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatchRepository;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRun;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRun.RunStart;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRunRepository;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskProfiler;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.EntryStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.service.ListedEntry;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistDirectory;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The screening engine (SNSRP-301, 302, 602; FR-SS-030 to 033): starts a logged run with the
 * matching criteria and risk rules in force, scores each client in scope against the candidate
 * watchlist entries (blocking keys, then {@link PairMatcher}), records new matches only (one per
 * client, entry version and configuration version; suppressed pairs skipped), evaluates the risk
 * rules through {@link RiskProfiler}, ends the run and publishes {@link ScreeningCompleted}.
 *
 * <p>Without an ACTIVE matching-criteria version the run is not started and the alert {@code
 * SCR_NO_ACTIVE_CONFIG} is raised (FR-SS-030 alternate flow).
 */
@Service
@Transactional
public class ScreeningEngine {

  /** Alert raised when a trigger fires without matching criteria in force. */
  public static final String NO_CONFIG_ALERT = "SCR_NO_ACTIVE_CONFIG";

  /** Entity type of runs in alerts and audit. */
  public static final String RUN_ENTITY = "ScreeningRun";

  private final ActiveConfig config;
  private final ClientService clients;
  private final NameKeyIndex index;
  private final WatchlistDirectory directory;
  private final ScreeningRunRepository runs;
  private final ScreeningMatchRepository matches;
  private final MatchSuppressionRepository suppressions;
  private final RiskProfiler risk;
  private final DocumentNumberService numbers;
  private final AlertService alerts;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Creates the engine.
   *
   * @param config active configuration
   * @param clients client master (read)
   * @param index blocking keys
   * @param directory watchlist entries
   * @param runs runs
   * @param matches matches
   * @param suppressions false-positive suppressions
   * @param risk risk profiling
   * @param numbers run numbers
   * @param alerts alerts
   * @param events event publisher
   * @param clock clock
   */
  public ScreeningEngine(
      ActiveConfig config,
      ClientService clients,
      NameKeyIndex index,
      WatchlistDirectory directory,
      ScreeningRunRepository runs,
      ScreeningMatchRepository matches,
      MatchSuppressionRepository suppressions,
      RiskProfiler risk,
      DocumentNumberService numbers,
      AlertService alerts,
      ApplicationEventPublisher events,
      Clock clock) {
    this.config = config;
    this.clients = clients;
    this.index = index;
    this.directory = directory;
    this.runs = runs;
    this.matches = matches;
    this.suppressions = suppressions;
    this.risk = risk;
    this.numbers = numbers;
    this.alerts = alerts;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Screens one client against the whole ACTIVE list (triggers client registered, client changed,
   * account submitted and manual). Rebuilds the client's keys first.
   *
   * @param clientId the client
   * @param trigger the trigger
   * @param reference the trigger's reference, {@code null} for the client code
   * @return the result, empty when the client is inactive or no criteria are in force
   */
  public Optional<ScreeningResult> screenClient(
      Long clientId, ScreeningTrigger trigger, String reference) {
    Client client = clients.get(clientId);
    if (client.getStatus() == ClientStatus.INACTIVE) {
      return Optional.empty();
    }
    ScreeningSubject subject = ScreeningSubject.of(client);
    index.rebuildClient(subject);
    String ref = reference == null ? subject.code() : reference;
    Optional<ScreeningRunContext> started =
        start(subject.companyId(), new RunStart(trigger, ref, "Client " + subject.code(), false));
    if (started.isEmpty()) {
      return Optional.empty();
    }
    index.ensureEntries();
    List<ListedEntry> entries =
        directory.entries(index.entriesFor(subject)).stream()
            .filter(e -> e.status() == EntryStatus.ACTIVE)
            .toList();
    ScreeningRunContext ctx = started.get();
    screen(ctx, List.of(subject), EntryPool.of(entries), true);
    return Optional.of(finish(ctx, entries.size()));
  }

  /**
   * Starts a run for a company, unless no matching criteria are in force (then the alert {@code
   * SCR_NO_ACTIVE_CONFIG} is raised).
   *
   * @param companyId the company
   * @param start trigger, reference and scope
   * @return the run context, empty when no criteria are in force
   */
  public Optional<ScreeningRunContext> start(Long companyId, RunStart start) {
    LocalDate today = LocalDate.now(clock);
    Optional<ConfigVersionRef> criteria =
        config.activeVersion(companyId, ConfigType.MATCH_CRITERIA, null, today);
    if (criteria.isEmpty()) {
      alerts.raise(
          NO_CONFIG_ALERT,
          new AlertFacts(
              companyId,
              null,
              RUN_ENTITY,
              start.trigger().name(),
              "Screening "
                  + start.trigger().name()
                  + " ("
                  + start.reference()
                  + ") not started: no ACTIVE matching criteria",
              null,
              NO_CONFIG_ALERT + ":" + companyId));
      return Optional.empty();
    }
    Long riskVersionId =
        config
            .activeVersion(companyId, ConfigType.RISK_RULES, null, today)
            .map(ConfigVersionRef::id)
            .orElse(null);
    ScreeningRun run =
        runs.save(
            new ScreeningRun(
                numbers.next("SCN-" + today.getYear()),
                companyId,
                start,
                criteria.get().id(),
                riskVersionId,
                clock.instant()));
    return Optional.of(
        new ScreeningRunContext(run, config.matchCriteria(criteria.get().id()), riskVersionId));
  }

  /**
   * Screens clients against a pool of entries and evaluates their risk rules.
   *
   * @param ctx the run
   * @param subjects the clients
   * @param pool the entries
   * @param evaluateAll true to evaluate the risk rules of every client (single-client triggers);
   *     false to evaluate only the clients with a new match (batch)
   */
  public void screen(
      ScreeningRunContext ctx,
      Collection<ScreeningSubject> subjects,
      EntryPool pool,
      boolean evaluateAll) {
    int newMatches = 0;
    int changes = 0;
    for (ScreeningSubject subject : subjects) {
      int found = pool.isEmpty() ? 0 : matchOne(ctx, subject, pool);
      newMatches += found;
      if (evaluateAll || found > 0) {
        changes += evaluate(ctx, subject);
      }
    }
    ctx.run().count(subjects.size(), newMatches, changes);
  }

  private int matchOne(ScreeningRunContext ctx, ScreeningSubject subject, EntryPool pool) {
    int found = 0;
    for (ListedEntry entry : pool.candidates(subject)) {
      if (suppressions.existsByClientIdAndEntryIdAndEntryVersion(
          subject.clientId(), entry.id(), entry.entryVersion())) {
        continue;
      }
      Optional<PairMatcher.Hit> hit = PairMatcher.match(subject, entry, ctx.criteria());
      if (hit.isPresent() && !recorded(ctx, subject, entry)) {
        ctx.addMatch(ScreenedMatch.from(matches.save(match(ctx, subject, hit.get()))));
        found++;
      }
    }
    return found;
  }

  private boolean recorded(ScreeningRunContext ctx, ScreeningSubject subject, ListedEntry entry) {
    return matches.existsByClientIdAndEntryIdAndEntryVersionAndMatchVersionId(
        subject.clientId(), entry.id(), entry.entryVersion(), ctx.run().getMatchVersionId());
  }

  private static ScreeningMatch match(
      ScreeningRunContext ctx, ScreeningSubject subject, PairMatcher.Hit hit) {
    ListedEntry e = hit.entry();
    return new ScreeningMatch(
        ctx.run().getId(),
        new ScreeningMatch.MatchPair(
            subject.companyId(),
            subject.clientId(),
            subject.code(),
            subject.displayName(),
            e.id(),
            e.entryVersion(),
            e.primaryName(),
            e.sourceCode(),
            e.listType(),
            e.entityType()),
        new ScreeningMatch.MatchScore(
            ctx.run().getMatchVersionId(),
            hit.rule().id(),
            hit.score(),
            hit.rule().algorithm(),
            hit.fields(),
            hit.reachesCase()));
  }

  private int evaluate(ScreeningRunContext ctx, ScreeningSubject subject) {
    return risk.evaluate(subject, ctx.riskVersionId(), ctx.run().getId(), ctx.runNo())
        .map(
            o -> {
              ctx.addOutcome(o);
              return o.changed() ? 1 : 0;
            })
        .orElse(0);
  }

  /**
   * Adds the cases the case wave opened from a run's results to the run log (FR-SS-030 "cases
   * opened").
   *
   * @param runId the run
   * @param count cases opened
   */
  public void recordCasesOpened(Long runId, int count) {
    runs.findById(runId).ifPresent(r -> r.casesOpened(count));
  }

  /**
   * Ends a run, writes its counts and publishes {@link ScreeningCompleted}.
   *
   * @param ctx the run
   * @param entriesScreened the number of entries screened
   * @return the result
   */
  public ScreeningResult finish(ScreeningRunContext ctx, int entriesScreened) {
    ScreeningRun run = ctx.run();
    run.complete(entriesScreened, clock.instant());
    ScreeningResult result =
        new ScreeningResult(
            run.getId(),
            run.getRunNo(),
            run.getCompanyId(),
            run.getTrigger(),
            run.getReference(),
            run.getMatchVersionId(),
            run.getRiskVersionId(),
            run.getClientsScreened(),
            entriesScreened,
            ctx.matches(),
            ctx.outcomes());
    events.publishEvent(new ScreeningCompleted(result));
    return result;
  }
}
