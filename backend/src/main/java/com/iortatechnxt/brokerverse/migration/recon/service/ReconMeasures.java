package com.iortatechnxt.brokerverse.migration.recon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.intake.service.ControlFile;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.recon.domain.ReconLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The measures of a batch reconciliation (DATA_MIGRATION_DESIGN section 12): L1 row counts of the
 * control file, staging and processing, L2 amount totals of the control file, staging and BIBS, L3
 * hash totals and cross-reference keys, L4 fields of the loaded records read back from BIBS.
 */
@Component
class ReconMeasures {

  private static final String L1 = "L1";
  private static final String L2 = "L2";
  private static final String L3 = "L3";
  private static final String L4 = "L4";
  private static final int MAX_DETAIL_KEYS = 10;
  private static final Set<RowStatus> LOADED = EnumSet.of(RowStatus.LOADED, RowStatus.SKIPPED);

  private final ObjectMapper json;

  ReconMeasures(ObjectMapper json) {
    this.json = json;
  }

  /**
   * The lines L1 to L4 of a batch.
   *
   * @param batch batch
   * @param ext its extracts
   * @param all its rows (and those of its reruns)
   * @param loader loader of the object, null when none
   * @param loaded live cross-references of the batch
   * @return lines
   */
  List<ReconLineSpec> lines(
      MigBatch batch,
      List<MigExtract> ext,
      List<StageRow> all,
      MigrationLoader loader,
      List<KeyXref> loaded) {
    List<ReconLineSpec> specs = new ArrayList<>();
    counts(batch, ext, all, specs);
    amounts(ext, all, loader, loaded, specs);
    hashes(batch, all, loaded, specs);
    if (loader != null) {
      fields(all, loader, loaded, specs);
    }
    return specs;
  }

  private static void counts(
      MigBatch batch, List<MigExtract> ext, List<StageRow> all, List<ReconLineSpec> specs) {
    for (MigExtract e : ext) {
      BigDecimal declared =
          e.getDeclaredRows() == null ? null : BigDecimal.valueOf(e.getDeclaredRows());
      specs.add(
          new ReconLineSpec(
              L1,
              "Rows received " + e.getFileName(),
              null,
              ReconLine.Values.of(
                  declared,
                  BigDecimal.valueOf(e.getParsedRows()),
                  BigDecimal.valueOf(e.getStagedRows())),
              e.getExtractNo()));
    }
    Map<RowStatus, Integer> by = new EnumMap<>(RowStatus.class);
    all.forEach(r -> by.merge(r.getStatus(), 1, Integer::sum));
    int processed =
        by.getOrDefault(RowStatus.LOADED, 0)
            + by.getOrDefault(RowStatus.SKIPPED, 0)
            + by.getOrDefault(RowStatus.REJECTED, 0)
            + by.getOrDefault(RowStatus.EXCLUDED, 0)
            + by.getOrDefault(RowStatus.ROLLED_BACK, 0);
    specs.add(
        new ReconLineSpec(
            L1,
            "Loaded + skipped + rejected + excluded = staged",
            null,
            ReconLine.Values.of(
                BigDecimal.valueOf(all.size()),
                BigDecimal.valueOf(all.size()),
                BigDecimal.valueOf(processed)),
            "Batch " + batch.getBatchNo() + ": " + by));
  }

  private void amounts(
      List<MigExtract> ext,
      List<StageRow> all,
      MigrationLoader loader,
      List<KeyXref> loaded,
      List<ReconLineSpec> specs) {
    for (MigExtract e : ext) {
      List<StageRow> ofExtract =
          all.stream().filter(r -> r.getExtractId().equals(e.getId())).toList();
      for (ControlFile.AmountTotal total : totals(e)) {
        BigDecimal staged = sum(ofExtract, total, false);
        BigDecimal target =
            loader == null
                ? null
                : loader
                    .targetTotal(
                        new MigrationLoader.AmountMeasure(
                            e.getLayoutCode(),
                            total.column(),
                            total.filterColumn(),
                            total.filterValue(),
                            total.currency()),
                        loaded)
                    .orElse(sum(ofExtract, total, true));
        specs.add(
            new ReconLineSpec(
                L2,
                e.getLayoutCode() + " " + total.label(),
                total.currency(),
                ReconLine.Values.of(total.value(), staged, target),
                e.getExtractNo()));
      }
    }
  }

  private List<ControlFile.AmountTotal> totals(MigExtract e) {
    if (e.getControlTotals() == null || e.getControlTotals().isBlank()) {
      return List.of();
    }
    try {
      return json.readValue(e.getControlTotals(), new TypeReference<>() {});
    } catch (JsonProcessingException ex) {
      return List.of();
    }
  }

  private static BigDecimal sum(
      List<StageRow> rows, ControlFile.AmountTotal total, boolean loadedOnly) {
    BigDecimal sum = BigDecimal.ZERO;
    for (StageRow r : rows) {
      Map<String, String> v = r.getRawPayload();
      boolean filter =
          total.filterColumn() == null
              || total.filterValue().equalsIgnoreCase(v.getOrDefault(total.filterColumn(), ""));
      boolean currency =
          total.currency() == null
              || !v.containsKey("currency")
              || total.currency().equalsIgnoreCase(v.get("currency"));
      if (filter && currency && (!loadedOnly || LOADED.contains(r.getStatus()))) {
        sum = sum.add(Values.amount(v.get(total.column())));
      }
    }
    return sum;
  }

  private static void hashes(
      MigBatch batch, List<StageRow> all, List<KeyXref> loaded, List<ReconLineSpec> specs) {
    Set<String> keys = new HashSet<>();
    all.stream()
        .filter(
            r ->
                r.getLayoutCode().equals(batch.getObjectCode())
                    || all.stream().noneMatch(x -> x.getLayoutCode().equals(batch.getObjectCode())))
        .filter(r -> r.getStatus() == RowStatus.LOADED)
        .forEach(r -> keys.add(r.getLegacyKey()));
    Set<String> xref = new HashSet<>();
    loaded.forEach(x -> xref.add(x.getLegacyKey()));
    Set<String> missing = new HashSet<>(keys);
    missing.removeAll(xref);
    specs.add(
        new ReconLineSpec(
            L3,
            "Keys loaded = keys in the cross-reference",
            null,
            ReconLine.Values.of(
                null,
                BigDecimal.valueOf(keys.size()),
                BigDecimal.valueOf(keys.size() - missing.size())),
            missing.isEmpty()
                ? null
                : "Missing: " + missing.stream().limit(MAX_DETAIL_KEYS).toList()));
  }

  /**
   * The entry of each target record that carries its values: the first key loaded into it (legacy
   * records merged into one target keep the survivor's values).
   */
  private static List<KeyXref> survivors(List<KeyXref> loaded) {
    Map<Long, KeyXref> first = new LinkedHashMap<>();
    for (KeyXref x : loaded) {
      first.merge(x.getTargetId(), x, (a, b) -> a.getId() < b.getId() ? a : b);
    }
    return new ArrayList<>(first.values());
  }

  private static void fields(
      List<StageRow> all, MigrationLoader loader, List<KeyXref> loaded, List<ReconLineSpec> specs) {
    List<String> cols = loader.reconciledColumns();
    if (cols.isEmpty() || loaded.isEmpty()) {
      return;
    }
    Map<String, StageRow> byKey = new HashMap<>();
    all.stream()
        .filter(r -> r.getStatus() == RowStatus.LOADED)
        .forEach(r -> byKey.putIfAbsent(r.getLegacyKey(), r));
    Map<String, FieldTally> tallies = new LinkedHashMap<>();
    cols.forEach(c -> tallies.put(c, new FieldTally()));
    for (KeyXref x : survivors(loaded)) {
      StageRow row = byKey.get(x.getLegacyKey());
      if (row != null) {
        Map<String, String> target = loader.readBack(x);
        cols.forEach(
            c ->
                tallies
                    .get(c)
                    .count(x.getLegacyKey(), row.getMappedPayload().get(c), target.get(c)));
      }
    }
    tallies.forEach(
        (col, t) ->
            specs.add(
                new ReconLineSpec(
                    L4,
                    "Field " + col,
                    null,
                    ReconLine.Values.of(
                        BigDecimal.valueOf(t.compared), null, BigDecimal.valueOf(t.matched)),
                    t.diffs.isEmpty() ? null : String.join("; ", t.diffs))));
  }

  /** Comparison count of one field. */
  private static final class FieldTally {
    private int compared;
    private int matched;
    private final List<String> diffs = new ArrayList<>();

    void count(String key, String staged, String target) {
      compared++;
      if (equal(staged, target)) {
        matched++;
      } else if (diffs.size() < MAX_DETAIL_KEYS) {
        diffs.add(key + ": " + staged + " / " + target);
      }
    }
  }

  private static boolean equal(String staged, String target) {
    String a = staged == null ? "" : staged.strip();
    String b = target == null ? "" : target.strip();
    if (a.equalsIgnoreCase(b)) {
      return true;
    }
    Optional<BigDecimal> x = Values.decimal(a);
    Optional<BigDecimal> y = Values.decimal(b);
    return x.isPresent() && y.isPresent() && x.get().compareTo(y.get()) == 0;
  }
}
