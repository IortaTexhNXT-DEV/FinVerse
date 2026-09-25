package com.iortatechnxt.brokerverse.screening.watchlist.service;

import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRun;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionTrigger;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.RunStatus;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.SourceTransport;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSource;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.WatchlistSourceRepository;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code SCR_WATCHLIST_INGEST} (SNSRP-201; cron {@code brokerverse.jobs.scr-watchlist-ingest-cron},
 * 01:00 PHT): reads every active FILE source through its {@link WatchlistFeed} and applies the
 * official list at once. A source without a new file gets a FAILED run ("No list file was
 * received"), which raises {@code SCR_INGEST_FAILED}. Each file is one transaction.
 */
@Component
public class WatchlistIngestJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "SCR_WATCHLIST_INGEST";

  private final WatchlistSourceRepository sources;
  private final WatchlistFeed feed;
  private final WatchlistIngestionService ingestion;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param sources sources
   * @param feed list feed port
   * @param ingestion ingestion
   * @param txManager transactions
   * @param cron schedule
   */
  public WatchlistIngestJob(
      WatchlistSourceRepository sources,
      WatchlistFeed feed,
      WatchlistIngestionService ingestion,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.scr-watchlist-ingest-cron:-}") String cron) {
    this.sources = sources;
    this.feed = feed;
    this.ingestion = ingestion;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Reads the sanctions / PEP list files of the active sources and logs each run (SNSRP-201)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int runs = 0;
    int failed = 0;
    for (WatchlistSource source :
        sources.findByActiveTrueAndTransportOrderByCodeAsc(SourceTransport.FILE)) {
      for (IngestionRun run : read(source)) {
        runs++;
        if (run.getStatus() == RunStatus.FAILED) {
          failed++;
        }
      }
    }
    return new JobOutcome(runs, runs + " list run(s), " + failed + " failed");
  }

  private List<IngestionRun> read(WatchlistSource source) {
    List<WatchlistFeed.FeedFile> files = tx.execute(s -> feed.pending(source));
    if (files == null || files.isEmpty()) {
      IngestionRun missing = tx.execute(s -> ingestion.missingFile(source));
      return missing == null ? List.of() : List.of(missing);
    }
    return files.stream()
        .map(f -> tx.execute(s -> ingestion.ingest(source, IngestionTrigger.SCHEDULED, f, false)))
        .filter(Objects::nonNull)
        .toList();
  }
}
