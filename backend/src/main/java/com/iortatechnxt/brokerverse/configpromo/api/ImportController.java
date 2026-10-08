package com.iortatechnxt.brokerverse.configpromo.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.configpromo.api.dto.DecisionRequest;
import com.iortatechnxt.brokerverse.configpromo.api.dto.ImportDatasetResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.ImportResponse;
import com.iortatechnxt.brokerverse.configpromo.api.dto.OptionsRequest;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportDataset;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalJson;
import com.iortatechnxt.brokerverse.configpromo.engine.ChangeType;
import com.iortatechnxt.brokerverse.configpromo.service.CatalogueService;
import com.iortatechnxt.brokerverse.configpromo.service.DryRunRecorder;
import com.iortatechnxt.brokerverse.configpromo.service.ImportApplier;
import com.iortatechnxt.brokerverse.configpromo.service.ImportService;
import com.iortatechnxt.brokerverse.configpromo.service.ImportService.UploadCommand;
import com.iortatechnxt.brokerverse.configpromo.service.ImportViews.ItemView;
import com.iortatechnxt.brokerverse.configpromo.service.PackageStore;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Imports of configuration packages: upload, check and dry run, difference viewer, submission,
 * approval and apply, rejection, withdrawal, rollback. The same upload endpoint serves the
 * deployment pipelines (a service user with the prepare permission): dry run first, then submit, or
 * apply its own dry run outside production when the environment allows it.
 */
@RestController
@RequestMapping("/api/v1/config-promotion/imports")
@PreAuthorize(PromotionController.ANY_PERMISSION)
public class ImportController {

  private static final String PREPARE = "hasAuthority('CONFIG_IMPORT_PREPARE')";
  private static final String APPROVE = "hasAuthority('CONFIG_IMPORT_APPROVE')";
  private static final int MAX_PAGE = 500;
  private static final TypeReference<List<ItemView>> ITEMS = new TypeReference<>() {};

  private final ImportService imports;
  private final ImportApplier applier;
  private final DryRunRecorder recorder;
  private final PackageStore store;
  private final CatalogueService catalogue;

  /**
   * Creates the controller.
   *
   * @param imports import lifecycle
   * @param applier approval and apply
   * @param recorder dry run records
   * @param store packages
   * @param catalogue catalogue
   */
  public ImportController(
      ImportService imports,
      ImportApplier applier,
      DryRunRecorder recorder,
      PackageStore store,
      CatalogueService catalogue) {
    this.imports = imports;
    this.applier = applier;
    this.recorder = recorder;
    this.store = store;
    this.catalogue = catalogue;
  }

  /**
   * Uploads a package, checks it and runs the dry run; a pipeline may ask to apply it at once where
   * the environment allows the pipeline apply.
   *
   * @param file package (zip)
   * @param datasets datasets to import; empty = all
   * @param deactivate datasets whose items only here are deactivated
   * @param includeUsers whether users are imported
   * @param changeReference change request number (mandatory in production)
   * @param reason reason (mandatory in production)
   * @param pipeline whether a deployment pipeline sends the package
   * @param apply pipeline only: apply the import when its dry run is clean and allowed here
   * @return the import
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(PREPARE)
  @ResponseStatus(HttpStatus.CREATED)
  @SuppressWarnings("java:S107") // request parameters of the form
  public ImportResponse upload(
      @RequestParam MultipartFile file,
      @RequestParam(required = false) List<String> datasets,
      @RequestParam(required = false) List<String> deactivate,
      @RequestParam(defaultValue = "false") boolean includeUsers,
      @RequestParam(required = false) String changeReference,
      @RequestParam(required = false) String reason,
      @RequestParam(defaultValue = "false") boolean pipeline,
      @RequestParam(defaultValue = "false") boolean apply)
      throws IOException {
    PromotionImport imp =
        imports.upload(
            new UploadCommand(
                file.getOriginalFilename(),
                file.getBytes(),
                new OptionsRequest(datasets, deactivate, includeUsers).options(),
                changeReference,
                reason,
                pipeline));
    if (pipeline && apply) {
      imp = applier.pipelineApply(imp.getId());
    }
    return response(imp);
  }

  /**
   * Imports, newest first.
   *
   * @param page page
   * @param size size
   * @return imports
   */
  @GetMapping
  public PageResponse<ImportResponse> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        imports.list(PageRequest.of(page, Math.min(size, MAX_PAGE))), this::response);
  }

  /**
   * One import.
   *
   * @param id import
   * @return import
   */
  @GetMapping("/{id}")
  public ImportResponse get(@PathVariable Long id) {
    return response(imports.get(id));
  }

  /**
   * The datasets of an import with the dry run, apply and reconciliation figures.
   *
   * @param id import
   * @return datasets in load order
   */
  @GetMapping("/{id}/datasets")
  public List<ImportDatasetResponse> datasets(@PathVariable Long id) {
    return imports.datasets(id).stream()
        .map(
            l ->
                ImportDatasetResponse.from(
                    l, catalogue.catalogue().find(l.getDatasetCode()).orElse(null)))
        .toList();
  }

  /**
   * The items of one dataset of an import (difference viewer).
   *
   * @param id import
   * @param code dataset
   * @param type ADDED, CHANGED or ONLY_IN_TARGET; empty = all
   * @param search text the key must contain
   * @param page page
   * @param size size
   * @return items
   */
  @GetMapping("/{id}/datasets/{code}/items")
  public PageResponse<ItemView> items(
      @PathVariable Long id,
      @PathVariable String code,
      @RequestParam(required = false) ChangeType type,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    ImportDataset line = imports.dataset(id, code);
    List<ItemView> all =
        CanonicalJson.read(line.getDetails().getBytes(StandardCharsets.UTF_8), ITEMS).stream()
            .filter(i -> type == null || i.type() == type)
            .filter(i -> matches(i.key(), search))
            .toList();
    return PromotionController.page(all, page, Math.min(size, MAX_PAGE));
  }

  /**
   * Runs the dry run again with other choices.
   *
   * @param id import
   * @param request choices
   * @return the import
   */
  @PostMapping("/{id}/check")
  @PreAuthorize(PREPARE)
  public ImportResponse check(@PathVariable Long id, @RequestBody OptionsRequest request) {
    return response(imports.recheck(id, request.options()));
  }

  /**
   * Submits an import for approval.
   *
   * @param id import
   * @return the import
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(PREPARE)
  public ImportResponse submit(@PathVariable Long id) {
    return response(imports.submit(id));
  }

  /**
   * Approves an import and applies it (a second user).
   *
   * @param id import
   * @param request remarks
   * @return the import, applied or failed
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize(APPROVE)
  public ImportResponse approve(
      @PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
    return response(applier.approve(id, request.note()));
  }

  /**
   * Rejects an import.
   *
   * @param id import
   * @param request reason
   * @return the import
   */
  @PostMapping("/{id}/reject")
  @PreAuthorize(APPROVE)
  public ImportResponse reject(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return response(imports.reject(id, request.reason()));
  }

  /**
   * Withdraws an import.
   *
   * @param id import
   * @return the import
   */
  @PostMapping("/{id}/cancel")
  @PreAuthorize(PREPARE)
  public ImportResponse cancel(@PathVariable Long id) {
    return response(imports.cancel(id));
  }

  /**
   * Prepares the rollback of an applied import from its snapshot.
   *
   * @param id applied import
   * @return the rollback import
   */
  @PostMapping("/{id}/rollback")
  @PreAuthorize(PREPARE)
  @ResponseStatus(HttpStatus.CREATED)
  public ImportResponse rollback(@PathVariable Long id) {
    return response(imports.rollback(id));
  }

  /**
   * Applies the pipeline's own dry run, outside production when allowed.
   *
   * @param id import
   * @return the import
   */
  @PostMapping("/{id}/pipeline-apply")
  @PreAuthorize(PREPARE)
  public ImportResponse pipelineApply(@PathVariable Long id) {
    return response(applier.pipelineApply(id));
  }

  private static boolean matches(String key, String search) {
    return search == null
        || search.isBlank()
        || key.toLowerCase(Locale.ROOT).contains(search.strip().toLowerCase(Locale.ROOT));
  }

  private ImportResponse response(PromotionImport i) {
    PromotionPackage pkg = store.get(i.getPackageId());
    return ImportResponse.from(
        i,
        pkg.getPackageNo(),
        pkg.getSourceEnvironment(),
        recorder.options(i),
        recorder.messages(i));
  }
}
