package com.iortatechnxt.brokerverse.opsledger.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.api.dto.FlowInDtos.ReplayRequest;
import com.iortatechnxt.brokerverse.opsledger.api.dto.FlowInDtos.RunResponse;
import com.iortatechnxt.brokerverse.opsledger.api.dto.Invoice360Response;
import com.iortatechnxt.brokerverse.opsledger.api.dto.LedgerSearchParams;
import com.iortatechnxt.brokerverse.opsledger.api.dto.MovementResponse;
import com.iortatechnxt.brokerverse.opsledger.api.dto.OpsInvoiceResponse;
import com.iortatechnxt.brokerverse.opsledger.api.dto.OpsInvoiceSummaryResponse;
import com.iortatechnxt.brokerverse.opsledger.api.dto.StatusChangeResponse;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.service.Invoice360Service;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceFeedReplayService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operations invoices (RMTID.026/032/038, ADJID.024): search, the invoice 360 view, movements,
 * status history, the invoices of an account and the replay of the booking feed.
 */
@RestController
@RequestMapping("/api/v1/ops")
public class OpsInvoiceController {

  private final InvoiceLedgerQueryService ledger;
  private final Invoice360Service views;
  private final InvoiceFeedReplayService replay;

  /**
   * Creates the controller.
   *
   * @param ledger ledger reads
   * @param views invoice 360
   * @param replay booking feed replay
   */
  public OpsInvoiceController(
      InvoiceLedgerQueryService ledger, Invoice360Service views, InvoiceFeedReplayService replay) {
    this.ledger = ledger;
    this.views = views;
    this.replay = replay;
  }

  /**
   * Searches invoices.
   *
   * @param companyId company
   * @param params criteria
   * @param page page
   * @param size size
   * @return invoices, newest booking first
   */
  @GetMapping("/invoices")
  @PreAuthorize(OpsAccess.VIEW)
  public PageResponse<OpsInvoiceSummaryResponse> search(
      @RequestParam Long companyId,
      LedgerSearchParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), OpsAccess.MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "classification.bookingDate", "id"));
    return PageResponse.of(
        ledger.searchLoaded(params.toSearch(companyId), pageable), OpsInvoiceSummaryResponse::from);
  }

  /**
   * The invoice 360 view.
   *
   * @param invoiceNo invoice number
   * @return view
   */
  @GetMapping("/invoices/{invoiceNo}")
  @PreAuthorize(OpsAccess.VIEW)
  public Invoice360Response view(@PathVariable String invoiceNo) {
    return Invoice360Response.from(views.view(invoiceNo));
  }

  /**
   * Movements of an invoice.
   *
   * @param invoiceNo invoice number
   * @return movements in posting order
   */
  @GetMapping("/invoices/{invoiceNo}/movements")
  @PreAuthorize(OpsAccess.VIEW)
  public List<MovementResponse> movements(@PathVariable String invoiceNo) {
    return ledger.movements(invoiceNo).stream().map(MovementResponse::from).toList();
  }

  /**
   * The invoice family: every invoice sharing the root invoice number (DIS 3.27.2, ACSL 2.16.0).
   *
   * @param invoiceNo any invoice of the family
   * @return invoices, root first
   */
  @GetMapping("/invoices/{invoiceNo}/family")
  @PreAuthorize(OpsAccess.VIEW)
  public List<OpsInvoiceResponse> family(@PathVariable String invoiceNo) {
    return ledger.family(invoiceNo).stream().map(OpsInvoiceResponse::from).toList();
  }

  /**
   * Status, flag and lock history of an invoice.
   *
   * @param invoiceNo invoice number
   * @return changes, oldest first
   */
  @GetMapping("/invoices/{invoiceNo}/history")
  @PreAuthorize(OpsAccess.VIEW)
  public List<StatusChangeResponse> history(@PathVariable String invoiceNo) {
    return ledger.history(invoiceNo).stream().map(StatusChangeResponse::from).toList();
  }

  /**
   * Invoices of an account (ARN look-up).
   *
   * @param arn Account Reference Number
   * @return invoices, oldest first
   */
  @GetMapping("/accounts/{arn}/invoices")
  @PreAuthorize(OpsAccess.VIEW)
  public List<OpsInvoiceResponse> forAccount(@PathVariable String arn) {
    return ledger.forArn(arn).stream().map(OpsInvoiceResponse::from).toList();
  }

  /**
   * Copies booked invoices missing from the ledger: one invoice, one account or a company.
   *
   * @param request invoice, account or company
   * @return the replay run
   */
  @PostMapping("/invoices/replay")
  @PreAuthorize(OpsAccess.FLOWIN)
  public RunResponse replay(@Valid @RequestBody ReplayRequest request) {
    FlowInRun run;
    if (hasText(request.invoiceNo())) {
      run = replay.replayInvoice(request.invoiceNo().strip());
    } else if (hasText(request.arn())) {
      run = replay.replayAccount(request.arn().strip());
    } else if (request.companyId() != null) {
      run = replay.replayCompany(request.companyId(), Trigger.MANUAL);
    } else {
      throw new BusinessRuleException(
          "REPLAY_SCOPE", "Give an invoice number, an ARN or a company to replay");
    }
    return RunResponse.from(run);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
