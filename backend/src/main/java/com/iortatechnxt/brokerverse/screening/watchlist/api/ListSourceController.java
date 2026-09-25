package com.iortatechnxt.brokerverse.screening.watchlist.api;

import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.screening.common.service.ScreeningPermissions;
import com.iortatechnxt.brokerverse.screening.watchlist.api.dto.RunDetail;
import com.iortatechnxt.brokerverse.screening.watchlist.api.dto.RunDto;
import com.iortatechnxt.brokerverse.screening.watchlist.api.dto.SourceDto;
import com.iortatechnxt.brokerverse.screening.watchlist.domain.IngestionRun;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistBulkHandler;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistIngestionService;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistService;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistService.SourceSettings;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * List sources and ingestion runs (SNSRP-201, 202; FR-SS-020, 021): sources and their settings, the
 * list file template, "Upload List File" (a run whose changes wait for a checker), staging a file
 * for the scheduled run, and the run log with failed records.
 */
@RestController
@RequestMapping("/api/v1/screening/watchlist")
public class ListSourceController {

  private static final int MAX_PAGE = 200;
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final WatchlistService watchlists;
  private final WatchlistIngestionService ingestion;
  private final BulkService bulk;

  /**
   * Creates the controller.
   *
   * @param watchlists sources
   * @param ingestion ingestion runs
   * @param bulk bulk platform (template)
   */
  public ListSourceController(
      WatchlistService watchlists, WatchlistIngestionService ingestion, BulkService bulk) {
    this.watchlists = watchlists;
    this.ingestion = ingestion;
    this.bulk = bulk;
  }

  /**
   * The sources.
   *
   * @return sources by code
   */
  @GetMapping("/sources")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_ACCESS)
  public List<SourceDto> sources() {
    return watchlists.sources().stream().map(SourceDto::from).toList();
  }

  /**
   * Changes the settings of a source.
   *
   * @param code source
   * @param request name, schedule, layout, full file, active
   * @return the source
   */
  @PutMapping("/sources/{code}")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_MAINTAIN)
  public SourceDto updateSource(@PathVariable String code, @Valid @RequestBody SourceDto request) {
    return SourceDto.from(
        watchlists.updateSource(
            code,
            new SourceSettings(
                request.name(),
                request.schedule(),
                request.fileLayout(),
                request.fullFile(),
                request.active())));
  }

  /**
   * The list file template (bulk handler SCR_WATCHLIST).
   *
   * @return xlsx
   */
  @GetMapping("/template")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_MAINTAIN)
  public ResponseEntity<byte[]> template() {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"watchlist_template.xlsx\"")
        .contentType(MediaType.parseMediaType(XLSX))
        .body(bulk.template(WatchlistBulkHandler.CODE));
  }

  /**
   * Uploads a list file and logs the run at once; its changes wait for a checker.
   *
   * @param code source
   * @param file CSV or XLSX in the template layout
   * @return the run
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/sources/{code}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(ScreeningPermissions.HAS_LIST_MAINTAIN)
  public RunDto upload(@PathVariable String code, @RequestParam MultipartFile file)
      throws IOException {
    IngestionRun run = ingestion.upload(code, file.getOriginalFilename(), file.getBytes());
    return RunDto.from(run, watchlists.sourceCodes());
  }

  /**
   * Stages a list file for the next scheduled run of the source.
   *
   * @param code source
   * @param file CSV or XLSX in the template layout
   * @return the stored file id
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/sources/{code}/stage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(ScreeningPermissions.HAS_LIST_MAINTAIN)
  public Map<String, Long> stage(@PathVariable String code, @RequestParam MultipartFile file)
      throws IOException {
    return Map.of(
        "attachmentId", ingestion.stage(code, file.getOriginalFilename(), file.getBytes()).getId());
  }

  /**
   * The run log, newest first.
   *
   * @param source source code, blank for all
   * @param page page
   * @param size size
   * @return runs
   */
  @GetMapping("/runs")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_ACCESS)
  public PageResponse<RunDto> runs(
      @RequestParam(required = false) String source,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    Map<Long, String> codes = watchlists.sourceCodes();
    return PageResponse.of(
        ingestion.runs(source, PageRequest.of(page, Math.min(size, MAX_PAGE))),
        r -> RunDto.from(r, codes));
  }

  /**
   * A run with its failed records.
   *
   * @param id run
   * @return detail
   */
  @GetMapping("/runs/{id}")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_ACCESS)
  public RunDetail run(@PathVariable Long id) {
    return new RunDetail(
        RunDto.from(ingestion.run(id), watchlists.sourceCodes()),
        ingestion.errors(id).stream().map(RunDetail.ErrorRow::from).toList(),
        watchlists.pendingOfRun(id));
  }

  /**
   * Approves every pending change of an uploaded file.
   *
   * @param id run
   * @return number approved
   */
  @PostMapping("/runs/{id}/approve")
  @PreAuthorize(ScreeningPermissions.HAS_LIST_APPROVE)
  public Map<String, Integer> approveRun(@PathVariable Long id) {
    return Map.of("approved", watchlists.approveRun(id));
  }
}
