package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.configpromo.domain.ConfigBaseline;
import com.iortatechnxt.brokerverse.configpromo.domain.ConfigBaselineRepository;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportStatus;
import com.iortatechnxt.brokerverse.configpromo.domain.PackageKind;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImportRepository;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalJson;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalRow;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetDiff;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetModel;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetReader;
import com.iortatechnxt.brokerverse.configpromo.engine.DiffEngine;
import com.iortatechnxt.brokerverse.configpromo.engine.ManifestDataset;
import com.iortatechnxt.brokerverse.configpromo.service.ImportViews.ItemView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration baselines and drift: a package exported from this environment, or applied to it, is
 * marked as its baseline (for example "UAT signed-off baseline 2027-11"); the drift report compares
 * the current configuration with it, dataset by dataset and item by item.
 */
@Service
@Transactional
public class BaselineService {

  private static final String ENTITY = "ConfigBaseline";

  private final ConfigBaselineRepository baselines;
  private final PromotionImportRepository imports;
  private final PackageStore store;
  private final CatalogueService catalogue;
  private final PromotionSettings settings;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param baselines baselines
   * @param imports imports
   * @param store package store
   * @param catalogue catalogue
   * @param settings settings
   * @param audit audit trail
   */
  public BaselineService(
      ConfigBaselineRepository baselines,
      PromotionImportRepository imports,
      PackageStore store,
      CatalogueService catalogue,
      PromotionSettings settings,
      AuditTrailService audit) {
    this.baselines = baselines;
    this.imports = imports;
    this.store = store;
    this.catalogue = catalogue;
    this.settings = settings;
    this.audit = audit;
  }

  /**
   * Marks a package as a baseline.
   *
   * @param packageId package exported here or applied here
   * @param name name
   * @param remarks remarks
   * @return the baseline
   */
  public ConfigBaseline mark(Long packageId, String name, String remarks) {
    PromotionPackage pkg = store.get(packageId);
    boolean applied =
        imports.findByPackageIdOrderByIdDesc(packageId).stream()
            .anyMatch(i -> i.getStatus() == ImportStatus.APPLIED);
    if (pkg.getKind() != PackageKind.EXPORT && !applied) {
      throw new BusinessRuleException(
          "CONFIG_BASELINE_NOT_PROMOTED",
          "Only a package exported from this environment or applied to it can be its baseline");
    }
    if (name == null || name.isBlank() || baselines.existsByNameIgnoreCase(name.strip())) {
      throw new BusinessRuleException(
          "CONFIG_BASELINE_NAME", "Give the baseline a name that is not used yet");
    }
    ConfigBaseline baseline =
        baselines.save(
            new ConfigBaseline(name.strip(), packageId, settings.environment(), remarks));
    audit.record(
        ENTITY,
        baseline.getName(),
        AuditAction.CREATE,
        "Baseline from package " + pkg.getPackageNo() + ", SHA-256 " + pkg.getSha256());
    return baseline;
  }

  /**
   * Retires a baseline.
   *
   * @param id baseline
   * @return the baseline
   */
  public ConfigBaseline retire(Long id) {
    ConfigBaseline baseline = get(id);
    baseline.retire();
    audit.record(ENTITY, baseline.getName(), AuditAction.DEACTIVATE, "Retired");
    return baseline;
  }

  /**
   * Baselines, newest first.
   *
   * @return baselines
   */
  @Transactional(readOnly = true)
  public List<ConfigBaseline> list() {
    return baselines.findAllByOrderByIdDesc();
  }

  /**
   * A baseline.
   *
   * @param id id
   * @return baseline
   */
  @Transactional(readOnly = true)
  public ConfigBaseline get(Long id) {
    return baselines
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Configuration baseline", id));
  }

  /**
   * The drift of one dataset.
   *
   * @param code dataset
   * @param name dataset name
   * @param group catalogue group
   * @param comparable whether the dataset has the same fields as when the baseline was taken
   * @param added items new since the baseline
   * @param changed items changed since the baseline
   * @param removed items of the baseline no longer present
   * @param items the items (only when asked for one dataset)
   */
  public record DatasetDrift(
      String code,
      String name,
      String group,
      boolean comparable,
      int added,
      int changed,
      int removed,
      List<ItemView> items) {}

  /**
   * The drift between the current configuration and a baseline.
   *
   * @param id baseline
   * @param only dataset to detail with its items, null for the summary of every dataset
   * @return drift per dataset
   */
  public List<DatasetDrift> drift(Long id, String only) {
    ConfigBaseline baseline = get(id);
    ConfigPackage base = store.open(store.get(baseline.getPackageId()));
    CatalogueModel model = catalogue.model();
    DatasetReader reader = catalogue.reader();
    List<DatasetDrift> drift = new ArrayList<>();
    for (ManifestDataset d : base.manifest().datasets()) {
      if (only != null && !only.equals(d.code())) {
        continue;
      }
      if (!model.has(d.code()) || !model.model(d.code()).fingerprint().equals(d.fingerprint())) {
        drift.add(new DatasetDrift(d.code(), d.name(), d.group(), false, 0, 0, 0, List.of()));
        continue;
      }
      DatasetModel m = model.model(d.code());
      List<Map<String, Object>> current =
          reader.rows(d.code()).stream().map(CanonicalRow::values).toList();
      List<CanonicalRow> before =
          base.rows(d.code()).stream().map(r -> CanonicalRow.of(null, r, m)).toList();
      Set<String> parents = new HashSet<>();
      if (m.dataset().collection()) {
        before.stream()
            .map(r -> r.get(m.dataset().parent()))
            .filter(Objects::nonNull)
            .forEach(p -> parents.add(CanonicalJson.text(p)));
      }
      DatasetDiff diff = DiffEngine.diff(m, current, before, true, parents);
      drift.add(
          new DatasetDrift(
              d.code(),
              d.name(),
              d.group(),
              true,
              diff.added().size(),
              diff.changed().size(),
              diff.onlyInTarget().size(),
              only == null ? List.of() : ImportViews.items(m, diff)));
    }
    return drift;
  }
}
