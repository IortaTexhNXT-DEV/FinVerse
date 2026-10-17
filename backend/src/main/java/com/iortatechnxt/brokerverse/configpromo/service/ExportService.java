package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import com.iortatechnxt.brokerverse.configpromo.domain.ConfigBaseline;
import com.iortatechnxt.brokerverse.configpromo.domain.ConfigBaselineRepository;
import com.iortatechnxt.brokerverse.configpromo.domain.PackageKind;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalRow;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.Checksums;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetReader;
import com.iortatechnxt.brokerverse.configpromo.engine.ManifestDataset;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageManifest;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageWriter;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageWriter.Header;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageWriter.Written;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports the configuration of this environment into a signed package: every dataset or a selection
 * (full mode), or the datasets changed since a baseline (incremental mode). Users are exported only
 * when asked, and never their passwords or second factor.
 */
@Service
@Transactional
public class ExportService {

  private static final String ENTITY = "ConfigPackage";

  private final CatalogueService catalogue;
  private final PackageStore store;
  private final PromotionSettings settings;
  private final ConfigBaselineRepository baselines;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param catalogue catalogue
   * @param store package store
   * @param settings settings
   * @param baselines baselines
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ExportService(
      CatalogueService catalogue,
      PackageStore store,
      PromotionSettings settings,
      ConfigBaselineRepository baselines,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.catalogue = catalogue;
    this.store = store;
    this.settings = settings;
    this.baselines = baselines;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * What to export.
   *
   * @param datasets dataset codes; empty = every dataset selected by default
   * @param includeUsers whether users are exported
   * @param baselineId baseline for an incremental package (datasets changed since), null for full
   * @param description purpose
   */
  public record ExportCommand(
      List<String> datasets, boolean includeUsers, Long baselineId, String description) {

    /** Defensive copy. */
    public ExportCommand {
      datasets = datasets == null ? List.of() : List.copyOf(datasets);
    }
  }

  /**
   * Exports a package.
   *
   * @param command what to export
   * @return the package record
   */
  public PromotionPackage export(ExportCommand command) {
    CatalogueModel model = catalogue.model();
    List<String> selected = selection(model, command);
    DatasetReader reader = catalogue.reader();
    String mode = PackageManifest.FULL;
    if (command.baselineId() != null) {
      selected = changedSince(command.baselineId(), selected, reader);
      mode = PackageManifest.INCREMENTAL;
    }
    if (selected.isEmpty()) {
      throw new BusinessRuleException(
          "CONFIG_EXPORT_EMPTY", "Nothing to export: no dataset is selected or changed");
    }
    PromotionPackage pkg =
        write(
            reader,
            PackageWriter.expand(model, selected),
            mode,
            command.includeUsers(),
            command.description(),
            PackageKind.EXPORT);
    audit.record(
        ENTITY,
        pkg.getPackageNo(),
        AuditAction.EXPORT,
        "Exported "
            + pkg.getDatasetCount()
            + " dataset(s), "
            + pkg.getRowCount()
            + " item(s), "
            + mode.toLowerCase(java.util.Locale.ROOT)
            + ", SHA-256 "
            + pkg.getSha256());
    return pkg;
  }

  /**
   * Takes a snapshot of the configuration of this environment (before an import).
   *
   * @param codes datasets
   * @param includeUsers whether user datasets are among them
   * @param description purpose
   * @return the snapshot package
   */
  public PromotionPackage snapshot(List<String> codes, boolean includeUsers, String description) {
    DatasetReader reader = catalogue.reader();
    return write(
        reader, codes, PackageManifest.FULL, includeUsers, description, PackageKind.SNAPSHOT);
  }

  private PromotionPackage write(
      DatasetReader reader,
      List<String> codes,
      String mode,
      boolean includeUsers,
      String description,
      PackageKind kind) {
    Header header =
        new Header(
            UUID.randomUUID().toString(),
            settings.platformVersion(),
            settings.schemaVersion(),
            settings.environment(),
            currentUser.username(),
            clock.instant().toString(),
            mode,
            includeUsers,
            description);
    Written written = PackageWriter.write(reader, codes, header, settings.signer());
    return store.keep(kind, written.manifest(), written.content(), settings.signer().keyId());
  }

  private List<String> selection(CatalogueModel model, ExportCommand command) {
    List<String> selected = new ArrayList<>();
    if (command.datasets().isEmpty()) {
      for (CatalogueDataset d : model.catalogue().datasets()) {
        if (d.selectedByDefault(command.includeUsers()) && model.has(d.code())) {
          selected.add(d.code());
        }
      }
      return selected;
    }
    for (String code : command.datasets()) {
      CatalogueDataset d =
          model
              .catalogue()
              .find(code)
              .orElseThrow(
                  () ->
                      new BusinessRuleException(
                          "CONFIG_DATASET_UNKNOWN", code + " is not a configuration dataset"));
      if (d.users() && !command.includeUsers()) {
        throw new BusinessRuleException(
            "CONFIG_USERS_NOT_INCLUDED",
            d.name() + " holds users; choose to include users to export it");
      }
      selected.add(code);
    }
    return selected;
  }

  /** The selected datasets whose content differs from the baseline package. */
  private List<String> changedSince(Long baselineId, Collection<String> selected, DatasetReader r) {
    ConfigBaseline baseline =
        baselines
            .findById(baselineId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "CONFIG_BASELINE_UNKNOWN", "The baseline does not exist"));
    ConfigPackage base = store.open(store.get(baseline.getPackageId()));
    List<String> changed = new ArrayList<>();
    for (String code : selected) {
      ManifestDataset before = base.manifest().dataset(code);
      List<Map<String, Object>> now = r.rows(code).stream().map(CanonicalRow::values).toList();
      String hash = Checksums.content(now, r.model().model(code).comparedColumns());
      if (before == null || before.rows() != now.size() || !before.contentSha256().equals(hash)) {
        changed.add(code);
      }
    }
    return changed;
  }
}
