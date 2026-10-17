package com.iortatechnxt.brokerverse.collections.files.api;

import com.iortatechnxt.brokerverse.collections.files.api.dto.FileDtos.ExportRequest;
import com.iortatechnxt.brokerverse.collections.files.api.dto.FileDtos.FileResponse;
import com.iortatechnxt.brokerverse.collections.files.api.dto.FileDtos.GenerateRequest;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Frequency;
import com.iortatechnxt.brokerverse.collections.files.service.CollectionFiles;
import com.iortatechnxt.brokerverse.collections.files.service.ScheduledFileService;
import com.iortatechnxt.brokerverse.collections.worklist.api.ClxAccess;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Collections files (BRCLXN.024-029, 045; caveat p.93): the published daily, weekly and monthly
 * files and the exports with their availability, a job run on demand for a date, and the export of
 * the Outstanding PR List. Files are downloaded through the report archive under {@code CLX_EXPORT}
 * from their availability time.
 */
@RestController
@RequestMapping("/api/v1/collections")
public class FilesController {

  private final ScheduledFileService files;
  private final CollectionFiles publications;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param files published files
   * @param publications file publications
   * @param clock clock
   */
  public FilesController(ScheduledFileService files, CollectionFiles publications, Clock clock) {
    this.files = files;
    this.publications = publications;
    this.clock = clock;
  }

  /**
   * Published files, newest first.
   *
   * @param companyId company
   * @param frequency frequencies (empty = all)
   * @param page page
   * @param size size
   * @return files
   */
  @GetMapping("/files")
  @PreAuthorize(ClxAccess.FILES)
  public PageResponse<FileResponse> files(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<Frequency> frequency,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Instant now = clock.instant();
    return PageResponse.of(
        files.list(
            companyId,
            frequency == null ? List.of() : frequency,
            ClxAccess.page(page, size, Sort.unsorted())),
        f -> FileResponse.from(f, now));
  }

  /**
   * Runs a file publication now for a date (Collections Setup).
   *
   * @param companyId company
   * @param request frequency and date
   * @return the files
   */
  @PostMapping("/files/generate")
  @PreAuthorize(ClxAccess.SETUP)
  public List<FileResponse> generate(
      @RequestParam Long companyId, @Valid @RequestBody GenerateRequest request) {
    List<ScheduledFile> out =
        switch (request.frequency()) {
          case DAILY -> publications.daily(companyId, request.date());
          case WEEKLY -> publications.weekly(companyId, request.date());
          default -> publications.monthly(companyId, request.date());
        };
    Instant now = clock.instant();
    return out.stream().map(f -> FileResponse.from(f, now)).toList();
  }

  /**
   * Exports the Outstanding PR List of a segment and unit (caveat p.93: capped, permission-gated).
   *
   * @param companyId company
   * @param request segment and unit
   * @return the file to download
   */
  @PostMapping("/exports")
  @PreAuthorize(ClxAccess.EXPORT)
  public FileResponse export(
      @RequestParam Long companyId, @Valid @RequestBody ExportRequest request) {
    return FileResponse.from(
        publications.export(companyId, request.segment(), request.salesUnit()), clock.instant());
  }
}
