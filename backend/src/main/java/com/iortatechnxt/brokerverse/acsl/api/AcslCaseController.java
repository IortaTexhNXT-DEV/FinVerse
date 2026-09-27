package com.iortatechnxt.brokerverse.acsl.api;

import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.AssignInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.CaseInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.CorrectionInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.FindingsInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.MessageInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.ResultInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.ReversalInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslViews.CaseView;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslViews.CorrectionView;
import com.iortatechnxt.brokerverse.acsl.domain.CaseStage;
import com.iortatechnxt.brokerverse.acsl.domain.CaseType;
import com.iortatechnxt.brokerverse.acsl.service.AcslQueryService;
import com.iortatechnxt.brokerverse.acsl.service.CaseLinks;
import com.iortatechnxt.brokerverse.acsl.service.CaseService;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
 * ACSL cases (ACSL 2.5.0-2.6.2, 2.9.0): the cases board, one case, the cases of an invoice family,
 * and the actions: open, assign, record findings, give the result, raise a correction, request a
 * payment reversal and message the Account Officer. Start and send-back are generic workflow
 * actions.
 */
@RestController
@RequestMapping("/api/v1/acsl")
public class AcslCaseController {

  private static final String CASE = "/cases/{id}";

  private final AcslQueryService queries;
  private final CaseService cases;
  private final CaseLinks links;
  private final CorrectionService corrections;
  private final InvoiceLedgerQueryService ledger;

  /**
   * Creates the controller.
   *
   * @param queries reads
   * @param cases cases
   * @param links reversal and AO messages
   * @param corrections corrections (raised from a case)
   * @param ledger invoice ledger (family root)
   */
  public AcslCaseController(
      AcslQueryService queries,
      CaseService cases,
      CaseLinks links,
      CorrectionService corrections,
      InvoiceLedgerQueryService ledger) {
    this.queries = queries;
    this.cases = cases;
    this.links = links;
    this.corrections = corrections;
    this.ledger = ledger;
  }

  /**
   * Cases board (ACSL 2.5.x).
   *
   * @param companyId company
   * @param stage stage
   * @param type type
   * @param q case, subject, invoice or AR
   * @param page page
   * @param size size
   * @return cases, newest first
   */
  @GetMapping("/cases")
  @PreAuthorize(AcslAccess.VIEW)
  public PageResponse<CaseView> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) CaseStage stage,
      @RequestParam(required = false) CaseType type,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.cases(companyId, stage, type, q, page(page, size)), CaseView::from);
  }

  /**
   * Cases per stage.
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/cases/counts")
  @PreAuthorize(AcslAccess.VIEW)
  public Map<CaseStage, Long> counts(@RequestParam Long companyId) {
    return queries.caseCounts(companyId);
  }

  /**
   * One case.
   *
   * @param id case
   * @return case
   */
  @GetMapping(CASE)
  @PreAuthorize(AcslAccess.VIEW)
  public CaseView get(@PathVariable Long id) {
    return CaseView.from(cases.get(id));
  }

  /**
   * Cases of the family of an invoice (ACSL 2.5.3, 2.16.0).
   *
   * @param invoiceNo invoice
   * @return cases, newest first
   */
  @GetMapping("/invoices/{invoiceNo}/cases")
  @PreAuthorize(AcslAccess.VIEW)
  public List<CaseView> family(@PathVariable String invoiceNo) {
    return queries.casesOfFamily(ledger.require(invoiceNo).getRootInvoiceNo()).stream()
        .map(CaseView::from)
        .toList();
  }

  /**
   * Opens a case (ACSL 2.5.0, 2.5.5, 2.6.x).
   *
   * @param companyId company
   * @param input type, invoice, AR, amount, subject and details
   * @return the case
   */
  @PostMapping("/cases")
  @PreAuthorize(AcslAccess.PROCESS)
  public CaseView open(@RequestParam Long companyId, @Valid @RequestBody CaseInput input) {
    return CaseView.from(cases.open(companyId, input.draft()));
  }

  /**
   * Assigns a case (team leader).
   *
   * @param id case
   * @param input processor and comment
   * @return the case
   */
  @PostMapping(CASE + "/assign")
  @PreAuthorize(AcslAccess.ASSIGN)
  public CaseView assign(@PathVariable Long id, @Valid @RequestBody AssignInput input) {
    return CaseView.from(cases.assign(id, input.username().strip(), input.comment()));
  }

  /**
   * Records the findings (ACSL 2.5.0).
   *
   * @param id case
   * @param input findings
   * @return the case
   */
  @PutMapping(CASE + "/findings")
  @PreAuthorize(AcslAccess.PROCESS)
  public CaseView findings(@PathVariable Long id, @Valid @RequestBody FindingsInput input) {
    return CaseView.from(cases.recordFindings(id, input.findings()));
  }

  /**
   * Gives the result to the requester (ACSL 2.5.4).
   *
   * @param id case
   * @param input outcome and remarks
   * @return the case
   */
  @PostMapping(CASE + "/result")
  @PreAuthorize(AcslAccess.PROCESS)
  public CaseView result(@PathVariable Long id, @Valid @RequestBody ResultInput input) {
    return CaseView.from(cases.provideResult(id, input.outcome(), input.remarks()));
  }

  /**
   * Raises a correction entry from the investigation (ACSL 2.9.0).
   *
   * @param id case
   * @param input kind, original journal and description
   * @return the correction
   */
  @PostMapping(CASE + "/correction")
  @PreAuthorize(AcslAccess.PROCESS)
  public CorrectionView raiseCorrection(
      @PathVariable Long id, @Valid @RequestBody CorrectionInput input) {
    return CorrectionView.from(corrections.raiseFromCase(id, input.draft()));
  }

  /**
   * Requests the sub-ledger payment reversal from Cashiering (ACSL 2.6.1).
   *
   * @param id case
   * @param input receipt, amount and reason
   * @return the case
   */
  @PostMapping(CASE + "/payment-reversal")
  @PreAuthorize(AcslAccess.APPLY)
  public CaseView reversal(@PathVariable Long id, @Valid @RequestBody ReversalInput input) {
    return CaseView.from(
        links.requestReversal(id, input.receiptNo(), input.amount(), input.reason()));
  }

  /**
   * Messages the Account Officer about a short or over payment (ACSL 2.6.2).
   *
   * @param id case
   * @param input message
   * @return the Account Officer notified
   */
  @PostMapping(CASE + "/message-ao")
  @PreAuthorize(AcslAccess.PROCESS)
  public Map<String, String> messageAo(
      @PathVariable Long id, @Valid @RequestBody MessageInput input) {
    return Map.of("accountOfficer", links.messageAccountOfficer(id, input.message()));
  }

  static PageRequest page(int page, int size) {
    return PageRequest.of(
        Math.max(page, 0),
        Math.min(Math.max(size, 1), AcslAccess.MAX_PAGE),
        Sort.by(Sort.Direction.DESC, "id"));
  }
}
