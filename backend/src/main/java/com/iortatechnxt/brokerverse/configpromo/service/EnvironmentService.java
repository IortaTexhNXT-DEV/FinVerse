package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalRow;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetModel;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetReader;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This environment as Configuration Promotion sees it: its name, whether it is production, the
 * change window, the signing key, and the environment overrides — the values every environment
 * keeps for itself and an import never changes.
 */
@Service
@Transactional(readOnly = true)
public class EnvironmentService {

  private final PromotionSettings settings;
  private final CatalogueService catalogue;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param settings settings
   * @param catalogue catalogue
   * @param jdbc database
   * @param clock clock
   */
  public EnvironmentService(
      PromotionSettings settings, CatalogueService catalogue, JdbcTemplate jdbc, Clock clock) {
    this.settings = settings;
    this.catalogue = catalogue;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * Facts of this environment.
   *
   * @param environment name
   * @param production whether production
   * @param changeWindow change window of production imports, empty when not set
   * @param windowOpen whether the window is open now
   * @param signingConfigured whether packages can be signed and verified
   * @param keyId identifier of the signing key, null when not configured
   * @param pipelineApply whether the pipeline may apply its own dry run here
   * @param platformVersion platform version
   * @param schemaVersion schema version
   */
  public record EnvironmentFacts(
      String environment,
      boolean production,
      String changeWindow,
      boolean windowOpen,
      boolean signingConfigured,
      String keyId,
      boolean pipelineApply,
      String platformVersion,
      String schemaVersion) {}

  /**
   * Facts of this environment.
   *
   * @return facts
   */
  public EnvironmentFacts facts() {
    String window = settings.productionWindow();
    boolean open = false;
    if (!window.isBlank()) {
      try {
        open = ChangeWindow.parse(window).isOpen(BusinessClock.now(clock).toLocalDateTime());
      } catch (RuntimeException e) {
        open = false;
      }
    }
    return new EnvironmentFacts(
        settings.environment(),
        settings.production(),
        window,
        open,
        settings.signingConfigured(),
        settings.signingConfigured() ? settings.signer().keyId() : null,
        settings.pipelineApplyAllowed(),
        settings.platformVersion(),
        settings.schemaVersion());
  }

  /**
   * The environment values of one dataset.
   *
   * @param code dataset
   * @param name dataset name
   * @param kind ROWS (whole items kept by the environment) or FIELDS (fields of every item)
   * @param fields labels of the kept fields
   * @param items items with the values this environment keeps
   */
  public record EnvironmentOverride(
      String code, String name, String kind, List<String> fields, List<OverrideItem> items) {}

  /**
   * One item of an override.
   *
   * @param key natural key in words
   * @param values kept values by field label
   */
  public record OverrideItem(String key, Map<String, String> values) {}

  /**
   * The environment overrides of this environment.
   *
   * @return overrides per dataset
   */
  public List<EnvironmentOverride> overrides() {
    CatalogueModel model = catalogue.model();
    DatasetReader reader = catalogue.reader();
    List<EnvironmentOverride> overrides = new ArrayList<>();
    for (String code : model.loadOrder()) {
      DatasetModel m = model.model(code);
      CatalogueDataset d = m.dataset();
      if (d.environmentRows() != null) {
        overrides.add(environmentRows(m));
      }
      if (!d.environment().isEmpty()) {
        List<OverrideItem> items = new ArrayList<>();
        for (CanonicalRow row : reader.rows(code)) {
          Map<String, String> values = new LinkedHashMap<>();
          d.environment()
              .forEach(c -> values.put(ImportViews.label(c), ImportViews.text(row.get(c))));
          items.add(new OverrideItem(ImportViews.keyLabel(row.key()), values));
        }
        overrides.add(
            new EnvironmentOverride(
                code,
                d.name(),
                "FIELDS",
                d.environment().stream().map(ImportViews::label).toList(),
                items));
      }
    }
    return overrides;
  }

  private EnvironmentOverride environmentRows(DatasetModel m) {
    CatalogueDataset d = m.dataset();
    String column = d.environmentRows().column();
    List<OverrideItem> items = new ArrayList<>();
    if ("sys_parameter".equals(m.table().name())) {
      for (String key : d.environmentRows().values()) {
        List<String> values =
            jdbc.queryForList(
                "select param_value from sys_parameter where param_key = ?", String.class, key);
        if (!values.isEmpty()) {
          items.add(
              new OverrideItem(key, Map.of("Value", values.get(0) == null ? "" : values.get(0))));
        }
      }
    } else {
      d.environmentRows().values().forEach(v -> items.add(new OverrideItem(v, Map.of())));
    }
    return new EnvironmentOverride(
        m.code(), d.name(), "ROWS", List.of(ImportViews.label(column)), items);
  }
}
