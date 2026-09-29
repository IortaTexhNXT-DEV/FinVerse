package com.iortatechnxt.brokerverse.migration.quality.service;

import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMaps;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * What the object rules of a batch see: the batch, its rows by layout with their raw and mapped
 * values, the migration parameters and a look-up of legacy keys already loaded for other objects
 * (the referential rules accept a parent that is loaded or in the same load).
 */
public final class ValidationScope {

  private final MigBatch batch;
  private final Map<String, List<StageRow>> rows;
  private final Map<Long, Map<String, String>> mapped;
  private final MigrationParameters parameters;
  private final BiFunction<String, Collection<String>, Set<String>> loadedKeys;
  private final LocalDate today;
  private CodeMaps maps = CodeMaps.empty();
  private Map<Long, String> sources = Map.of();
  private String baseCurrency;

  /**
   * Creates the scope.
   *
   * @param batch batch
   * @param rows rows by layout
   * @param mapped mapped values by row id
   * @param parameters parameters
   * @param loadedKeys object code and legacy keys to the keys already loaded
   * @param today business date of the validation
   */
  public ValidationScope(
      MigBatch batch,
      Map<String, List<StageRow>> rows,
      Map<Long, Map<String, String>> mapped,
      MigrationParameters parameters,
      BiFunction<String, Collection<String>, Set<String>> loadedKeys,
      LocalDate today) {
    this.batch = batch;
    this.rows = rows;
    this.mapped = mapped;
    this.parameters = parameters;
    this.loadedKeys = loadedKeys;
    this.today = today;
  }

  public LocalDate today() {
    return today;
  }

  /**
   * The approved code maps of the validation.
   *
   * @return maps
   */
  public CodeMaps maps() {
    return maps;
  }

  /**
   * Sets the approved code maps.
   *
   * @param approved maps
   * @return this scope
   */
  public ValidationScope withMaps(CodeMaps approved) {
    this.maps = approved;
    return this;
  }

  /**
   * Sets the source systems of the extracts.
   *
   * @param bySource source system by extract id
   * @return this scope
   */
  /**
   * Sets the base currency of the company of the batch (a foreign currency needs a booking rate).
   *
   * @param currency base currency
   * @return this scope
   */
  public ValidationScope withBaseCurrency(String currency) {
    this.baseCurrency = currency;
    return this;
  }

  /**
   * The base currency of the company of the batch.
   *
   * @return currency, null when not set
   */
  public String baseCurrency() {
    return baseCurrency;
  }

  public ValidationScope withSources(Map<Long, String> bySource) {
    this.sources = Map.copyOf(bySource);
    return this;
  }

  /**
   * The source system of a row.
   *
   * @param row row
   * @return source system
   */
  public String source(StageRow row) {
    return sources.getOrDefault(row.getExtractId(), "");
  }

  public MigBatch batch() {
    return batch;
  }

  /**
   * Rows of a layout.
   *
   * @param layoutCode layout
   * @return rows
   */
  public List<StageRow> rows(String layoutCode) {
    return rows.getOrDefault(layoutCode, List.of());
  }

  /**
   * Mapped values of a row (code maps applied), falling back to the raw values.
   *
   * @param row row
   * @return values
   */
  public Map<String, String> values(StageRow row) {
    return mapped.getOrDefault(row.getId(), row.getRawPayload());
  }

  /**
   * One value of a row as received (before the code maps).
   *
   * @param row row
   * @param column column
   * @return value
   */
  public String raw(StageRow row, String column) {
    return row.getRawPayload().get(column);
  }

  public MigrationParameters parameters() {
    return parameters;
  }

  /**
   * The legacy keys of an object already loaded (live cross-references) among the given keys.
   *
   * @param objectCode object
   * @param keys legacy keys
   * @return loaded keys
   */
  public Set<String> loaded(String objectCode, Collection<String> keys) {
    return loadedKeys.apply(objectCode, keys);
  }
}
