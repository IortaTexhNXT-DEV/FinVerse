package com.iortatechnxt.brokerverse.configpromo.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.BaselineRequest;
import com.iortatechnxt.brokerverse.configpromo.api.dto.BaselineResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.CatalogueResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.CatalogueResponse.DatasetResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.CatalogueResponse.ExcludedResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.ExportRequest;
import com.iortatechnxt.brokerverse.configpromo.api.dto.PackageDetailResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.PackageResponse;
import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import com.iortatechnxt.brokerverse.configpromo.catalogue.ConfigCatalogue;
import com.iortatechnxt.brokerverse.configpromo.domain.ConfigBaseline;
import com.iortatechnxt.brokerverse.configpromo.domain.PackageKind;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackageRepository;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalJson;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageManifest;
import com.iortatechnxt.brokerverse.configpromo.service.BaselineService;
import com.iortatechnxt.brokerverse.configpromo.service.BaselineService.DatasetDrift;
import com.iortatechnxt.brokerverse.configpromo.service.CatalogueService;
import com.iortatechnxt.brokerverse.configpromo.service.EnvironmentService;
import com.iortatechnxt.brokerverse.configpromo.service.EnvironmentService.EnvironmentFacts;
import com.iortatechnxt.brokerverse.configpromo.service.EnvironmentService.EnvironmentOverride;
import com.iortatechnxt.brokerverse.configpromo.service.ExportService;
import com.iortatechnxt.brokerverse.configpromo.service.ExportService.ExportCommand;
import com.iortatechnxt.brokerverse.configpromo.service.ImportViews;
import com.iortatechnxt.brokerverse.configpromo.service.PackageStore;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuration Promotion (System Administration): the catalogue, this environment and its
 * overrides, the exports and the packages kept here, the baselines and the drift report.
 */
@RestController
@RequestMapping("/api/v1/config-promotion")
@PreAuthorize(PromotionController.ANY_PERMISSION)
public class PromotionController {

  /** Any permission of Configuration Promotion (read access to its screens). */
  public static final String ANY_PERMISSION =
      "hasAnyAuthority('CONFIG_EXPORT','CONFIG_IMPORT_PREPARE','CONFIG_IMPORT_APPROVE',"
          + "'CONFIG_BASELINE_MANAGE')";

  private static final String BASELINE_MANAGE = "hasAuthority('CONFIG_BASELINE_MANAGE')";
  private static final int MAX_PAGE = 500;

  private final CatalogueService catalogue;
  private final EnvironmentService environment;
  private final ExportService exports;
  private final PackageStore store;
  private final PromotionPackageRepository packages;
  private final BaselineService baselines;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param catalogue catalogue
   * @param environment environment
   * @param exports exports
   * @param store package store
   * @param packages package records
   * @param baselines baselines
   * @param downloads file downloads
   */
  public PromotionController(
      CatalogueService catalogue,
      EnvironmentService environment,
      ExportService exports,
      PackageStore store,
      PromotionPackageRepository packages,
      BaselineService baselines,
      FileDownloads downloads) {
    this.catalogue = catalogue;
    this.environment = environment;
    this.exports = exports;
    this.store = store;
    this.packages = packages;
    this.baselines = baselines;
    this.downloads = downloads;
  }

  /**
   * This environment: name, production, change window, signing key, versions.
   *
   * @return facts
   */
  @GetMapping("/environment")
  public EnvironmentFacts environment() {
    return environment.facts();
  }

  /**
   * The environment overrides: values this environment keeps and an import never changes.
   *
   * @return overrides per dataset
   */
  @GetMapping("/overrides")
  public List<EnvironmentOverride> overrides() {
    return environment.overrides();
  }

  /**
   * The configuration catalogue with the number of items of each dataset here.
   *
   * @return catalogue
   */
  @GetMapping("/catalogue")
  public CatalogueResponse catalogue() {
    CatalogueModel model = catalogue.model();
    ConfigCatalogue cat = model.catalogue();
    List<DatasetResponse> datasets =
        catalogue.counts().stream()
            .map(
                c -> {
                  CatalogueDataset d = c.dataset();
                  return new DatasetResponse(
                      d.code(),
                      d.name(),
                      d.group(),
                      d.module(),
                      d.key().stream().map(ImportViews::label).toList(),
                      List.copyOf(model.model(d.code()).dependencies()),
                      d.collection(),
                      d.optional(),
                      d.users(),
                      d.environment().stream().map(ImportViews::label).toList(),
                      d.environmentRows() != null,
                      c.items());
                })
            .toList();
    Map<String, Long> excluded =
        cat.excludedTables().values().stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    List<ExcludedResponse> reasons =
        cat.reasons().entrySet().stream()
            .map(
                e ->
                    new ExcludedResponse(
                        e.getKey(), e.getValue(), excluded.getOrDefault(e.getKey(), 0L)))
            .toList();
    return new CatalogueResponse(cat.groups(), datasets, reasons);
  }

  /**
   * Exports a package.
   *
   * @param request what to export
   * @return the package
   */
  @PostMapping("/exports")
  @PreAuthorize("hasAuthority('CONFIG_EXPORT')")
  @ResponseStatus(HttpStatus.CREATED)
  public PackageResponse export(@Valid @RequestBody ExportRequest request) {
    return PackageResponse.from(
        exports.export(
            new ExportCommand(
                request.datasets(),
                request.includeUsers(),
                request.baselineId(),
                request.description())));
  }

  /**
   * Packages kept here, newest first.
   *
   * @param kind EXPORT, UPLOAD or SNAPSHOT; empty = all
   * @param page page
   * @param size size
   * @return packages
   */
  @GetMapping("/packages")
  @Transactional(readOnly = true)
  public PageResponse<PackageResponse> packages(
      @RequestParam(required = false) PackageKind kind,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable = PageRequest.of(page, Math.min(size, MAX_PAGE));
    return PageResponse.of(
        kind == null
            ? packages.findAllByOrderByIdDesc(pageable)
            : packages.findByKindOrderByIdDesc(kind, pageable),
        PackageResponse::from);
  }

  /**
   * A package with its datasets.
   *
   * @param id package
   * @return package
   */
  @GetMapping("/packages/{id}")
  public PackageDetailResponse pkg(@PathVariable Long id) {
    PromotionPackage p = store.get(id);
    PackageManifest manifest =
        CanonicalJson.read(p.getManifest().getBytes(StandardCharsets.UTF_8), PackageManifest.class);
    return new PackageDetailResponse(PackageResponse.from(p), manifest.datasets());
  }

  /**
   * Downloads the file of a package.
   *
   * @param id package
   * @param request HTTP request
   * @return redirect to the file link, or the file
   */
  @GetMapping("/packages/{id}/file")
  public ResponseEntity<byte[]> file(@PathVariable Long id, HttpServletRequest request) {
    return downloads.respond(FileDownload.stored(store.get(id).getStoredFileId()), request);
  }

  /**
   * Baselines, newest first.
   *
   * @return baselines
   */
  @GetMapping("/baselines")
  public List<BaselineResponse> baselines() {
    return baselines.list().stream().map(this::baseline).toList();
  }

  /**
   * Marks a package as the baseline of this environment.
   *
   * @param request package, name, remarks
   * @return the baseline
   */
  @PostMapping("/baselines")
  @PreAuthorize(BASELINE_MANAGE)
  @ResponseStatus(HttpStatus.CREATED)
  public BaselineResponse markBaseline(@Valid @RequestBody BaselineRequest request) {
    return baseline(baselines.mark(request.packageId(), request.name(), request.remarks()));
  }

  /**
   * Retires a baseline.
   *
   * @param id baseline
   * @return the baseline
   */
  @PostMapping("/baselines/{id}/retire")
  @PreAuthorize(BASELINE_MANAGE)
  public BaselineResponse retireBaseline(@PathVariable Long id) {
    return baseline(baselines.retire(id));
  }

  /**
   * The drift of the current configuration from a baseline, per dataset.
   *
   * @param id baseline
   * @return drift
   */
  @GetMapping("/baselines/{id}/drift")
  public List<DatasetDrift> drift(@PathVariable Long id) {
    return baselines.drift(id, null);
  }

  /**
   * The drift of one dataset with its items.
   *
   * @param id baseline
   * @param code dataset
   * @return drift of the dataset
   */
  @GetMapping("/baselines/{id}/drift/{code}")
  public DatasetDrift driftOf(@PathVariable Long id, @PathVariable String code) {
    return baselines.drift(id, code).stream()
        .findFirst()
        .orElseThrow(
            () ->
                new com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException(
                    "Dataset of the baseline", code));
  }

  private BaselineResponse baseline(ConfigBaseline b) {
    return BaselineResponse.from(b, store.get(b.getPackageId()).getPackageNo());
  }

  /**
   * One page of a list.
   *
   * @param all items
   * @param page page number
   * @param size page size
   * @param <T> item type
   * @return page
   */
  static <T> PageResponse<T> page(List<T> all, int page, int size) {
    PageRequest pageable = PageRequest.of(page, Math.max(1, size));
    int from = (int) Math.min(pageable.getOffset(), all.size());
    int to = Math.min(from + pageable.getPageSize(), all.size());
    return PageResponse.of(new PageImpl<>(all.subList(from, to), pageable, all.size()), t -> t);
  }
}
