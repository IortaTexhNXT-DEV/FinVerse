package com.iortatechnxt.brokerverse.payrequest.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.crm.api.dto.PayoutAccountResponse;
import com.iortatechnxt.brokerverse.crm.service.ClientPayoutAccounts;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestInputs.CashAdvanceInput;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestInputs.CheckCancellationInput;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestInputs.RefundInput;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestViews.RequestSummary;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestViews.RequestView;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestViews.ValidationView;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.service.LiquidationService;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestDocuments;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestDocuments.Generated;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestQueryService;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestQueryService.Filter;
import com.iortatechnxt.brokerverse.payrequest.service.RefundValidationService;
import com.iortatechnxt.brokerverse.payrequest.service.RequestFormService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

/**
 * Refund and cash-advance requests (MKT 1.2.0-1.19.0, 2.23.0-2.26.0): the work list, one request
 * with its validations, the forms (raise and change an RRF, an RFP or a check cancellation), the
 * printable request form and the client's known CA / SA information.
 */
@RestController
@RequestMapping("/api/v1/payment-requests")
public class PayRequestController {

  private static final String REQUEST = "/requests/{id}";

  private final PayRequestQueryService queries;
  private final RequestFormService forms;
  private final RefundValidationService validations;
  private final LiquidationService liquidations;
  private final PayRequestDocuments documents;
  private final ClientPayoutAccounts payouts;

  /**
   * Creates the controller.
   *
   * @param queries reads
   * @param forms raise and change
   * @param validations validation tasks
   * @param liquidations liquidations
   * @param documents request forms
   * @param payouts client CA / SA information
   */
  public PayRequestController(
      PayRequestQueryService queries,
      RequestFormService forms,
      RefundValidationService validations,
      LiquidationService liquidations,
      PayRequestDocuments documents,
      ClientPayoutAccounts payouts) {
    this.queries = queries;
    this.forms = forms;
    this.validations = validations;
    this.liquidations = liquidations;
    this.documents = documents;
    this.payouts = payouts;
  }

  /**
   * Searches requests (MKT 1.3.0, 1.6.0, 1.18.0).
   *
   * @param companyId company
   * @param stage stage
   * @param kind kind
   * @param from first request date
   * @param to last request date
   * @param q request, payee, reference or DV number
   * @param page page
   * @param size size
   * @return requests, newest first
   */
  @GetMapping("/requests")
  @PreAuthorize(PayRequestAccess.VIEW)
  public PageResponse<RequestSummary> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) RequestStage stage,
      @RequestParam(required = false) RequestKind kind,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), PayRequestAccess.MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "id"));
    return PageResponse.of(
        queries.search(companyId, new Filter(stage, kind, from, to, q), pageable),
        RequestSummary::from);
  }

  /**
   * Requests per stage (work list tabs).
   *
   * @param companyId company
   * @return count per stage
   */
  @GetMapping("/requests/counts")
  @PreAuthorize(PayRequestAccess.VIEW)
  public Map<RequestStage, Long> counts(@RequestParam Long companyId) {
    return queries.counts(companyId);
  }

  /**
   * One request (MKT 1.5.0, 2.26.0).
   *
   * @param id request
   * @return request
   */
  @GetMapping(REQUEST)
  @PreAuthorize(PayRequestAccess.VIEW)
  public RequestView get(@PathVariable Long id) {
    return view(queries.get(id));
  }

  /**
   * Validation tasks of a refund (MKT 1.11.0).
   *
   * @param id request
   * @return tasks, newest round first
   */
  @GetMapping(REQUEST + "/validations")
  @PreAuthorize(PayRequestAccess.VIEW)
  public List<ValidationView> validations(@PathVariable Long id) {
    return validations.of(id).stream().map(ValidationView::from).toList();
  }

  /**
   * The printable form of a request: RRF or RFP (MKT 1.10.0).
   *
   * @param id request
   * @return PDF
   */
  @GetMapping(REQUEST + "/form")
  @PreAuthorize(PayRequestAccess.VIEW)
  public ResponseEntity<byte[]> form(@PathVariable Long id) {
    return pdf(documents.form(id));
  }

  /**
   * The Cash Advance Liquidation Form (Appendix D).
   *
   * @param id cash-advance request
   * @return PDF
   */
  @GetMapping(REQUEST + "/liquidation/form")
  @PreAuthorize(PayRequestAccess.VIEW)
  public ResponseEntity<byte[]> liquidationForm(@PathVariable Long id) {
    return pdf(documents.liquidationForm(id));
  }

  /**
   * The CA / SA information a client already has (to prefill a refund, MKT 2.25.0).
   *
   * @param companyId company
   * @param clientCode client
   * @return live accounts first
   */
  @GetMapping("/payout-accounts")
  @PreAuthorize(PayRequestAccess.VIEW)
  public List<PayoutAccountResponse> payoutAccounts(
      @RequestParam Long companyId, @RequestParam String clientCode) {
    return payouts.forClient(companyId, clientCode).stream()
        .map(PayoutAccountResponse::from)
        .toList();
  }

  /**
   * Raises a Refund Request Form (MKT 1.10.0).
   *
   * @param companyId company
   * @param input form
   * @return the request
   */
  @PostMapping("/requests/refunds")
  @PreAuthorize(PayRequestAccess.CREATE)
  public RequestView createRefund(
      @RequestParam Long companyId, @Valid @RequestBody RefundInput input) {
    return view(forms.createRefund(companyId, input.draft()));
  }

  /**
   * Changes a refund request being prepared.
   *
   * @param id request
   * @param input form
   * @return the request
   */
  @PutMapping(REQUEST + "/refund")
  @PreAuthorize(PayRequestAccess.CREATE)
  public RequestView updateRefund(@PathVariable Long id, @Valid @RequestBody RefundInput input) {
    return view(forms.updateRefund(id, input.draft()));
  }

  /**
   * Raises a Request for Payment of a cash advance (MKT 1.10.0).
   *
   * @param companyId company
   * @param input form
   * @return the request
   */
  @PostMapping("/requests/cash-advances")
  @PreAuthorize(PayRequestAccess.CREATE)
  public RequestView createCashAdvance(
      @RequestParam Long companyId, @Valid @RequestBody CashAdvanceInput input) {
    return view(forms.createCashAdvance(companyId, input.draft()));
  }

  /**
   * Changes a draft cash-advance request.
   *
   * @param id request
   * @param input form
   * @return the request
   */
  @PutMapping(REQUEST + "/cash-advance")
  @PreAuthorize(PayRequestAccess.CREATE)
  public RequestView updateCashAdvance(
      @PathVariable Long id, @Valid @RequestBody CashAdvanceInput input) {
    return view(forms.updateCashAdvance(id, input.draft()));
  }

  /**
   * Requests the cancellation of a disbursed check (MKT 1.19.0).
   *
   * @param companyId company
   * @param input paid request, check and reason
   * @return the request
   */
  @PostMapping("/requests/check-cancellations")
  @PreAuthorize(PayRequestAccess.CREATE)
  public RequestView createCheckCancellation(
      @RequestParam Long companyId, @Valid @RequestBody CheckCancellationInput input) {
    return view(forms.createCheckCancellation(companyId, input.draft()));
  }

  private RequestView view(PaymentRequest r) {
    return RequestView.from(r, liquidations.of(r.getId()).orElse(null));
  }

  private static ResponseEntity<byte[]> pdf(Generated document) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(document.fileName()))
        .body(document.content());
  }
}
