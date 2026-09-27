package com.iortatechnxt.brokerverse.migration.quality.service;

import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssue.Finding;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigIssue.Severity;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import com.iortatechnxt.brokerverse.migration.mapping.domain.LayoutColumn;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MigRule;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMaps;
import com.iortatechnxt.brokerverse.migration.mapping.service.RuleCatalogueService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The rule engine of the validation step (DATA_MIGRATION_DESIGN section 8; FR-DM-012, FR-DM-013):
 * for every staged row it applies the approved code maps and the generic rules of its layout -
 * mandatory (DQ-001), format and type (DQ-002), lookup (DQ-003, unmapped codes), unique key
 * (DQ-004) and length (DQ-005) - and then the object rules. The catalogue decides whether a rule is
 * active and its severity.
 */
@Component
public class RuleEngine {

  /** Mandatory column blank. */
  public static final String MANDATORY = "DQ-001";

  /** Wrong format or type. */
  public static final String FORMAT = "DQ-002";

  /** Legacy code not mapped. */
  public static final String LOOKUP = "DQ-003";

  /** Duplicate key. */
  public static final String UNIQUE = "DQ-004";

  /** Too long. */
  public static final String LENGTH = "DQ-005";

  private final List<ObjectRules> objectRules;
  private final RuleCatalogueService catalogue;
  private final MigrationParameters parameters;

  /**
   * Creates the engine.
   *
   * @param objectRules object rules
   * @param catalogue rule catalogue
   * @param parameters parameters
   */
  public RuleEngine(
      List<ObjectRules> objectRules,
      RuleCatalogueService catalogue,
      MigrationParameters parameters) {
    this.objectRules = objectRules;
    this.catalogue = catalogue;
    this.parameters = parameters;
  }

  /**
   * Validates the rows of a batch.
   *
   * @param batch batch
   * @param input rows and columns by layout, code maps and the loaded-key look-up
   * @return findings, mapped values and unmapped codes
   */
  public Outcome validate(MigBatch batch, Input input) {
    Map<String, MigRule> rules = catalogue.byCode();
    Outcome outcome = new Outcome();
    outcome.sources(input.sources());
    FindingSink sink = (row, f) -> outcome.add(row, configured(f, rules));
    for (Map.Entry<String, List<StageRow>> e : input.rows().entrySet()) {
      List<LayoutColumn> cols = input.columns().getOrDefault(e.getKey(), List.of());
      Set<String> keys = new HashSet<>();
      for (StageRow row : e.getValue()) {
        outcome.mapped.put(row.getId(), generic(row, cols, input.maps(), sink, outcome));
        if (!keys.add(row.getLegacyKey())) {
          sink.error(row, UNIQUE, null, row.getLegacyKey(), row.getLegacyKey() + " appears twice");
        }
      }
    }
    ValidationScope scope =
        new ValidationScope(
                batch, input.rows(), outcome.mapped, parameters, input.loadedKeys(), input.today())
            .withMaps(input.maps())
            .withSources(input.sources());
    for (ObjectRules r : objectRules) {
      if (r.layouts().stream().anyMatch(input.rows()::containsKey)) {
        r.check(scope, sink);
      }
    }
    return outcome;
  }

  private static Finding configured(Finding f, Map<String, MigRule> rules) {
    MigRule rule = rules.get(f.ruleCode());
    if (rule == null) {
      return f;
    }
    if (!rule.isActive()) {
      return null;
    }
    boolean lookup = LOOKUP.equals(rule.getCode());
    Severity severity = lookup ? f.severity() : Severity.valueOf(rule.getSeverity());
    return new Finding(f.ruleCode(), severity, f.field(), f.value(), f.message());
  }

  private Map<String, String> generic(
      StageRow row, List<LayoutColumn> cols, CodeMaps maps, FindingSink sink, Outcome outcome) {
    Map<String, String> raw = row.getRawPayload();
    Map<String, String> mapped = new LinkedHashMap<>(raw);
    String source = sourceOf(row, outcome);
    for (LayoutColumn c : cols) {
      String value = raw.get(c.getName());
      if (Values.blank(value)) {
        if (c.isMandatory()) {
          sink.error(row, MANDATORY, c.getName(), null, c.getName() + " is mandatory");
        }
        continue;
      }
      checkFormat(row, c, value, sink);
      if (c.getMapSet() != null && !c.getMapSet().isBlank()) {
        mapValue(row, c, value, new Mapping(maps, source, mapped, sink, outcome));
      }
    }
    return mapped;
  }

  private static String sourceOf(StageRow row, Outcome outcome) {
    return outcome.sources.getOrDefault(row.getExtractId(), "");
  }

  private static void checkFormat(StageRow row, LayoutColumn c, String value, FindingSink sink) {
    Integer max = c.maxLength();
    if (max != null && value.length() > max) {
      sink.error(
          row, LENGTH, c.getName(), value, c.getName() + " is longer than " + max + " characters");
    }
    Optional<String> type = typeError(c, value);
    type.ifPresent(
        t ->
            sink.error(
                row,
                FORMAT,
                c.getName(),
                value,
                c.getName() + " " + value + " is not a valid " + t));
    List<String> allowed = c.allowed();
    if (type.isEmpty()
        && !allowed.isEmpty()
        && !allowed.contains(value.strip().toUpperCase(Locale.ROOT))) {
      sink.error(
          row,
          FORMAT,
          c.getName(),
          value,
          c.getName() + " " + value + " is not one of " + String.join(", ", allowed));
    }
  }

  private static Optional<String> typeError(LayoutColumn c, String value) {
    boolean ok =
        switch (c.getDataType()) {
          case DATE -> Values.date(value).isPresent();
          case TIMESTAMP -> Values.stamp(value).isPresent();
          case AMOUNT -> Values.validAmount(value);
          case INTEGER -> value.strip().matches("-?\\d+");
          case DECIMAL -> Values.decimal(value).isPresent();
          case FLAG -> value.strip().matches("[YyNn]");
          default -> true;
        };
    return ok ? Optional.empty() : Optional.of(c.getDataType().name().toLowerCase(Locale.ROOT));
  }

  private static void mapValue(StageRow row, LayoutColumn c, String value, Mapping m) {
    String set = c.getMapSet();
    Optional<CodeMaps.Resolution> r = m.maps().resolve(set, m.source(), value.strip());
    if (r.isEmpty()) {
      m.sink()
          .add(
              row,
              new Finding(
                  LOOKUP,
                  c.isMandatory() ? Severity.ERROR : Severity.WARNING,
                  c.getName(),
                  value,
                  "Code " + value + " of " + set + " is not mapped"));
      if (!c.isMandatory()) {
        m.mapped().remove(c.getName());
      }
    } else if (r.get().action() == EntryAction.REJECT) {
      m.sink()
          .error(
              row,
              LOOKUP,
              c.getName(),
              value,
              "Code " + value + " of " + set + " is rejected by the code map");
    } else {
      m.mapped().put(c.getName(), r.get().target() == null ? value.strip() : r.get().target());
    }
  }

  private record Mapping(
      CodeMaps maps,
      String source,
      Map<String, String> mapped,
      FindingSink sink,
      Outcome outcome) {}

  /**
   * Input of a validation.
   *
   * @param rows rows by layout
   * @param columns columns by layout
   * @param maps approved code maps
   * @param sources source system by extract id
   * @param loadedKeys look-up of loaded legacy keys of other objects
   * @param today business date
   */
  public record Input(
      Map<String, List<StageRow>> rows,
      Map<String, List<LayoutColumn>> columns,
      CodeMaps maps,
      Map<Long, String> sources,
      BiFunction<String, Collection<String>, Set<String>> loadedKeys,
      LocalDate today) {}

  /**
   * An unmapped legacy code.
   *
   * @param setCode code map set
   * @param source source system
   * @param legacyCode legacy code
   * @param legacyKey key of a row using it
   * @param error found in a mandatory column
   */
  public record Unmapped(
      String setCode, String source, String legacyCode, String legacyKey, boolean error) {}

  /** Result of a validation. */
  public static final class Outcome {

    private static final Pattern NOT_MAPPED = Pattern.compile("^Code .* of (\\S+) is not mapped$");

    private final Map<Long, List<Finding>> findings = new HashMap<>();
    private final Map<Long, Map<String, String>> mapped = new HashMap<>();
    private final List<Unmapped> unmapped = new ArrayList<>();
    private final Map<Long, String> sources = new HashMap<>();

    void add(StageRow row, Finding f) {
      if (f != null) {
        findings.computeIfAbsent(row.getId(), k -> new ArrayList<>()).add(f);
        unmappedOf(row, f);
      }
    }

    /**
     * Every "not mapped" look-up finding, of a column or of an object rule, is an unmapped code.
     */
    private void unmappedOf(StageRow row, Finding f) {
      if (!LOOKUP.equals(f.ruleCode()) || f.message() == null) {
        return;
      }
      Matcher m = NOT_MAPPED.matcher(f.message());
      if (m.matches()) {
        Unmapped u =
            new Unmapped(
                m.group(1),
                sources.get(row.getExtractId()),
                f.value() == null ? "" : f.value().strip(),
                row.getLegacyKey(),
                f.severity() == Severity.ERROR);
        if (!unmapped.contains(u)) {
          unmapped.add(u);
        }
      }
    }

    /**
     * Findings of a row.
     *
     * @param rowId row
     * @return findings
     */
    public List<Finding> findings(Long rowId) {
      return findings.getOrDefault(rowId, List.of());
    }

    /**
     * Mapped values of a row.
     *
     * @param rowId row
     * @return values
     */
    public Map<String, String> mapped(Long rowId) {
      return mapped.get(rowId);
    }

    public List<Unmapped> unmapped() {
      return List.copyOf(unmapped);
    }

    /**
     * Seeds the source systems of the extracts (used by the look-ups).
     *
     * @param bySource source system by extract id
     */
    public void sources(Map<Long, String> bySource) {
      sources.putAll(bySource);
    }
  }
}
