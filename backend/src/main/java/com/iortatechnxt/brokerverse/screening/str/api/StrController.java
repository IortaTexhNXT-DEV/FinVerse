package com.iortatechnxt.brokerverse.screening.str.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.docgen.service.DocumentFormat;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.ReviewDto;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseQueries;
import com.iortatechnxt.brokerverse.screening.str.api.dto.StrDtos;
import com.iortatechnxt.brokerverse.screening.str.api.dto.StrDtos.ExtractionDto;
import com.iortatechnxt.brokerverse.screening.str.api.dto.StrDtos.StrDetail;
import com.iortatechnxt.brokerverse.screening.str.api.dto.StrDtos.StrRow;
import com.iortatechnxt.brokerverse.screening.str.domain.StrStatus;
import com.iortatechnxt.brokerverse.screening.str.domain.SuspiciousTransactionReport;
import com.iortatechnxt.brokerverse.screening.str.service.StrDocumentService;
import com.iortatechnxt.brokerverse.screening.str.service.StrExtractionService;
import com.iortatechnxt.brokerverse.screening.str.service.StrFilingService;
import com.iortatechnxt.brokerverse.screening.str.service.StrService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
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

/**
 * Suspicious transaction reports (SNSRP-705, 706; FR-SS-070 to 072): the STR register, the STR of a
 * case (prepare, save, completeness, mark ready, PDF / Word), the extraction of committee-approved
 * STRs with the file download, and the AMLC filing reference.
 */
@RestController
@RequestMapping("/api/v1/screening")
public class StrController {

  private static final String HAS_VIEW = "hasAuthority('SCR_VIEW')";
  private static final String HAS_PREPARE = "hasAuthority('SCR_COMPLIANCE_REVIEW')";
  private static final String HAS_EXTRACT = "hasAuthority('SCR_STR_EXTRACT')";
  private static final String HAS_REGISTER =
      "hasAnyAuthority('SCR_COMPLIANCE_REVIEW','SCR_STR_EXTRACT','SCR_AUDIT_VIEW')";
  private static final int PAGE_SIZE = 25;

  private final StrService strs;
  private final StrExtractionService extractions;
  private final StrFilingService filings;
  private final StrDocumentService documents;
  private final CaseQueries cases;

  /**
   * Creates the controller.
   *
   * @param strs STR preparation
   * @param extractions extraction
   * @param filings filing
   * @param documents PDF / Word
   * @param cases case reads (scope)
   */
  public StrController(
      StrService strs,
      StrExtractionService extractions,
      StrFilingService filings,
      StrDocumentService documents,
      CaseQueries cases) {
    this.strs = strs;
    this.extractions = extractions;
    this.filings = filings;
    this.documents = documents;
    this.cases = cases;
  }

  /**
   * The STR register of a company, newest first.
   *
   * @param companyId company
   * @param status status, blank for all
   * @param page page
   * @return STRs
   */
  @GetMapping("/str")
  @PreAuthorize(HAS_REGISTER)
  public PageResponse<StrRow> register(
      @RequestParam Long companyId,
      @RequestParam(required = false) StrStatus status,
      @RequestParam(required = false) Integer page) {
    return PageResponse.of(
        strs.register(
            companyId, status, PageRequest.of(page == null ? 0 : Math.max(0, page), PAGE_SIZE)),
        StrRow::from);
  }

  /**
   * The STR of a case; 204 before it is prepared.
   *
   * @param caseId the case
   * @return the STR
   */
  @GetMapping("/cases/{caseId}/str")
  @PreAuthorize(HAS_VIEW)
  public ResponseEntity<StrDetail> ofCase(@PathVariable Long caseId) {
    cases.get(caseId);
    return strs.ofCase(caseId)
        .map(this::detail)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * Prepares (or opens) the STR of a case in STR_PREPARATION (FR-SS-070).
   *
   * @param caseId the case
   * @return the STR
   */
  @PostMapping("/cases/{caseId}/str")
  @PreAuthorize(HAS_PREPARE)
  public StrDetail prepare(@PathVariable Long caseId) {
    return detail(strs.prepare(caseId));
  }

  /**
   * Saves a draft STR.
   *
   * @param id the STR
   * @param request values, reason codes and transactions
   * @return the STR
   */
  @PutMapping("/str/{id}")
  @PreAuthorize(HAS_PREPARE)
  public StrDetail save(@PathVariable Long id, @Valid @RequestBody StrDtos.Save request) {
    return detail(strs.save(id, request.edit()));
  }

  /**
   * Marks a complete STR ready (FR-SS-070).
   *
   * @param id the STR
   * @return the STR
   */
  @PostMapping("/str/{id}/ready")
  @PreAuthorize(HAS_PREPARE)
  public StrDetail ready(@PathVariable Long id) {
    return detail(strs.markReady(id));
  }

  /**
   * The STR as a PDF or Word document.
   *
   * @param id the STR
   * @param format PDF (default) or DOCX
   * @return the file
   */
  @GetMapping("/str/{id}/document")
  @PreAuthorize(HAS_REGISTER)
  public ResponseEntity<byte[]> document(
      @PathVariable Long id, @RequestParam(required = false) DocumentFormat format) {
    StrDocumentService.Rendered file =
        documents.render(id, format == null ? DocumentFormat.PDF : format);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.fileName()))
        .body(file.content());
  }

  /**
   * The STRs an extraction would hold (FR-SS-071 list before confirming).
   *
   * @param request company, period, reason
   * @return STRs
   */
  @PostMapping("/str/extractions/preview")
  @PreAuthorize(HAS_EXTRACT)
  public List<StrRow> preview(@Valid @RequestBody StrDtos.Extract request) {
    return extractions
        .preview(request.companyId(), request.from(), request.to(), request.reason())
        .stream()
        .map(StrRow::from)
        .toList();
  }

  /**
   * Extracts the committee-approved STRs of a period (FR-SS-071).
   *
   * @param request company, period, reason
   * @return the extraction
   */
  @PostMapping("/str/extractions")
  @PreAuthorize(HAS_EXTRACT)
  public ExtractionDto extract(@Valid @RequestBody StrDtos.Extract request) {
    return ExtractionDto.from(
        extractions.extract(request.companyId(), request.from(), request.to(), request.reason()));
  }

  /**
   * The extractions of a company, newest first.
   *
   * @param companyId company
   * @param page page
   * @return extractions
   */
  @GetMapping("/str/extractions")
  @PreAuthorize(HAS_REGISTER)
  public PageResponse<ExtractionDto> extractions(
      @RequestParam Long companyId, @RequestParam(required = false) Integer page) {
    return PageResponse.of(
        extractions.extractions(
            companyId, PageRequest.of(page == null ? 0 : Math.max(0, page), PAGE_SIZE)),
        ExtractionDto::from);
  }

  /**
   * Downloads the file of an extraction.
   *
   * @param id the extraction
   * @return the file
   */
  @GetMapping("/str/extractions/{id}/file")
  @PreAuthorize(HAS_EXTRACT)
  public ResponseEntity<byte[]> file(@PathVariable Long id) {
    RunFile file = extractions.file(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.fileName()))
        .body(file.content());
  }

  /**
   * Records the AMLC filing and closes the case (FR-SS-072).
   *
   * @param id the STR
   * @param request AMLC reference and filing date
   * @return the STR
   */
  @PostMapping("/str/{id}/filing")
  @PreAuthorize(HAS_EXTRACT)
  public StrRow filing(@PathVariable Long id, @Valid @RequestBody StrDtos.Filing request) {
    return StrRow.from(filings.file(id, request.reference(), request.filedOn()));
  }

  private StrDetail detail(SuspiciousTransactionReport s) {
    return new StrDetail(
        StrRow.from(s),
        s.getSubjectSnapshot(),
        s.getTemplateVersionId(),
        strs.template(s)
            .map(t -> t.fields().stream().map(ReviewDto.FieldDto::from).toList())
            .orElse(List.of()),
        strs.values(s.getId()),
        strs.transactions(s.getId()).stream().map(StrDtos.Transaction::from).toList(),
        strs.gaps(s.getId()));
  }
}
