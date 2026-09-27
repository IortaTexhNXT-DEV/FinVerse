package com.iortatechnxt.brokerverse.disbursement.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.RequestDtos.EncodeRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.RequestDtos.ReasonComment;
import com.iortatechnxt.brokerverse.disbursement.api.dto.RequestDtos.RequestResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.RequestDtos.SummaryResponse;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest.RequestFacts;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementQueryService;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementQueryService.RequestSearch;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementSettings;
import com.iortatechnxt.brokerverse.disbursement.service.RequestIntakeService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment requests and the workbench counts (DIS 2.4.0-2.4.4, 2.6.1-2.6.2, 3.25.0): requests by
 * status and received date, one request, a request encoded from an e-mail, the voucher of a
 * received request, the return of a request to its source and the release of BIR 2307 certificates.
 */
@RestController
@RequestMapping("/api/v1/disbursement")
public class RequestController {

  private final RequestIntakeService intake;
  private final DisbursementQueryService queries;

  /**
   * Creates the controller.
   *
   * @param intake request intake
   * @param queries reads
   */
  public RequestController(RequestIntakeService intake, DisbursementQueryService queries) {
    this.intake = intake;
    this.queries = queries;
  }

  /**
   * The counts of the workbench tabs.
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/summary")
  @PreAuthorize(DisbursementAccess.READ)
  public SummaryResponse summary(@RequestParam Long companyId) {
    return SummaryResponse.from(queries.summary(companyId));
  }

  /**
   * Payment requests, newest first (DIS 2.4.0-2.4.1).
   *
   * @param companyId company
   * @param status statuses
   * @param from received from
   * @param to received to
   * @param q request, RFP, source reference or payee
   * @param page page
   * @param size size
   * @return requests
   */
  @GetMapping("/requests")
  @PreAuthorize(DisbursementAccess.READ)
  public PageResponse<RequestResponse> requests(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<RequestStatus> status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.requests(
            new RequestSearch(companyId, status, from, to, q),
            DisbursementAccess.newestFirst(page, size)),
        RequestResponse::from);
  }

  /**
   * One payment request.
   *
   * @param id request
   * @return request
   */
  @GetMapping("/requests/{id}")
  @PreAuthorize(DisbursementAccess.READ)
  public RequestResponse request(@PathVariable Long id) {
    return RequestResponse.from(intake.get(id));
  }

  /**
   * Encodes a request received by e-mail (DIS 2.6.1): its voucher is created with the processor.
   *
   * @param body request
   * @return request
   */
  @PostMapping("/requests")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(DisbursementAccess.PROCESS)
  public RequestResponse encode(@Valid @RequestBody EncodeRequest body) {
    return RequestResponse.from(
        intake.register(
            new RequestFacts(
                body.companyId(),
                RequestSource.ENCODED,
                DisbursementSettings.MODULE,
                null,
                body.rfpNo(),
                body.disbursementType().toUpperCase(Locale.ROOT),
                null,
                body.payeeCode().strip(),
                body.payeeName(),
                body.currency(),
                body.amount(),
                body.purpose().strip(),
                body.rootInvoiceNo(),
                List.of(),
                List.of(),
                false,
                null,
                null,
                body.expenseAccount(),
                body.costCenter())));
  }

  /**
   * Creates the voucher of a received request (DIS 2.7.5).
   *
   * @param id request
   * @return request
   */
  @PostMapping("/requests/{id}/voucher")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public RequestResponse createVoucher(@PathVariable Long id) {
    return RequestResponse.from(intake.createVoucher(id));
  }

  /**
   * Returns a request without voucher to its source (DIS 3.25.0).
   *
   * @param id request
   * @param body reason
   * @return request
   */
  @PostMapping("/requests/{id}/return")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public RequestResponse returnToSource(
      @PathVariable Long id, @Valid @RequestBody ReasonComment body) {
    return RequestResponse.from(intake.returnToSource(id, body.reasonCode(), body.comment()));
  }

  /**
   * Releases BIR 2307 certificates requested without a payment (DBMID.001).
   *
   * @param id request
   * @return request
   */
  @PostMapping("/requests/{id}/release")
  @PreAuthorize(DisbursementAccess.PROCESS)
  public RequestResponse release(@PathVariable Long id) {
    return RequestResponse.from(intake.release(id));
  }
}
