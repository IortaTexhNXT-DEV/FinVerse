package com.iortatechnxt.brokerverse.migration.mapping.api;

import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.ApprovalResponse;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.DiffResponse;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.DraftRequest;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.EntryRequest;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.EntryResponse;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.SetResponse;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.UnmappedResponse;
import com.iortatechnxt.brokerverse.migration.mapping.api.dto.MapDtos.VersionResponse;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapSet;
import com.iortatechnxt.brokerverse.migration.mapping.domain.CodeMapVersion;
import com.iortatechnxt.brokerverse.migration.mapping.domain.MapVersionStatus;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapExcel;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapService;
import com.iortatechnxt.brokerverse.migration.mapping.service.UnmappedCodes;
import com.iortatechnxt.brokerverse.migration.object.api.dto.ObjectDtos.CommentRequest;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Versioned code maps (FR-DM-011, FR-DM-012; screen Code Maps). */
@RestController
@RequestMapping("/api/v1/migration/maps")
public class MapController {

  private static final String VIEW = "hasAuthority('MIG_VIEW')";
  private static final String EDIT = "hasAuthority('MIG_MAPPING_EDIT')";
  private static final String APPROVE = "hasAuthority('MIG_MAPPING_APPROVE')";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final CodeMapService maps;
  private final CodeMapExcel excel;
  private final UnmappedCodes unmapped;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param maps code maps
   * @param excel Excel export and import
   * @param unmapped unmapped codes
   * @param downloads file answers
   */
  public MapController(
      CodeMapService maps, CodeMapExcel excel, UnmappedCodes unmapped, FileDownloads downloads) {
    this.maps = maps;
    this.excel = excel;
    this.unmapped = unmapped;
    this.downloads = downloads;
  }

  /**
   * Every set with its approved and open versions.
   *
   * @return sets
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public List<SetResponse> sets() {
    return maps.sets().stream().map(this::set).toList();
  }

  private SetResponse set(CodeMapSet s) {
    List<CodeMapVersion> versions = maps.versions(s.getCode());
    CodeMapVersion approved =
        versions.stream()
            .filter(v -> v.getStatus() == MapVersionStatus.APPROVED)
            .findFirst()
            .orElse(null);
    CodeMapVersion open =
        versions.stream()
            .filter(
                v ->
                    v.getStatus() == MapVersionStatus.DRAFT
                        || v.getStatus() == MapVersionStatus.SUBMITTED)
            .findFirst()
            .orElse(null);
    return SetResponse.from(s, approved, open);
  }

  /**
   * The versions of a set.
   *
   * @param setCode set
   * @return versions, newest first
   */
  @GetMapping("/{setCode}/versions")
  @PreAuthorize(VIEW)
  public List<VersionResponse> versions(@PathVariable String setCode) {
    return maps.versions(setCode).stream().map(VersionResponse::from).toList();
  }

  /**
   * The entries of a version.
   *
   * @param versionId version
   * @return entries
   */
  @GetMapping("/versions/{versionId}/entries")
  @PreAuthorize(VIEW)
  public List<EntryResponse> entries(@PathVariable Long versionId) {
    return maps.entries(versionId).stream().map(EntryResponse::from).toList();
  }

  /**
   * Differences of a version with the approved version.
   *
   * @param versionId version
   * @return differences
   */
  @GetMapping("/versions/{versionId}/diff")
  @PreAuthorize(VIEW)
  public DiffResponse diff(@PathVariable Long versionId) {
    CodeMapService.Diff d = maps.diff(versionId);
    return new DiffResponse(
        d.added().stream().map(EntryResponse::from).toList(),
        d.changed().stream().map(EntryResponse::from).toList(),
        d.removed().stream().map(EntryResponse::from).toList());
  }

  /**
   * Creates a draft version.
   *
   * @param setCode set
   * @param companyId company of the approval work case
   * @param request draft
   * @return version
   */
  @PostMapping("/{setCode}/versions")
  @PreAuthorize(EDIT)
  public VersionResponse draft(
      @PathVariable String setCode,
      @RequestParam Long companyId,
      @RequestBody DraftRequest request) {
    return VersionResponse.from(
        maps.createDraft(companyId, setCode, request.copyApproved(), request.comment()));
  }

  /**
   * Imports a workbook as a new draft version.
   *
   * @param setCode set
   * @param companyId company
   * @param file workbook
   * @return version
   * @throws IOException when the file cannot be read
   */
  @PostMapping("/{setCode}/import")
  @PreAuthorize(EDIT)
  public VersionResponse importDraft(
      @PathVariable String setCode,
      @RequestParam Long companyId,
      @RequestPart("file") MultipartFile file)
      throws IOException {
    return VersionResponse.from(
        maps.importDraft(
            companyId,
            setCode,
            excel.read(file.getOriginalFilename(), file.getBytes()),
            "Imported from " + file.getOriginalFilename()));
  }

  /**
   * Exports a version to Excel.
   *
   * @param versionId version
   * @param request HTTP request
   * @return XLSX
   */
  @GetMapping("/versions/{versionId}/export")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> export(@PathVariable Long versionId, HttpServletRequest request) {
    CodeMapVersion v = maps.version(versionId);
    String name =
        v.getSetCode().replaceAll("[^A-Za-z0-9_]", "_") + "_v" + v.getVersionNo() + ".xlsx";
    return downloads.respond(
        FileDownload.inline(name, XLSX, excel.export(v, maps.entries(versionId))), request);
  }

  /**
   * Adds an entry to a draft.
   *
   * @param versionId draft
   * @param request entry
   * @return entry
   */
  @PostMapping("/versions/{versionId}/entries")
  @PreAuthorize(EDIT)
  public EntryResponse addEntry(
      @PathVariable Long versionId, @Valid @RequestBody EntryRequest request) {
    return EntryResponse.from(maps.saveEntry(versionId, null, request.toData()));
  }

  /**
   * Changes an entry of a draft.
   *
   * @param versionId draft
   * @param entryId entry
   * @param request entry
   * @return entry
   */
  @PutMapping("/versions/{versionId}/entries/{entryId}")
  @PreAuthorize(EDIT)
  public EntryResponse updateEntry(
      @PathVariable Long versionId,
      @PathVariable Long entryId,
      @Valid @RequestBody EntryRequest request) {
    return EntryResponse.from(maps.saveEntry(versionId, entryId, request.toData()));
  }

  /**
   * Removes an entry of a draft.
   *
   * @param versionId draft
   * @param entryId entry
   */
  @DeleteMapping("/versions/{versionId}/entries/{entryId}")
  @PreAuthorize(EDIT)
  public void deleteEntry(@PathVariable Long versionId, @PathVariable Long entryId) {
    maps.deleteEntry(versionId, entryId);
  }

  /**
   * Submits a draft.
   *
   * @param versionId draft
   * @return version
   */
  @PostMapping("/versions/{versionId}/submit")
  @PreAuthorize(EDIT)
  public VersionResponse submit(@PathVariable Long versionId) {
    return VersionResponse.from(maps.submit(versionId));
  }

  /**
   * Approves a version.
   *
   * @param versionId version
   * @param request comment
   * @return version and the values to create
   */
  @PostMapping("/versions/{versionId}/approve")
  @PreAuthorize(APPROVE)
  public ApprovalResponse approve(
      @PathVariable Long versionId, @RequestBody CommentRequest request) {
    CodeMapService.Approval a = maps.approve(versionId, request.comment());
    return new ApprovalResponse(VersionResponse.from(a.version()), a.toCreate());
  }

  /**
   * Returns a version to draft.
   *
   * @param versionId version
   * @param request reason
   * @return version
   */
  @PostMapping("/versions/{versionId}/return")
  @PreAuthorize(APPROVE)
  public VersionResponse returnVersion(
      @PathVariable Long versionId, @Valid @RequestBody ReasonRequest request) {
    return VersionResponse.from(maps.returnVersion(versionId, request.reason()));
  }

  /**
   * Unmapped legacy codes.
   *
   * @param companyId company
   * @return codes
   */
  @GetMapping("/unmapped")
  @PreAuthorize(VIEW)
  public List<UnmappedResponse> unmapped(@RequestParam Long companyId) {
    return unmapped.list(companyId).stream()
        .map(
            c ->
                new UnmappedResponse(
                    c.setCode(),
                    c.sourceSystem(),
                    c.legacyCode(),
                    c.rows(),
                    c.sampleKeys(),
                    c.error()))
        .toList();
  }
}
