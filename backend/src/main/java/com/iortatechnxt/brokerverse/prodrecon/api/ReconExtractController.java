package com.iortatechnxt.brokerverse.prodrecon.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ProdReconDtos.UploadResponse;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ProdReconDtos.UploadResultResponse;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ReconExtractDtos.ExtractLineResponse;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ReconExtractDtos.ExtractRequest;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ReconExtractDtos.ExtractResponse;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ReconExtractDtos.ScheduleRequest;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ReconExtractDtos.ScheduleResponse;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ReconExtractDtos.SendRequest;
import com.iortatechnxt.brokerverse.prodrecon.api.dto.ReconExtractDtos.SettingsResponse;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ExtractTrigger;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconScheduleService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconSendService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconSettings;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconUploadService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Production registers, insurer uploads and extraction schedules (PRCID.001-011/020/031/032): the
 * manual extraction, the extract register and its lines and file, the sending to the insurer, the
 * insurer upload with its attempt history, and the schedules per insurer.
 */
@RestController
@RequestMapping("/api/v1/prodrecon")
public class ReconExtractController {

  private final ProductionExtractService extracts;
  private final ReconSendService sender;
  private final ReconUploadService uploads;
  private final ReconScheduleService schedules;
  private final ReconSettings settings;
  private final ExtractRepositoryService repository;

  /**
   * Creates the controller.
   *
   * @param extracts extraction
   * @param sender sending
   * @param uploads insurer uploads
   * @param schedules schedules
   * @param settings parameters
   * @param repository extract repository (files)
   */
  public ReconExtractController(
      ProductionExtractService extracts,
      ReconSendService sender,
      ReconUploadService uploads,
      ReconScheduleService schedules,
      ReconSettings settings,
      ExtractRepositoryService repository) {
    this.extracts = extracts;
    this.sender = sender;
    this.uploads = uploads;
    this.schedules = schedules;
    this.settings = settings;
    this.repository = repository;
  }

  /**
   * Extracts a production register for a booking period (PRCID.011).
   *
   * @param request company, insurer and period
   * @return extract
   */
  @PostMapping("/extracts")
  @PreAuthorize(ReconAccess.PROCESS)
  public ExtractResponse extract(@Valid @RequestBody ExtractRequest request) {
    return ExtractResponse.from(
        extracts.extract(
            new ProductionExtractService.ExtractRequest(
                request.companyId(), request.insurerCode().strip(), request.from(), request.to()),
            ExtractTrigger.MANUAL));
  }

  /**
   * The extract register (PRCID.005/034).
   *
   * @param companyId company
   * @param insurer insurer
   * @param page page
   * @param size size
   * @return extracts
   */
  @GetMapping("/extracts")
  @PreAuthorize(ReconAccess.READ)
  public PageResponse<ExtractResponse> extracts(
      @RequestParam Long companyId,
      @RequestParam(required = false) String insurer,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        extracts.search(
            companyId,
            insurer == null || insurer.isBlank() ? null : insurer.strip(),
            ReconAccess.page(page, size)),
        ExtractResponse::from);
  }

  /**
   * Extracts of a cycle.
   *
   * @param id cycle
   * @return extracts, newest first
   */
  @GetMapping("/cycles/{id}/extracts")
  @PreAuthorize(ReconAccess.READ)
  public List<ExtractResponse> ofCycle(@PathVariable Long id) {
    return extracts.ofCycle(id).stream().map(ExtractResponse::from).toList();
  }

  /**
   * Lines of an extract (PRCID.020).
   *
   * @param id extract
   * @param page page
   * @param size size
   * @return lines
   */
  @GetMapping("/extracts/{id}/lines")
  @PreAuthorize(ReconAccess.READ)
  public PageResponse<ExtractLineResponse> lines(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        extracts.lines(id, ReconAccess.page(page, size)), ExtractLineResponse::from);
  }

  /**
   * Downloads the register workbook.
   *
   * @param id extract
   * @return the workbook
   */
  @GetMapping("/extracts/{id}/file")
  @PreAuthorize(ReconAccess.READ)
  public ResponseEntity<byte[]> file(@PathVariable Long id) {
    ReconExtract extract = extracts.require(id);
    if (extract.getFileId() == null) {
      throw new ResourceNotFoundException("Production register file", extract.getExtractNo());
    }
    ExtractFile file = repository.download(extract.getFileId());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.getFileName()))
        .body(file.getContent());
  }

  /**
   * Sends a register to the insurer (PRCID.003/007/008).
   *
   * @param id extract
   * @param request recipients
   * @return extract
   */
  @PostMapping("/extracts/{id}/send")
  @PreAuthorize(ReconAccess.SEND)
  public ExtractResponse send(@PathVariable Long id, @Valid @RequestBody SendRequest request) {
    return ExtractResponse.from(sender.send(id, request.to(), request.cc()));
  }

  /**
   * Uploads an insurer production report (PRCID.009/010).
   *
   * @param companyId company
   * @param file file
   * @return run and attempts
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(ReconAccess.PROCESS)
  public UploadResultResponse upload(@RequestParam Long companyId, @RequestParam MultipartFile file)
      throws IOException {
    return UploadResultResponse.from(
        uploads.upload(companyId, file.getOriginalFilename(), file.getBytes()));
  }

  /**
   * Upload history (PRCID.031/032).
   *
   * @param companyId company
   * @param insurer insurer
   * @param page page
   * @param size size
   * @return attempts
   */
  @GetMapping("/uploads")
  @PreAuthorize(ReconAccess.READ)
  public PageResponse<UploadResponse> uploads(
      @RequestParam Long companyId,
      @RequestParam(required = false) String insurer,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        uploads.history(
            companyId,
            insurer == null || insurer.isBlank() ? null : insurer.strip(),
            ReconAccess.page(page, size)),
        UploadResponse::from);
  }

  /**
   * Extraction schedules (PRCID.001).
   *
   * @param companyId company
   * @return schedules
   */
  @GetMapping("/schedules")
  @PreAuthorize(ReconAccess.READ)
  public List<ScheduleResponse> schedules(@RequestParam Long companyId) {
    return schedules.list(companyId).stream().map(ScheduleResponse::from).toList();
  }

  /**
   * Adds a schedule.
   *
   * @param request schedule
   * @return schedule
   */
  @PostMapping("/schedules")
  @PreAuthorize(ReconAccess.PROCESS)
  public ScheduleResponse createSchedule(@Valid @RequestBody ScheduleRequest request) {
    if (request.companyId() == null
        || request.insurerCode() == null
        || request.insurerCode().isBlank()) {
      throw new BusinessRuleException(
          "RECON_SCHEDULE_INSURER", "Choose the company and the insurer of the schedule");
    }
    return ScheduleResponse.from(
        schedules.create(request.companyId(), request.insurerCode(), request.terms()));
  }

  /**
   * Changes a schedule.
   *
   * @param id schedule
   * @param request schedule
   * @return schedule
   */
  @PutMapping("/schedules/{id}")
  @PreAuthorize(ReconAccess.PROCESS)
  public ScheduleResponse updateSchedule(
      @PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
    return ScheduleResponse.from(schedules.update(id, request.terms()));
  }

  /**
   * The reconciliation parameters (PRCID.022/026).
   *
   * @return tolerance and keys
   */
  @GetMapping("/settings")
  @PreAuthorize(ReconAccess.READ)
  public SettingsResponse settings() {
    return new SettingsResponse(
        settings.tolerance(), settings.keys().stream().map(Enum::name).toList());
  }
}
