package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.migration.common.service.LoaderIdentity;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtractRepository;
import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.BatchStatus;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatchRepository;
import com.iortatechnxt.brokerverse.migration.mapping.domain.Layout;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapLoader;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMaps;
import com.iortatechnxt.brokerverse.migration.mapping.service.LayoutService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the load of an approved batch (DATA_MIGRATION_DESIGN section 10; FR-DM-014): the VALID and
 * WARNING rows are grouped into units (a main row with its sub-layout rows), partitioned by the
 * loader's partition key and loaded in chunks of {@code MIG_CHUNK_SIZE} units, {@code
 * MIG_PARTITIONS} partitions in parallel, one transaction per chunk, as the system user {@code
 * mig-loader}. A failing chunk is retried unit by unit so that one bad unit fails alone. A unit
 * already loaded with the same hash is SKIPPED; a changed one is updated where the loader allows it
 * and otherwise REJECTED.
 */
@Service
public class LoadRunner {

  private static final Logger LOG = LoggerFactory.getLogger(LoadRunner.class);
  private static final String STEP = "LOAD";
  private static final String CHANGED = "MIG_CHANGED_AFTER_LOAD";
  private static final String NOT_VALID = "Not loaded: the row has data-quality errors";

  private final MigBatchRepository batches;
  private final MigExtractRepository extracts;
  private final BatchPlanService plans;
  private final LayoutService layouts;
  private final CodeMapLoader maps;
  private final LoaderRegistry loaders;
  private final XrefService xrefs;
  private final RowStatusWriter statuses;
  private final LoadCompletion completion;
  private final MigrationParameters parameters;
  private final CurrentUser currentUser;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the runner.
   *
   * @param batches batches
   * @param extracts extracts
   * @param plans batch reads
   * @param layouts layouts
   * @param maps code maps
   * @param loaders loaders
   * @param xrefs cross-references
   * @param statuses row status writer
   * @param completion end of the load
   * @param parameters chunk size and partitions
   * @param currentUser current user
   * @param txManager transactions
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public LoadRunner(
      MigBatchRepository batches,
      MigExtractRepository extracts,
      BatchPlanService plans,
      LayoutService layouts,
      CodeMapLoader maps,
      LoaderRegistry loaders,
      XrefService xrefs,
      RowStatusWriter statuses,
      LoadCompletion completion,
      MigrationParameters parameters,
      CurrentUser currentUser,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.batches = batches;
    this.extracts = extracts;
    this.plans = plans;
    this.layouts = layouts;
    this.maps = maps;
    this.loaders = loaders;
    this.xrefs = xrefs;
    this.statuses = statuses;
    this.completion = completion;
    this.parameters = parameters;
    this.currentUser = currentUser;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  /**
   * Loads an approved batch.
   *
   * @param batchNo batch
   * @return the batch after the load and its reconciliation
   */
  public MigBatch load(String batchNo) {
    String operator = currentUser.username();
    Prepared prepared = tx.execute(s -> prepare(batchNo, operator));
    if (prepared == null) {
      throw new IllegalStateException("The load of " + batchNo + " could not start");
    }
    boolean failed = false;
    try {
      runPartitions(prepared);
    } catch (RuntimeException e) {
      failed = true;
      LOG.error("Load of batch {} stopped", batchNo, e);
      completion.logError(prepared.batchId(), "The load stopped: " + e.getMessage());
    }
    return completion.finish(prepared.batchId(), failed);
  }

  private Prepared prepare(String batchNo, String operator) {
    MigBatch batch = plans.get(batchNo);
    MigrationLoader loader = loaders.require(batch.getObjectCode());
    if (batches.existsByCompanyIdAndObjectCodeAndStatusIn(
            batch.getCompanyId(), batch.getObjectCode(), EnumSet.of(BatchStatus.LOADING))
        && batch.getStatus() != BatchStatus.LOADING) {
      throw new BusinessRuleException(
          "MIG_LOAD_RUNNING", "Another batch of object " + batch.getObjectCode() + " is loading");
    }
    batch.startLoad(operator, clock.instant());
    plans
        .rowsByLayout(batch, EnumSet.of(RowStatus.INVALID))
        .values()
        .forEach(list -> list.forEach(r -> r.finish(RowStatus.REJECTED, NOT_VALID)));
    Map<String, List<StageRow>> rows =
        plans.rowsByLayout(batch, EnumSet.of(RowStatus.VALID, RowStatus.WARNING));
    Map<Long, String> sources =
        extracts.findAllById(batch.getExtractIds()).stream()
            .collect(Collectors.toMap(MigExtract::getId, MigExtract::getSourceSystem));
    List<LoadUnit> units = loader.group(units(batch.getObjectCode(), loader, rows, sources));
    CodeMaps codeMaps = maps.approved(batch.getMapVersions().keySet());
    return new Prepared(batch.getId(), batch.getObjectCode(), units, codeMaps);
  }

  private List<LoadUnit> units(
      String objectCode,
      MigrationLoader loader,
      Map<String, List<StageRow>> rows,
      Map<Long, String> sources) {
    String main = mainLayout(objectCode, rows);
    List<String> keys = layouts.current(main).map(Layout::keys).orElse(List.of());
    List<String> parentKeys = keys.isEmpty() ? List.of() : List.of(keys.get(0));
    Map<String, Map<String, List<StageRow>>> children = new LinkedHashMap<>();
    for (String child : loader.childLayouts()) {
      for (StageRow r : rows.getOrDefault(child, List.of())) {
        String parent = key(r.getRawPayload(), parentKeys);
        children
            .computeIfAbsent(parent, k -> new LinkedHashMap<>())
            .computeIfAbsent(child, k -> new ArrayList<>())
            .add(r);
      }
    }
    List<LoadUnit> out = new ArrayList<>();
    for (StageRow r : rows.getOrDefault(main, List.of())) {
      String parent = key(r.getRawPayload(), parentKeys);
      Map<String, List<StageRow>> kids = children.getOrDefault(parent, Map.of());
      out.add(
          new LoadUnit(
              r.getLegacyKey(),
              sources.getOrDefault(r.getExtractId(), ""),
              r,
              kids,
              unitHash(r, kids)));
    }
    return out;
  }

  private String mainLayout(String objectCode, Map<String, List<StageRow>> rows) {
    if (rows.containsKey(objectCode) || layouts.current(objectCode).isPresent()) {
      return objectCode;
    }
    return rows.keySet().stream().findFirst().orElse(objectCode);
  }

  private static String key(Map<String, String> values, List<String> cols) {
    return cols.stream().map(c -> values.getOrDefault(c, "")).collect(Collectors.joining("|"));
  }

  /**
   * The hash of a unit: the row hashes of its rows in order.
   *
   * @param main main row
   * @param kids child rows
   * @return hash
   */
  static String unitHash(StageRow main, Map<String, List<StageRow>> kids) {
    StringBuilder all = new StringBuilder(main.getRowHash());
    kids.keySet().stream()
        .sorted()
        .forEach(k -> kids.get(k).forEach(r -> all.append(r.getRowHash())));
    return kids.isEmpty()
        ? main.getRowHash()
        : Sha256.hex(all.toString().getBytes(StandardCharsets.UTF_8));
  }

  private void runPartitions(Prepared p) {
    MigrationLoader loader = loaders.require(p.objectCode());
    int partitions = parameters.partitions();
    int chunk = parameters.chunkSize();
    Map<Integer, List<LoadUnit>> byPartition = new LinkedHashMap<>();
    for (LoadUnit u : p.units()) {
      int part = Math.floorMod(loader.partitionKey(u).hashCode(), partitions);
      byPartition.computeIfAbsent(part, k -> new ArrayList<>()).add(u);
    }
    ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (List<LoadUnit> part : byPartition.values()) {
        futures.add(
            pool.submit(() -> LoaderIdentity.run(() -> runPartition(p, loader, part, chunk))));
      }
      for (Future<?> f : futures) {
        f.get();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("The load was interrupted", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException(e.getCause().getMessage(), e.getCause());
    } finally {
      pool.shutdown();
    }
  }

  private void runPartition(Prepared p, MigrationLoader loader, List<LoadUnit> part, int chunk) {
    for (int from = 0; from < part.size(); from += chunk) {
      List<LoadUnit> units = part.subList(from, Math.min(part.size(), from + chunk));
      try {
        tx.executeWithoutResult(s -> units.forEach(u -> loadUnit(p, loader, u)));
      } catch (RuntimeException chunkFailure) {
        LOG.info("Chunk of batch {} failed; retrying its units one by one", p.batchId());
        units.forEach(u -> retry(p, loader, u));
      }
    }
  }

  private void retry(Prepared p, MigrationLoader loader, LoadUnit u) {
    try {
      tx.executeWithoutResult(s -> loadUnit(p, loader, u));
    } catch (RuntimeException e) {
      String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      tx.executeWithoutResult(s -> statuses.finish(u.allRows(), RowStatus.REJECTED, message));
    }
  }

  private void loadUnit(Prepared p, MigrationLoader loader, LoadUnit u) {
    MigBatch batch = batches.findById(p.batchId()).orElseThrow();
    LoadContext ctx = new LoadContext(batch, p.maps(), BusinessClock.today(clock));
    Optional<KeyXref> existing =
        xrefs.find(batch.getCompanyId(), u.sourceSystem(), p.objectCode(), u.legacyKey());
    if (existing.isPresent() && existing.get().isLive()) {
      reload(batch, loader, u, existing.get(), ctx);
    } else {
      LoadOutcome outcome = loader.load(u, ctx);
      KeyXref x =
          existing.orElseGet(
              () ->
                  new KeyXref(
                      batch.getCompanyId(), u.sourceSystem(), p.objectCode(), u.legacyKey()));
      record(batch, u, x, outcome);
    }
  }

  /** A key loaded before: unchanged rows are skipped, changed rows update or are rejected. */
  private void reload(
      MigBatch batch, MigrationLoader loader, LoadUnit u, KeyXref x, LoadContext ctx) {
    if (x.getRowHash().equals(u.hash())) {
      statuses.finish(u.allRows(), RowStatus.SKIPPED, "Already loaded as " + x.getTargetCode());
    } else {
      Optional<LoadOutcome> updated = loader.update(u, x, ctx);
      if (updated.isPresent()) {
        record(batch, u, x, updated.get());
      } else {
        statuses.finish(
            u.allRows(),
            RowStatus.REJECTED,
            CHANGED + ": the record was loaded before with other values");
      }
    }
  }

  private void record(MigBatch batch, LoadUnit u, KeyXref x, LoadOutcome outcome) {
    x.loaded(outcome.target(), batch.getId(), u.hash(), clock.instant());
    xrefs.save(x);
    for (LoadUnit.Alias alias : u.aliases()) {
      KeyXref ax =
          xrefs
              .find(
                  batch.getCompanyId(),
                  alias.sourceSystem(),
                  batch.getObjectCode(),
                  alias.legacyKey())
              .orElseGet(
                  () ->
                      new KeyXref(
                          batch.getCompanyId(),
                          alias.sourceSystem(),
                          batch.getObjectCode(),
                          alias.legacyKey()));
      ax.loaded(outcome.target(), batch.getId(), alias.rowHash(), clock.instant());
      xrefs.save(ax);
    }
    statuses.loaded(u.allRows(), outcome.target(), clock.instant());
  }

  private record Prepared(Long batchId, String objectCode, List<LoadUnit> units, CodeMaps maps) {}
}
