package com.iortatechnxt.brokerverse.remittance.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.AccountResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.DtipRow;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.EodRequestBody;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.EodResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.ExtractionRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.FeedRunResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.IncentiveRuleRequest;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.IncentiveRuleResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.OrUploadResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.RunResponse;
import com.iortatechnxt.brokerverse.remittance.api.dto.RemittanceDtos.TagResponse;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.service.ExtractionService;
import com.iortatechnxt.brokerverse.remittance.service.IncentiveRuleService;
import com.iortatechnxt.brokerverse.remittance.service.InsurerOrUploads;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceQueryService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * Remittance extraction and look-ups (RMTID.001/003/004/005/023-025/028): extraction runs and their
 * tags, account search across batches, the DTIP status of the ledger invoices, end-of-day requests,
 * the early-remittance incentive rules and the insurer OR uploads with their exception report
 * (RMTID.012/013/016).
 */
@RestController
@RequestMapping("/api/v1/remittance")
public class RemittanceController {

  private final ExtractionService extraction;
  private final RemittanceQueryService queries;
  private final IncentiveRuleService incentives;
  private final InsurerOrUploads insurerOrs;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param extraction extraction
   * @param queries reads
   * @param incentives incentive rules
   * @param insurerOrs insurer OR uploads
   * @param clock clock
   */
  public RemittanceController(
      ExtractionService extraction,
      RemittanceQueryService queries,
      IncentiveRuleService incentives,
      InsurerOrUploads insurerOrs,
      Clock clock) {
    this.extraction = extraction;
    this.queries = queries;
    this.incentives = incentives;
    this.insurerOrs = insurerOrs;
    this.clock = clock;
  }

  /**
   * Extraction runs, newest first (RMTID.003).
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return runs
   */
  @GetMapping("/runs")
  @PreAuthorize(RemittanceAccess.TEAM)
  public PageResponse<RunResponse> runs(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.runs(companyId, RemittanceAccess.plain(page, size)), RunResponse::from);
  }

  /**
   * One run.
   *
   * @param id run
   * @return run
   */
  @GetMapping("/runs/{id}")
  @PreAuthorize(RemittanceAccess.TEAM)
  public RunResponse run(@PathVariable Long id) {
    return RunResponse.from(queries.run(id));
  }

  /**
   * The tags a run gave (RMTID.003).
   *
   * @param id run
   * @param page page
   * @param size size
   * @return tags
   */
  @GetMapping("/runs/{id}/tags")
  @PreAuthorize(RemittanceAccess.TEAM)
  public PageResponse<TagResponse> tags(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(queries.tags(id, RemittanceAccess.plain(page, size)), TagResponse::from);
  }

  /**
   * Runs an extraction now for an insurer and type, or for one invoice (RMTID.001/004).
   *
   * @param request scope
   * @return the finished run
   */
  @PostMapping("/runs")
  @PreAuthorize(RemittanceAccess.EXTRACT)
  @ResponseStatus(HttpStatus.CREATED)
  public RunResponse extract(@Valid @RequestBody ExtractionRequest request) {
    String invoiceNo = blankToNull(request.invoiceNo());
    return RunResponse.from(
        extraction.run(
            request.companyId(),
            new Scope(
                invoiceNo == null ? ExtractionTrigger.MANUAL : ExtractionTrigger.MANUAL_INVOICE,
                blankToNull(request.insurerCode()),
                request.type(),
                invoiceNo),
            LocalDate.now(clock)));
  }

  /**
   * Queues an invoice for the end-of-day extraction (RMTID.005).
   *
   * @param request invoice
   * @return the request
   */
  @PostMapping("/eod-requests")
  @PreAuthorize(RemittanceAccess.PROCESS)
  public EodResponse queue(@Valid @RequestBody EodRequestBody request) {
    return EodResponse.from(
        extraction.queueForEndOfDay(request.companyId(), request.invoiceNo().strip()));
  }

  /**
   * Recent end-of-day requests.
   *
   * @param companyId company
   * @return requests
   */
  @GetMapping("/eod-requests")
  @PreAuthorize(RemittanceAccess.TEAM)
  public List<EodResponse> endOfDayRequests(@RequestParam Long companyId) {
    return queries.endOfDayRequests(companyId).stream().map(EodResponse::from).toList();
  }

  /**
   * Accounts in batches by invoice, batch, endorsement, policy or assured (RMTID.025).
   *
   * @param companyId company
   * @param q text
   * @param page page
   * @param size size
   * @return accounts
   */
  @GetMapping("/accounts")
  @PreAuthorize(RemittanceAccess.TEAM)
  public PageResponse<AccountResponse> accounts(
      @RequestParam Long companyId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.lines(companyId, q, RemittanceAccess.newestFirst(page, size)),
        AccountResponse::from);
  }

  /**
   * DTIP status of the ledger invoices (RMTID.028/032).
   *
   * @param companyId company
   * @param q invoice, ARN, policy, client or assured
   * @param insurer insurer
   * @param status remittance status
   * @param payment payment status
   * @param page page
   * @param size size
   * @return invoices with their DTIP and tag
   */
  @GetMapping("/dtip")
  @PreAuthorize(RemittanceAccess.TEAM)
  public PageResponse<DtipRow> dtip(
      @RequestParam Long companyId,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String insurer,
      @RequestParam(required = false) RemittanceStatus status,
      @RequestParam(required = false) PaymentStatus payment,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    LedgerSearch search =
        new LedgerSearch(
            companyId, q, insurer, null, payment, status, null, null, false, null, null);
    return PageResponse.of(
        queries.dtipStatus(search, RemittanceAccess.newestFirst(page, size)), DtipRow::from);
  }

  /**
   * Early-remittance incentive rules (RMTID.023).
   *
   * @param companyId company
   * @return rules
   */
  @GetMapping("/incentive-rules")
  @PreAuthorize(RemittanceAccess.TEAM)
  public List<IncentiveRuleResponse> incentiveRules(@RequestParam Long companyId) {
    return incentives.list(companyId).stream().map(IncentiveRuleResponse::from).toList();
  }

  /**
   * Adds an incentive rule.
   *
   * @param request rule
   * @return rule
   */
  @PostMapping("/incentive-rules")
  @PreAuthorize(RemittanceAccess.APPROVE)
  @ResponseStatus(HttpStatus.CREATED)
  public IncentiveRuleResponse createRule(@Valid @RequestBody IncentiveRuleRequest request) {
    if (request.companyId() == null) {
      throw new BusinessRuleException("COMPANY_REQUIRED", "Choose the company of the rule");
    }
    return IncentiveRuleResponse.from(incentives.create(request.companyId(), request.terms()));
  }

  /**
   * Changes an incentive rule.
   *
   * @param id rule
   * @param request rule
   * @return rule
   */
  @PutMapping("/incentive-rules/{id}")
  @PreAuthorize(RemittanceAccess.APPROVE)
  public IncentiveRuleResponse updateRule(
      @PathVariable Long id, @Valid @RequestBody IncentiveRuleRequest request) {
    return IncentiveRuleResponse.from(incentives.update(id, request.terms()));
  }

  /**
   * Uploads an insurer OR schedule and returns its exception report (RMTID.012/013/016).
   *
   * @param file CSV with batchNo, invoiceNo, orNo, orDate, orAmount
   * @return run and exception report
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/insurer-or/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(RemittanceAccess.OR_UPLOAD)
  public OrUploadResponse uploadInsurerOr(@RequestParam MultipartFile file) throws IOException {
    return OrUploadResponse.from(
        insurerOrs.upload(new FlowInFile(file.getOriginalFilename(), file.getBytes())));
  }

  /**
   * Insurer OR uploads, newest first.
   *
   * @param page page
   * @param size size
   * @return runs
   */
  @GetMapping("/insurer-or/runs")
  @PreAuthorize(RemittanceAccess.TEAM)
  public PageResponse<FeedRunResponse> insurerOrRuns(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        insurerOrs.runs(RemittanceAccess.plain(page, size)), FeedRunResponse::from);
  }

  /**
   * The exception report of an insurer OR upload (RMTID.016).
   *
   * @param id run
   * @return run and exception report
   */
  @GetMapping("/insurer-or/runs/{id}")
  @PreAuthorize(RemittanceAccess.TEAM)
  public OrUploadResponse insurerOrRun(@PathVariable Long id) {
    return OrUploadResponse.from(insurerOrs.result(id));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
