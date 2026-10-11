package com.iortatechnxt.brokerverse.migration.intake.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.intake.api.dto.ExtractResponse;
import com.iortatechnxt.brokerverse.migration.intake.domain.MigExtract;
import com.iortatechnxt.brokerverse.migration.intake.service.IntakeService;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Extracts (FR-DM-008; screen Extracts): upload of a data file with its control file, the intake
 * checks and the list of extracts with their files.
 */
@RestController
@RequestMapping("/api/v1/migration/extracts")
public class ExtractController {

  private static final int MAX_PAGE = 200;

  private final IntakeService intake;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param intake intake
   * @param downloads file answers
   */
  public ExtractController(IntakeService intake, FileDownloads downloads) {
    this.intake = intake;
    this.downloads = downloads;
  }

  /**
   * Extracts of a company.
   *
   * @param companyId company
   * @param objectCode object filter
   * @param page page
   * @param size size
   * @return extracts
   */
  @GetMapping
  @PreAuthorize("hasAuthority('MIG_VIEW')")
  public PageResponse<ExtractResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) String objectCode,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        intake.search(companyId, objectCode, PageRequest.of(page, Math.min(size, MAX_PAGE))),
        ExtractResponse::from);
  }

  /**
   * An extract.
   *
   * @param extractNo extract
   * @return extract
   */
  @GetMapping("/{extractNo}")
  @PreAuthorize("hasAuthority('MIG_VIEW')")
  public ExtractResponse get(@PathVariable String extractNo) {
    return ExtractResponse.from(intake.get(extractNo));
  }

  /**
   * Receives an extract and its control file.
   *
   * @param companyId company
   * @param objectCode object chosen on the screen
   * @param mode FULL or DELTA
   * @param file data file
   * @param control control file
   * @return extract (REJECTED with the reason when a check failed)
   * @throws IOException when a file cannot be read
   */
  @PostMapping
  @PreAuthorize("hasAuthority('MIG_INTAKE')")
  public ExtractResponse upload(
      @RequestParam Long companyId,
      @RequestParam(required = false) String objectCode,
      @RequestParam(defaultValue = "FULL") String mode,
      @RequestPart("file") MultipartFile file,
      @RequestPart(value = "control", required = false) MultipartFile control)
      throws IOException {
    return ExtractResponse.from(
        intake.receive(
            companyId,
            new IntakeService.Upload(
                objectCode,
                mode,
                file.getOriginalFilename(),
                file.getBytes(),
                control == null ? null : control.getOriginalFilename(),
                control == null ? null : control.getBytes())));
  }

  /**
   * Downloads the data or the control file of an extract.
   *
   * @param extractNo extract
   * @param kind data or control
   * @param request HTTP request
   * @return file
   */
  @GetMapping("/{extractNo}/files/{kind}")
  @PreAuthorize("hasAuthority('MIG_VIEW')")
  public ResponseEntity<byte[]> file(
      @PathVariable String extractNo, @PathVariable String kind, HttpServletRequest request) {
    MigExtract e = intake.get(extractNo);
    Long id = "control".equals(kind) ? e.getControlFileId() : e.getStoredFileId();
    if (id == null) {
      throw new BusinessRuleException("MIG_FILE_MISSING", "The file is no longer kept.");
    }
    return downloads.respond(FileDownload.stored(id), request);
  }
}
