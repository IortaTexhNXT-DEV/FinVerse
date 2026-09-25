package com.iortatechnxt.brokerverse.commission.api;

import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.CommentRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.SettingsResponse;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.CertificateRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.CertificateResponse;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.EstimateRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.EstimatedResponse;
import com.iortatechnxt.brokerverse.commission.api.dto.IncentiveDtos.ReasonRequest;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission.OrLink;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.service.CertificateService;
import com.iortatechnxt.brokerverse.commission.service.DpBillingService;
import com.iortatechnxt.brokerverse.commission.service.DpResponseHandler;
import com.iortatechnxt.brokerverse.commission.service.EstimatedItemService;
import com.iortatechnxt.brokerverse.commission.service.FeedbackCalendar;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
 * BIR certificate submissions (CMRID.010/015), estimated items (RMTID.037), the billing file and
 * the upload of insurer answers (CMRID.009), and the commission settings shown on the screens.
 */
@RestController
@RequestMapping("/api/v1/commission")
public class CommissionController {

  private final CertificateService certificates;
  private final EstimatedItemService estimated;
  private final DpBillingService billings;
  private final ExtractRepositoryService repository;
  private final FlowInService flowIn;
  private final SystemParameterService parameters;

  /**
   * Creates the controller.
   *
   * @param certificates certificate submissions
   * @param estimated estimated items
   * @param billings billings
   * @param repository extract repository (billing files)
   * @param flowIn flow-in (insurer answers)
   * @param parameters business parameters
   */
  public CommissionController(
      CertificateService certificates,
      EstimatedItemService estimated,
      DpBillingService billings,
      ExtractRepositoryService repository,
      FlowInService flowIn,
      SystemParameterService parameters) {
    this.certificates = certificates;
    this.estimated = estimated;
    this.billings = billings;
    this.repository = repository;
    this.flowIn = flowIn;
    this.parameters = parameters;
  }

  /**
   * Certificate submissions of a company.
   *
   * @param companyId company
   * @param stage stage
   * @param page page
   * @param size size
   * @return submissions
   */
  @GetMapping("/certificates")
  @PreAuthorize(CommissionAccess.CERT_READ)
  public PageResponse<CertificateResponse> certificates(
      @RequestParam Long companyId,
      @RequestParam(required = false) String stage,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        certificates.search(
            companyId,
            stage == null || stage.isBlank() ? null : stage,
            CommissionAccess.page(page, size)),
        CertificateResponse::from);
  }

  /**
   * A submission.
   *
   * @param id submission
   * @return submission
   */
  @GetMapping("/certificates/{id}")
  @PreAuthorize(CommissionAccess.CERT_READ)
  public CertificateResponse certificate(@PathVariable Long id) {
    return CertificateResponse.from(certificates.require(id));
  }

  /**
   * ORs of the commission collected from an insurer (to tag a certificate to).
   *
   * @param companyId company
   * @param insurer insurer
   * @return ORs with amounts
   */
  @GetMapping("/certificates/receipts")
  @PreAuthorize(CommissionAccess.CERT_READ)
  public List<OrLink> receipts(@RequestParam Long companyId, @RequestParam String insurer) {
    return certificates.receiptsOf(companyId, insurer.strip());
  }

  /**
   * Submits a certificate to Comptrollership (CMRID.015).
   *
   * @param request certificate and ORs
   * @return submission
   */
  @PostMapping("/certificates")
  @PreAuthorize(CommissionAccess.CERT_SUBMIT)
  public CertificateResponse submit(@Valid @RequestBody CertificateRequest request) {
    if (request.companyId() == null
        || request.insurerCode() == null
        || request.insurerCode().isBlank()) {
      throw new BusinessRuleException(
          "CERT_INSURER_REQUIRED", "Choose the company and the insurer of the certificate");
    }
    return CertificateResponse.from(
        certificates.submit(request.companyId(), request.insurerCode(), request.toCertificate()));
  }

  /**
   * Corrects and resubmits a rejected submission.
   *
   * @param id submission
   * @param request certificate and ORs
   * @return submission
   */
  @PutMapping("/certificates/{id}")
  @PreAuthorize(CommissionAccess.CERT_SUBMIT)
  public CertificateResponse resubmit(
      @PathVariable Long id, @Valid @RequestBody CertificateRequest request) {
    return CertificateResponse.from(certificates.resubmit(id, request.toCertificate()));
  }

  /**
   * Acknowledges a submission (Comptrollership).
   *
   * @param id submission
   * @param request comment
   * @return submission
   */
  @PostMapping("/certificates/{id}/acknowledge")
  @PreAuthorize(CommissionAccess.CERT_ACK)
  public CertificateResponse acknowledge(
      @PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return CertificateResponse.from(certificates.acknowledge(id, request.comment()));
  }

  /**
   * Rejects a submission with a reason (Comptrollership).
   *
   * @param id submission
   * @param request reason
   * @return submission
   */
  @PostMapping("/certificates/{id}/reject")
  @PreAuthorize(CommissionAccess.CERT_ACK)
  public CertificateResponse reject(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return CertificateResponse.from(certificates.reject(id, request.reason()));
  }

  /**
   * Invoices flagged estimated (RMTID.037).
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return invoices
   */
  @GetMapping("/estimated")
  @PreAuthorize(CommissionAccess.READ)
  public PageResponse<EstimatedResponse> estimated(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        estimated.estimated(companyId, CommissionAccess.page(page, size)), EstimatedResponse::from);
  }

  /**
   * Flags or clears an invoice as estimated.
   *
   * @param request invoice, flag and reason
   * @return invoice
   */
  @PostMapping("/estimated")
  @PreAuthorize(CommissionAccess.PROCESS)
  public EstimatedResponse estimate(@Valid @RequestBody EstimateRequest request) {
    return EstimatedResponse.from(
        estimated.flag(request.invoiceNo().strip(), request.estimated(), request.reason()));
  }

  /**
   * Downloads a billing file.
   *
   * @param id billing
   * @return the workbook
   */
  @GetMapping("/dp/billings/{id}/file")
  @PreAuthorize(CommissionAccess.READ)
  public ResponseEntity<byte[]> billingFile(@PathVariable Long id) {
    DpBilling billing = billings.require(id);
    if (billing.getFileId() == null) {
      throw new ResourceNotFoundException("Billing file", billing.getBillingNo());
    }
    ExtractFile file = repository.download(billing.getFileId());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(file.getFileName()))
        .body(file.getContent());
  }

  /**
   * Uploads the insurer's answers to billings (flow-in feed INSURER_DP_RESPONSE, CMRID.009).
   *
   * @param file answered billing file
   * @return run number, status and message
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/dp/responses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(CommissionAccess.PROCESS)
  public Map<String, String> responses(@RequestParam MultipartFile file) throws IOException {
    FlowInRun run =
        flowIn.upload(
            DpResponseHandler.FEED, new FlowInFile(file.getOriginalFilename(), file.getBytes()));
    return Map.of(
        "runNo", run.getRunNo(),
        "status", run.getStatus().name(),
        "message", run.getMessage() == null ? "" : run.getMessage());
  }

  /**
   * The commission settings shown on the screens.
   *
   * @return feedback days, proposed bank and PR reversal posting
   */
  @GetMapping("/settings")
  @PreAuthorize(CommissionAccess.READ)
  public SettingsResponse settings() {
    return new SettingsResponse(
        parameters.intValue("CMR_FEEDBACK_WORKING_DAYS", FeedbackCalendar.DEFAULT_DAYS),
        parameters.text("CMR_DP_COLLECTION_BANK", ""),
        Boolean.parseBoolean(parameters.text("DP_PR_REVERSAL_POSTING", "false").strip()));
  }
}
