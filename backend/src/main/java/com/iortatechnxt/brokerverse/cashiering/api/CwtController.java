package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.api.dto.CwtDtos.BatchRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.CwtDtos.CwtBatchResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.CwtDtos.CwtTagRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.CwtDtos.CwtTagResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.CwtDtos.ExpectedResponse;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag.CwtDetails;
import com.iortatechnxt.brokerverse.cashiering.service.CwtService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BIR 2307 (CSHID.026/027, MKTID.010/013, DBMID.001): Marketing tagging, Cashiering receipt with
 * the CWT-copy checklist, cash path settlement, batch validation with the reclass, routing to
 * Disbursement and release to the insurer.
 */
@RestController
@RequestMapping("/api/v1/cashiering/cwt")
public class CwtController {

  private final CwtService cwt;
  private final InvoiceLedgerQueryService ledger;

  /**
   * Creates the controller.
   *
   * @param cwt BIR 2307
   * @param ledger invoice ledger
   */
  public CwtController(CwtService cwt, InvoiceLedgerQueryService ledger) {
    this.cwt = cwt;
    this.ledger = ledger;
  }

  /**
   * Tags in some stages.
   *
   * @param companyId company
   * @param stage stages (all live stages when absent)
   * @param page page
   * @param size size
   * @return tags, newest first
   */
  @GetMapping
  @PreAuthorize(CashAccess.CWT_VIEW)
  public PageResponse<CwtTagResponse> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<String> stage,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    List<String> stages =
        stage == null || stage.isEmpty()
            ? List.of("TAGGED", "VALIDATING", "REPORT_POSTED", "WITH_DISBURSEMENT")
            : stage;
    return PageResponse.of(
        cwt.list(companyId, stages, CashAccess.page(page, size)), CwtTagResponse::from);
  }

  /**
   * The 2% an invoice still expects (tagging screen).
   *
   * @param invoiceNo invoice
   * @return expected amount and facts
   */
  @GetMapping("/expected")
  @PreAuthorize(CashAccess.CWT_VIEW)
  public ExpectedResponse expected(@RequestParam String invoiceNo) {
    OpsInvoice invoice = ledger.require(invoiceNo);
    invoice.loadCollections();
    return new ExpectedResponse(
        invoice.getInvoiceNo(),
        invoice.getArn(),
        invoice.getAssuredName(),
        invoice.getInsurerCode(),
        invoice.isCwtFlag(),
        cwt.expected(invoice),
        invoice.getRemittanceStatus().name());
  }

  /**
   * Tags a 2307 reversal (Marketing Collection).
   *
   * @param request tag
   * @return tag
   */
  @PostMapping
  @PreAuthorize(CashAccess.CWT_TAG)
  public CwtTagResponse tag(@Valid @RequestBody CwtTagRequest request) {
    return CwtTagResponse.from(
        cwt.tag(
            request.companyId(),
            request.invoiceNo(),
            new CwtDetails(
                request.amount(),
                request.path(),
                request.certificateNo(),
                request.periodFrom(),
                request.periodTo(),
                request.remarks())));
  }

  /**
   * Receives a tag for validation.
   *
   * @param id tag
   * @param copyReceived CWT copy received
   * @return tag
   */
  @PostMapping("/{id}/receive")
  @PreAuthorize(CashAccess.CWT_PROCESS)
  public CwtTagResponse receive(
      @PathVariable Long id, @RequestParam(defaultValue = "false") boolean copyReceived) {
    return CwtTagResponse.from(cwt.receive(id, copyReceived));
  }

  /**
   * Updates the CWT copy checklist.
   *
   * @param id tag
   * @param copyReceived CWT copy received
   * @return tag
   */
  @PostMapping("/{id}/checklist")
  @PreAuthorize(CashAccess.CWT_PROCESS)
  public CwtTagResponse checklist(@PathVariable Long id, @RequestParam boolean copyReceived) {
    return CwtTagResponse.from(cwt.checklist(id, copyReceived));
  }

  /**
   * Settles a cash-path tag with an AR.
   *
   * @param id tag
   * @param branchId receiving branch
   * @return tag
   */
  @PostMapping("/{id}/settle-cash")
  @PreAuthorize(CashAccess.CWT_PROCESS)
  public CwtTagResponse settleCash(@PathVariable Long id, @RequestParam Long branchId) {
    return CwtTagResponse.from(cwt.settleCash(id, branchId));
  }

  /**
   * Recent batches.
   *
   * @param companyId company
   * @return batches
   */
  @GetMapping("/batches")
  @PreAuthorize(CashAccess.CWT_VIEW)
  public List<CwtBatchResponse> batches(@RequestParam Long companyId) {
    return cwt.batches(companyId).stream().map(CwtBatchResponse::from).toList();
  }

  /**
   * Validates certificates of one insurer and posts the report.
   *
   * @param request tags
   * @return batch
   */
  @PostMapping("/batches")
  @PreAuthorize(CashAccess.CWT_PROCESS)
  public CwtBatchResponse validate(@Valid @RequestBody BatchRequest request) {
    return CwtBatchResponse.from(cwt.validateBatch(request.companyId(), request.tagIds()));
  }

  /**
   * Tags of a batch.
   *
   * @param id batch
   * @return tags
   */
  @GetMapping("/batches/{id}/tags")
  @PreAuthorize(CashAccess.CWT_VIEW)
  public List<CwtTagResponse> batchTags(@PathVariable Long id) {
    return cwt.tagsOf(id).stream().map(CwtTagResponse::from).toList();
  }

  /**
   * Routes a batch to Disbursement.
   *
   * @param id batch
   * @return batch
   */
  @PostMapping("/batches/{id}/route")
  @PreAuthorize(CashAccess.CWT_PROCESS)
  public CwtBatchResponse route(@PathVariable Long id) {
    return CwtBatchResponse.from(cwt.route(id));
  }

  /**
   * Releases a batch to the insurer (Disbursement).
   *
   * @param id batch
   * @return batch
   */
  @PostMapping("/batches/{id}/release")
  @PreAuthorize(CashAccess.DISBURSEMENT)
  public CwtBatchResponse release(@PathVariable Long id) {
    return CwtBatchResponse.from(cwt.release(cwt.batch(id).getBatchNo(), false));
  }
}
