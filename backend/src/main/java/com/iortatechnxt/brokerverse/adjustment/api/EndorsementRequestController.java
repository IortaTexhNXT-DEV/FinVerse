package com.iortatechnxt.brokerverse.adjustment.api;

import com.iortatechnxt.brokerverse.adjustment.api.dto.RecomputeResponse;
import com.iortatechnxt.brokerverse.adjustment.api.dto.RequestInput;
import com.iortatechnxt.brokerverse.adjustment.api.dto.RequestResponse;
import com.iortatechnxt.brokerverse.adjustment.api.dto.RequestSummaryResponse;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest.Content;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentDocuments;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentDocuments.Generated;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService;
import com.iortatechnxt.brokerverse.adjustment.service.AdjustmentQueryService.GlLine;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService.Preview;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
 * Endorsement requests (ADJID.001-004/008/014/015/018/020-024, MKTID.008): search, one request,
 * preview and recompute, raise and change, the requests of an invoice, the endorsement slip and the
 * validation slip.
 */
@RestController
@RequestMapping("/api/v1/adjustment")
public class EndorsementRequestController {

  private final EndorsementRequestService requests;
  private final AdjustmentQueryService queries;
  private final AdjustmentDocuments documents;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param requests raise, change and preview
   * @param queries reads
   * @param documents slips
   * @param clock clock (aging)
   */
  public EndorsementRequestController(
      EndorsementRequestService requests,
      AdjustmentQueryService queries,
      AdjustmentDocuments documents,
      Clock clock) {
    this.requests = requests;
    this.queries = queries;
    this.documents = documents;
    this.clock = clock;
  }

  /**
   * Searches requests.
   *
   * @param companyId company
   * @param stage stage, empty for all
   * @param q request, invoice, ARN, policy, assured or endorsement reference
   * @param page page
   * @param size size
   * @return requests, newest first
   */
  @GetMapping("/requests")
  @PreAuthorize(AdjustmentAccess.VIEW)
  public PageResponse<RequestSummaryResponse> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) RequestStage stage,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), AdjustmentAccess.MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "id"));
    var now = clock.instant();
    return PageResponse.of(
        queries.search(companyId, stage, q, pageable), r -> RequestSummaryResponse.from(r, now));
  }

  /**
   * Number of requests per stage (work list tabs).
   *
   * @param companyId company
   * @return count per stage
   */
  @GetMapping("/requests/counts")
  @PreAuthorize(AdjustmentAccess.VIEW)
  public Map<RequestStage, Long> counts(@RequestParam Long companyId) {
    return queries.counts(companyId);
  }

  /**
   * One request.
   *
   * @param id request
   * @return request
   */
  @GetMapping("/requests/{id}")
  @PreAuthorize(AdjustmentAccess.VIEW)
  public RequestResponse get(@PathVariable Long id) {
    return RequestResponse.from(queries.get(id), clock.instant());
  }

  /**
   * Recompute of a request with the amounts in force.
   *
   * @param id request
   * @return recompute
   */
  @GetMapping("/requests/{id}/recompute")
  @PreAuthorize(AdjustmentAccess.VIEW)
  public RecomputeResponse recompute(@PathVariable Long id) {
    return response(requests.recompute(id));
  }

  /**
   * GL entries posted for a request (ADJID.017).
   *
   * @param id request
   * @return journal lines
   */
  @GetMapping("/requests/{id}/journal")
  @PreAuthorize(AdjustmentAccess.VIEW)
  public List<GlLine> journal(@PathVariable Long id) {
    return queries.journalLines(queries.get(id));
  }

  /**
   * Requests of an invoice (ADJID.020/024).
   *
   * @param invoiceNo invoice
   * @return requests, newest first
   */
  @GetMapping("/invoices/{invoiceNo}/requests")
  @PreAuthorize(AdjustmentAccess.VIEW)
  public List<RequestSummaryResponse> forInvoice(@PathVariable String invoiceNo) {
    var now = clock.instant();
    return queries.forInvoice(invoiceNo).stream()
        .map(r -> RequestSummaryResponse.from(r, now))
        .toList();
  }

  /**
   * Validates and recomputes a request before it is raised (new request wizard).
   *
   * @param input request on the first invoice
   * @return preview with the possible duplicates
   */
  @PostMapping("/requests/preview")
  @PreAuthorize(AdjustmentAccess.REQUEST)
  public RecomputeResponse preview(@Valid @RequestBody RequestInput input) {
    return response(requests.preview(input.toDraft(requireInvoice(input))));
  }

  /**
   * Raises one request per invoice.
   *
   * @param input invoices and terms
   * @return the draft requests
   */
  @PostMapping("/requests")
  @PreAuthorize(AdjustmentAccess.REQUEST)
  public List<RequestResponse> create(@Valid @RequestBody RequestInput input) {
    requireInvoice(input);
    return requests.create(input.invoiceNos(), input.toDraft(null)).stream()
        .map(r -> get(r.getId()))
        .toList();
  }

  /**
   * Changes a draft or returned request.
   *
   * @param id request
   * @param input terms
   * @return the request
   */
  @PutMapping("/requests/{id}")
  @PreAuthorize(AdjustmentAccess.REQUEST)
  public RequestResponse update(@PathVariable Long id, @Valid @RequestBody RequestInput input) {
    requests.update(id, input.toDraft(null));
    return get(id);
  }

  /**
   * The endorsement slip (ADJID.015, MKTID.008).
   *
   * @param id request
   * @return PDF
   */
  @GetMapping("/requests/{id}/endorsement-slip")
  @PreAuthorize(AdjustmentAccess.SLIP)
  public ResponseEntity<byte[]> endorsementSlip(@PathVariable Long id) {
    return pdf(documents.endorsementSlip(id));
  }

  /**
   * The validation slip (ADJID.018).
   *
   * @param id request
   * @return PDF
   */
  @GetMapping("/requests/{id}/validation-slip")
  @PreAuthorize(AdjustmentAccess.SLIP)
  public ResponseEntity<byte[]> validationSlip(@PathVariable Long id) {
    return pdf(documents.validationSlip(id));
  }

  private static ResponseEntity<byte[]> pdf(Generated document) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(document.fileName()))
        .body(document.content());
  }

  private static String requireInvoice(RequestInput input) {
    String first = input.firstInvoice();
    if (first == null || first.isBlank()) {
      throw new BusinessRuleException("ADJ_INVOICES_REQUIRED", "Select at least one invoice");
    }
    return first.strip();
  }

  private static RecomputeResponse response(Preview preview) {
    Content c = preview.content();
    return RecomputeResponse.from(
        new RecomputeResponse.Kind(
            c.requestClass().name(), c.computation().name(), c.negative(), c.needsApproval()),
        preview.recompute(),
        preview.duplicates());
  }
}
