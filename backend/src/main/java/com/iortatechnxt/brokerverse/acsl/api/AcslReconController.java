package com.iortatechnxt.brokerverse.acsl.api;

import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.ControlInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.SoaViews.ControlView;
import com.iortatechnxt.brokerverse.acsl.api.dto.SoaViews.GlSlRowView;
import com.iortatechnxt.brokerverse.acsl.api.dto.SoaViews.GlSlRunView;
import com.iortatechnxt.brokerverse.acsl.api.dto.SoaViews.LayoutView;
import com.iortatechnxt.brokerverse.acsl.api.dto.SoaViews.LogView;
import com.iortatechnxt.brokerverse.acsl.api.dto.SoaViews.ResultView;
import com.iortatechnxt.brokerverse.acsl.api.dto.SoaViews.UploadView;
import com.iortatechnxt.brokerverse.acsl.domain.ReconBucket;
import com.iortatechnxt.brokerverse.acsl.domain.SoaUpload;
import com.iortatechnxt.brokerverse.acsl.report.SoaReconReport;
import com.iortatechnxt.brokerverse.acsl.service.AcslQueryService;
import com.iortatechnxt.brokerverse.acsl.service.GlSlReconciliationService;
import com.iortatechnxt.brokerverse.acsl.service.SoaUploadService;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.core.ReportService.RenderedReport;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Reconciliations of ACSL (ACSL 2.4.0, 2.13.0-2.14.1): insurer SOA uploads with their upload log,
 * reconciliation results by bucket and the reconciliation report named "Insurer_Covered period";
 * the SOA layouts; the GL-SL reconciliation runs, their accounts and the sub-ledger configuration.
 */
@RestController
@RequestMapping("/api/v1/acsl")
public class AcslReconController {

  private static final String UPLOAD = "/soa-uploads/{id}";
  private static final long MAX_BYTES = 10L * 1024 * 1024;

  private final AcslQueryService queries;
  private final SoaUploadService soa;
  private final GlSlReconciliationService glsl;
  private final ReportService reports;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param queries reads
   * @param soa SOA uploads
   * @param glsl GL-SL reconciliation
   * @param reports report export
   * @param clock clock
   */
  public AcslReconController(
      AcslQueryService queries,
      SoaUploadService soa,
      GlSlReconciliationService glsl,
      ReportService reports,
      Clock clock) {
    this.queries = queries;
    this.soa = soa;
    this.glsl = glsl;
    this.reports = reports;
    this.clock = clock;
  }

  /**
   * SOA uploads.
   *
   * @param companyId company
   * @param q insurer or upload number
   * @param page page
   * @param size size
   * @return uploads, newest first
   */
  @GetMapping("/soa-uploads")
  @PreAuthorize(AcslAccess.VIEW)
  public PageResponse<UploadView> uploads(
      @RequestParam Long companyId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.uploads(companyId, q, AcslCaseController.page(page, size)), this::view);
  }

  /**
   * One upload with its latest reconciliation counts.
   *
   * @param id upload
   * @return upload
   */
  @GetMapping(UPLOAD)
  @PreAuthorize(AcslAccess.VIEW)
  public UploadView upload(@PathVariable Long id) {
    return view(queries.upload(id));
  }

  /**
   * The upload log (ACSL 2.4.0).
   *
   * @param id upload
   * @return rows
   */
  @GetMapping(UPLOAD + "/log")
  @PreAuthorize(AcslAccess.VIEW)
  public List<LogView> log(@PathVariable Long id) {
    return queries.uploadLog(id).stream().map(LogView::from).toList();
  }

  /**
   * Reconciliation results of the latest run (ACSL 2.14.1).
   *
   * @param id upload
   * @param bucket bucket, empty for all
   * @return results
   */
  @GetMapping(UPLOAD + "/results")
  @PreAuthorize(AcslAccess.VIEW)
  public List<ResultView> results(
      @PathVariable Long id, @RequestParam(required = false) ReconBucket bucket) {
    return queries.results(queries.upload(id), bucket).stream().map(ResultView::from).toList();
  }

  /**
   * The reconciliation report of an upload as Excel, named "Insurer_Covered period" (ACSL 2.14.1).
   *
   * @param id upload
   * @return XLSX
   */
  @GetMapping(UPLOAD + "/report")
  @PreAuthorize(AcslAccess.EXPORT)
  public ResponseEntity<byte[]> report(@PathVariable Long id) {
    SoaUpload upload = queries.upload(id);
    RenderedReport file =
        reports.export(
            SoaReconReport.CODE,
            Map.of(
                "companyId",
                String.valueOf(upload.getCompanyId()),
                SoaReconReport.UPLOAD,
                upload.getUploadNo()),
            ExportFormat.XLSX);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDispositions.attachment(upload.reportName() + ".xlsx"))
        .body(file.content());
  }

  /**
   * The SOA layouts (AQ21).
   *
   * @return layouts
   */
  @GetMapping("/soa-layouts")
  @PreAuthorize(AcslAccess.VIEW)
  public List<LayoutView> layouts() {
    return soa.layouts().stream().map(LayoutView::from).toList();
  }

  /**
   * Uploads an insurer SOA and reconciles it (ACSL 2.4.0, 2.13.0).
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param periodFrom first day covered
   * @param periodTo last day covered
   * @param file statement (.xlsx, .ods, .csv, .txt)
   * @return the upload
   * @throws IOException when the file cannot be read
   */
  @PostMapping(value = "/soa-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(AcslAccess.UPLOAD)
  public UploadView uploadSoa(
      @RequestParam Long companyId,
      @RequestParam String insurerCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo,
      @RequestParam MultipartFile file)
      throws IOException {
    if (file.getSize() > MAX_BYTES) {
      throw new BusinessRuleException("ACSL_SOA_TOO_LARGE", "The file must be at most 10 MB");
    }
    return view(
        soa.upload(
            companyId,
            new SoaUpload.Period(insurerCode, periodFrom, periodTo),
            file.getOriginalFilename(),
            file.getBytes()));
  }

  /**
   * Reconciles an upload again.
   *
   * @param id upload
   * @return the upload
   */
  @PostMapping(UPLOAD + "/reconcile")
  @PreAuthorize(AcslAccess.UPLOAD)
  public UploadView reconcile(@PathVariable Long id) {
    return view(soa.reconcile(id));
  }

  /**
   * GL-SL reconciliation runs (ACSL 2.13.2).
   *
   * @param companyId company
   * @return latest runs
   */
  @GetMapping("/gl-sl/runs")
  @PreAuthorize(AcslAccess.VIEW)
  public List<GlSlRunView> runs(@RequestParam Long companyId) {
    return glsl.runs(companyId).stream().map(GlSlRunView::from).toList();
  }

  /**
   * Accounts of a GL-SL run.
   *
   * @param id run
   * @return accounts
   */
  @GetMapping("/gl-sl/runs/{id}/rows")
  @PreAuthorize(AcslAccess.VIEW)
  public List<GlSlRowView> rows(@PathVariable Long id) {
    return glsl.rows(id).stream().map(GlSlRowView::from).toList();
  }

  /**
   * Runs the GL-SL reconciliation now.
   *
   * @param companyId company
   * @param asOf balances as of, today by default
   * @return the run
   */
  @PostMapping("/gl-sl/runs")
  @PreAuthorize(AcslAccess.PROCESS)
  public GlSlRunView run(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate asOf) {
    return GlSlRunView.from(glsl.run(companyId, asOf == null ? LocalDate.now(clock) : asOf));
  }

  /**
   * Sub-ledger configuration of the control accounts.
   *
   * @param companyId company
   * @return configurations
   */
  @GetMapping("/gl-sl/controls")
  @PreAuthorize(AcslAccess.VIEW)
  public List<ControlView> controls(@RequestParam Long companyId) {
    return glsl.controls(companyId).stream().map(ControlView::from).toList();
  }

  /**
   * Sets the sub-ledger of a control account (team leader).
   *
   * @param companyId company
   * @param input account and setting
   * @return the configuration
   */
  @PutMapping("/gl-sl/controls")
  @PreAuthorize(AcslAccess.REVIEW)
  public ControlView configure(
      @RequestParam Long companyId, @Valid @RequestBody ControlInput input) {
    return ControlView.from(
        glsl.configure(companyId, input.accountCode().strip(), input.setting()));
  }

  private UploadView view(SoaUpload upload) {
    return UploadView.from(upload, queries.lastRun(upload).orElse(null));
  }
}
