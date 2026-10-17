package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.domain.LovDetails;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.domain.LovValueRepository;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapSet;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapSetRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Loader of the lists of values and MIS values (object R01; BRID 3.1; FR-DM-030): a legacy value
 * mapped to an existing BIBS value is confirmed; a value marked CREATE in the approved code map is
 * created through {@link LovService} pending authorization, which the Data Steward gives in the
 * list maintenance under the normal maker-checker. The dependent objects wait until the values are
 * authorised.
 */
@Component
public class LovLoader implements MigrationLoader {

  /** Entity type of a list value. */
  public static final String ENTITY = "LovValue";

  private static final LocalDate DEFAULT_FROM = LocalDate.of(2020, 1, 1);
  private static final String TARGET = "target_code";
  private static final String CREATE = "CREATE";

  private final LovService lov;
  private final LovValueRepository values;
  private final CodeMapSetRepository sets;

  /**
   * Creates the loader.
   *
   * @param lov list maintenance
   * @param values list values (read)
   * @param sets code map sets (target list of a set)
   */
  public LovLoader(LovService lov, LovValueRepository values, CodeMapSetRepository sets) {
    this.lov = lov;
    this.values = values;
    this.sets = sets;
  }

  @Override
  public String objectCode() {
    return "R01";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String type = targetType(v.get("map_set"));
    String code = v.get(TARGET);
    LovValue existing = values.findByTypeCodeAndCode(type, code).orElse(null);
    if (existing != null) {
      return LoadOutcome.of(ENTITY, existing.getId(), type + ":" + code, existing.getVersion());
    }
    if (!CREATE.equals(v.get("map_action"))) {
      throw new BusinessRuleException(
          "MIG_TARGET_MISSING", "Value " + code + " of list " + type + " does not exist in BIBS");
    }
    LovValue created =
        lov.create(
            type,
            code,
            new LovDetails(
                Values.text(v.get("description")),
                0,
                Values.text(v.get("parent_code")),
                Values.date(v.get("effective_from")).orElse(DEFAULT_FROM),
                Values.date(v.get("effective_to")).orElse(null)));
    return new LoadOutcome(
        new KeyXref.Target(ENTITY, created.getId(), type + ":" + code, created.getVersion()),
        "Created pending authorisation");
  }

  private String targetType(String setCode) {
    return sets.findByCode(setCode)
        .map(CodeMapSet::getTargetRef)
        .filter(t -> t != null && !t.isBlank())
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "MIG_MAP_TARGET", "Code map " + setCode + " names no BIBS list"));
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(TARGET);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return values
        .findById(entry.getTargetId())
        .map(l -> Map.of(TARGET, l.getCode()))
        .orElse(Map.of());
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean changedSinceLoad(KeyXref entry) {
    return values
        .findById(entry.getTargetId())
        .map(l -> !Long.valueOf(l.getVersion()).equals(entry.getTargetVersion()))
        .orElse(false);
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    values
        .findById(entry.getTargetId())
        .filter(l -> l.getRecordStatus() == RecordStatus.PENDING_AUTHORIZATION)
        .ifPresent(l -> lov.deactivate(l.getId()));
    return true;
  }
}
