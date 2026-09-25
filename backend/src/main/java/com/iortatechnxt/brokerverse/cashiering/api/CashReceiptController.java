package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.api.dto.ReceiptDtos.ActionResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.ReceiptDtos.ArRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.ReceiptDtos.CancelRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.ReceiptDtos.OrRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.ReceiptDtos.ReceiptResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.ReceiptDtos.ReceiptSummaryResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.ReceiptDtos.ReinstateRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.OrAmounts;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction.Reason;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction.ReinstatementFields;
import com.iortatechnxt.brokerverse.cashiering.service.BatchPrintService;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.ArIssue;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.OrIssue;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptActionService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptDocument;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.ReceiptCriteria;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receipts (CSHID.001-005/010/014/019/021): search with the ten criteria, receipt page, AR for
 * non-premium payments, Head Office OR, cancellation and reinstatement requests and their approval,
 * printed receipt and Certificate of Payment.
 */
@RestController
@RequestMapping("/api/v1/cashiering")
public class CashReceiptController {

  private final CashReceiptService receipts;
  private final ReceiptSearchService search;
  private final ReceiptActionService actions;
  private final ReceiptDocument documents;
  private final BatchPrintService printing;

  /**
   * Creates the controller.
   *
   * @param receipts receipts
   * @param search receipt search
   * @param actions cancellations and reinstatements
   * @param documents printed receipts
   * @param printing batch printing and certificates
   */
  public CashReceiptController(
      CashReceiptService receipts,
      ReceiptSearchService search,
      ReceiptActionService actions,
      ReceiptDocument documents,
      BatchPrintService printing) {
    this.receipts = receipts;
    this.search = search;
    this.actions = actions;
    this.documents = documents;
    this.printing = printing;
  }

  /**
   * Searches receipts (CSHID.010).
   *
   * @param criteria criteria
   * @param page page
   * @param size size
   * @return receipts, newest first
   */
  @GetMapping("/receipts")
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<ReceiptSummaryResponse> search(
      SearchParams criteria,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        search.search(criteria.toCriteria(), CashAccess.newest(page, size)),
        ReceiptSummaryResponse::from);
  }

  /**
   * One receipt with its lines, applications and actions.
   *
   * @param id receipt
   * @return receipt
   */
  @GetMapping("/receipts/{id}")
  @PreAuthorize(CashAccess.VIEW)
  public ReceiptResponse get(@PathVariable Long id) {
    CashReceiptService.ReceiptDetail d = receipts.detail(id);
    return ReceiptResponse.from(d.receipt(), d.applications(), d.actions());
  }

  /**
   * Issues an AR without matching: non-premium insurer payments (CSHID.021).
   *
   * @param request AR
   * @return receipt
   */
  @PostMapping("/receipts/ar")
  @PreAuthorize(CashAccess.RECEIPT)
  public ReceiptResponse issueAr(@Valid @RequestBody ArRequest request) {
    Receipt r =
        receipts.issueAr(
            new ArIssue(
                request.companyId(),
                request.branchId(),
                request.arClass(),
                request.receiptDate(),
                request.payorCode(),
                request.payorName(),
                null,
                null,
                request.currency(),
                request.amount(),
                new ReceiptTender(
                    request.mode(),
                    request.checkNo(),
                    request.checkBank(),
                    null,
                    null,
                    ReceiptSource.OTC,
                    null,
                    null,
                    request.remarks())));
    return get(r.getId());
  }

  /**
   * Issues a Head Office OR (CSHID.002/006).
   *
   * @param request OR
   * @return receipt
   */
  @PostMapping("/receipts/or")
  @PreAuthorize(CashAccess.RECEIPT)
  public ReceiptResponse issueOr(@Valid @RequestBody OrRequest request) {
    Receipt r =
        receipts.issueOr(
            new OrIssue(
                request.companyId(),
                null,
                request.orType(),
                request.receiptDate(),
                request.payorCode(),
                request.payorName(),
                request.currency(),
                request.lines().stream()
                    .map(
                        l ->
                            CashReceiptService.line(
                                l.invoiceNo(),
                                l.insurerCode(),
                                new OrAmounts(l.gross(), l.vat(), l.wtax()),
                                l.description()))
                    .toList(),
                new ReceiptTender(
                    request.mode(),
                    request.checkNo(),
                    request.checkBank(),
                    null,
                    request.certificateRef(),
                    ReceiptSource.OTC,
                    null,
                    null,
                    request.remarks()),
                false));
    return get(r.getId());
  }

  /**
   * Requests a cancellation (CSHID.001/003).
   *
   * @param id receipt
   * @param request reason
   * @return the action
   */
  @PostMapping("/receipts/{id}/cancel")
  @PreAuthorize(CashAccess.CANCEL)
  public ActionResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
    return ActionResponse.from(
        actions.requestCancel(id, new Reason(request.reasonCode(), request.reasonText())));
  }

  /**
   * Requests a reinstatement (CSHID.004/005).
   *
   * @param id cancelled receipt
   * @param request full or partial, reason and encoded fields
   * @return the action
   */
  @PostMapping("/receipts/{id}/reinstate")
  @PreAuthorize(CashAccess.REINSTATE)
  public ActionResponse reinstate(
      @PathVariable Long id, @Valid @RequestBody ReinstateRequest request) {
    return ActionResponse.from(
        actions.requestReinstatement(
            id,
            new ReceiptActionService.ReinstateRequest(
                request.full(),
                request.amount(),
                new Reason(request.reasonCode(), request.reasonText()),
                new ReinstatementFields(
                    request.invoiceNo(),
                    request.documentNo(),
                    request.payorName(),
                    request.accountOfficer(),
                    request.unitHead(),
                    request.teamLeader()))));
  }

  /**
   * Cancellations and reinstatements of a company in some stages.
   *
   * @param companyId company
   * @param stage stages (FOR_APPROVAL when absent)
   * @param page page
   * @param size size
   * @return actions, newest first
   */
  @GetMapping("/receipt-actions")
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<ActionResponse> actions(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<String> stage,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    List<String> stages = stage == null || stage.isEmpty() ? List.of("FOR_APPROVAL") : stage;
    return PageResponse.of(
        actions.list(companyId, stages, CashAccess.page(page, size)), ActionResponse::from);
  }

  /**
   * Approves and posts a cancellation or reinstatement (checker).
   *
   * @param id action
   * @return the action
   */
  @PostMapping("/receipt-actions/{id}/approve")
  @PreAuthorize(CashAccess.APPROVE)
  public ActionResponse approve(@PathVariable Long id) {
    return ActionResponse.from(actions.approve(id));
  }

  /**
   * Submits a returned request again.
   *
   * @param id action
   * @return the action
   */
  @PostMapping("/receipt-actions/{id}/resubmit")
  @PreAuthorize("hasAnyAuthority('CASH_CANCEL', 'CASH_REINSTATE')")
  public ActionResponse resubmit(@PathVariable Long id) {
    ReceiptAction a = actions.resubmit(id);
    return ActionResponse.from(a);
  }

  /**
   * The printed receipt.
   *
   * @param id receipt
   * @return PDF
   */
  @GetMapping("/receipts/{id}/pdf")
  @PreAuthorize(CashAccess.VIEW)
  public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
    Receipt r = receipts.get(id);
    return CashAccess.pdf(r.getReceiptNo() + ".pdf", documents.pdf(r));
  }

  /**
   * A Certificate of Payment (Annex II report 19).
   *
   * @param id receipt
   * @param policyNo policy number
   * @param unit requesting market unit
   * @return PDF
   */
  @PostMapping("/receipts/{id}/certificate-of-payment")
  @PreAuthorize(CashAccess.RECEIPT)
  public ResponseEntity<byte[]> certificateOfPayment(
      @PathVariable Long id,
      @RequestParam(required = false) String policyNo,
      @RequestParam String unit) {
    return CashAccess.pdf("COP-" + id + ".pdf", printing.certificateOfPayment(id, policyNo, unit));
  }

  /**
   * Search parameters (CSHID.010).
   *
   * @param companyId company
   * @param receiptNo AR / OR number
   * @param clientCode client number
   * @param invoiceNo invoice number
   * @param payor payor name
   * @param assured assured name
   * @param amount amount
   * @param from issued from
   * @param to issued to
   * @param policyNo policy number
   * @param insurer insurer
   * @param kind AR or OR
   * @param status status
   */
  public record SearchParams(
      Long companyId,
      String receiptNo,
      String clientCode,
      String invoiceNo,
      String payor,
      String assured,
      BigDecimal amount,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      String policyNo,
      String insurer,
      ReceiptKind kind,
      ReceiptStatus status) {

    ReceiptCriteria toCriteria() {
      return new ReceiptCriteria(
          companyId,
          receiptNo,
          clientCode,
          invoiceNo,
          payor,
          assured,
          amount,
          from,
          to,
          policyNo,
          insurer,
          kind,
          status);
    }
  }
}
